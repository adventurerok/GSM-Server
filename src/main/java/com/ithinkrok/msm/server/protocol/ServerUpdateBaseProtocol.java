package com.ithinkrok.msm.server.protocol;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.common.util.io.DirectoryListener;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.ServerListener;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.config.YamlConfigIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by paul on 10/03/16.
 */
public abstract class ServerUpdateBaseProtocol implements ServerListener, DirectoryListener {

    private final Logger log = LogManager.getLogger(ServerUpdateBaseProtocol.class);

    /**
     * A map of resource config paths to resources.
     */
    private final Map<Path, ServerResource> serverResources = new ConcurrentHashMap<>();

    /**
     * A map of client names to maps of resource names to resource versions.
     */
    private final Map<String, Map<String, Instant>> clientResources = new ConcurrentHashMap<>();

    private final Map<Path, Future<?>> modifiedPaths = new ConcurrentHashMap<>();
    private final Path serverResourcePath;
    private Server server;

    public ServerUpdateBaseProtocol(Path serverResourcePath) {
        this.serverResourcePath = serverResourcePath;

        if (!Files.exists(serverResourcePath)) {
            try {
                Files.createDirectories(serverResourcePath);
            } catch (IOException e) {
                log.warn("Failed to create missing updated resource directory", e);
            }
        }
    }

    @Override
    public void serverStarted(Server server) {
        this.server = server;

        server.getDirectoryWatcher().registerListener(serverResourcePath, this);

        updateResourceVersions(serverResourcePath);
    }

    private void updateResourceVersions(Path serverResourcePath) {
        Set<FileVisitOption> options = EnumSet.of(FileVisitOption.FOLLOW_LINKS);

        try {
            Files.walkFileTree(serverResourcePath, options, Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    server.getDirectoryWatcher().registerListener(dir, ServerUpdateBaseProtocol.this);

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    updateResource(file);

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("Failed to walk file tree", e);
        }
    }

    /**
     * @param path The path of the resource or resource config to update
     * @return If the resource or resource config was updated
     * @throws IOException If an error occurs while reading the resource info
     */
    private boolean updateResource(Path path) throws IOException {
        Config config = getResourceConfig(path);

        if (config == null) {
            //Ensure that this resource does not have a provided config.

            String fileName = path.getFileName().toString();
            String configName = fileName + ".0";
            Path configPath = path.getParent().resolve(configName);

            if (Files.exists(configPath)) {
                return false;
            }

            config = getDefaultResourceConfig(path);
        }

        updateResourceFromConfig(path, config);

        return true;
    }

    private Config getResourceConfig(Path path) throws IOException {
        String fileName = path.getFileName().toString().toLowerCase();

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) return null;

        String extension = fileName.substring(dotIndex + 1, fileName.length());
        if (extension.isEmpty()) return null;

        try {
            //Check the extension is an integer. This means it is a config file for a resource
            //noinspection ResultOfMethodCallIgnored
            Integer.parseInt(extension);

            Config config = YamlConfigIO.loadToConfig(path, new MemoryConfig());

            config.set("resource_name", fileName.substring(0, dotIndex));
            return config;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    protected Config getDefaultResourceConfig(Path path) {
        Config config = new MemoryConfig();

        String fileName = path.getFileName().toString();
        config.set("resource_name", fileName);
        config.set("resource_path", path.toString());

        return config;
    }

    private void updateResourceFromConfig(Path configPath, Config config) throws IOException {
        ServerResource resource = new ServerResource();

        resource.name = config.getString("resource_name");
        resource.path = serverResourcePath.resolve(config.getString("resource_path"));

        Instant modified = getModifiedInstant(resource.path);
        if (modified == null) return;

        resource.modified = modified;

        serverResources.put(configPath, resource);

        //TODO update client versions
    }

    protected Instant getModifiedInstant(Path path) throws IOException {
        return Files.getLastModifiedTime(path).toInstant();
    }

    @Override
    public void serverStopped(Server server) {

    }

    @Override
    public void connectionOpened(Connection connection, Channel channel) {

    }

    @Override
    public void connectionClosed(Connection connection) {
        //Remove our details on the versions of the resources this client has
        clientResources.remove(connection.getClient().getName());
    }

    @Override
    public void packetRecieved(Connection connection, Channel channel, Config payload) {

        String mode = payload.getString("mode");
        if(mode == null) return;

        switch (mode) {
            case "ResourceInfo":
                handleResourceInfo(connection, payload);
        }
    }

    private void handleResourceInfo(Connection connection, Config payload) {
        Map<String, Instant> clientResources = new ConcurrentHashMap<>();

        Config versions = payload.getConfigOrEmpty("versions");

        for(String name : versions.getKeys(true)) {
            clientResources.put(name, Instant.ofEpochMilli(versions.getLong(name)));
        }

        this.clientResources.put(connection.getClient().getName(), clientResources);

        //TODO check updates
    }

    @Override
    public void fileChanged(Path changed, WatchEvent.Kind<?> event) {
        if (Files.isDirectory(changed) && event == StandardWatchEventKinds.ENTRY_CREATE) {
            server.getDirectoryWatcher().registerListener(changed, this);
        } else if (event == StandardWatchEventKinds.ENTRY_DELETE) {
            server.getDirectoryWatcher().unregisterAllListeners(changed);

            //Remove all resources that start with this resource path
            //If this is a directory, it removes all resources in this directory or subdirectories.
            //If this is a file, it removes this resource.
            Iterator<Map.Entry<Path, ServerResource>> iterator = serverResources.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Path, ServerResource> next = iterator.next();

                if (next.getKey().startsWith(changed)) {
                    log.info("Removing resource " + next.getKey() + " as it's directory was deleted");
                    iterator.remove();
                }
            }
        }

        if (Files.isDirectory(changed)) return;

        //Wait 5 seconds before reading the change to allow time for file to be fully uploaded.
        Future<?> oldFuture = modifiedPaths.get(changed);
        if (oldFuture != null) oldFuture.cancel(false);

        Future<?> newFuture = server.scheduleAsync(() -> {
            try {
                resourceModified(changed);
            } catch (IOException e) {
                log.warn("Failed to update resource: " + changed, e);
            }
            modifiedPaths.remove(changed);
        }, 5, TimeUnit.SECONDS);

        modifiedPaths.put(changed, newFuture);
    }

    private void resourceModified(Path resourcePath) throws IOException {
        if (updateResource(resourcePath)) return;

        for (Map.Entry<Path, ServerResource> resourceEntry : serverResources.entrySet()) {
            if (!resourceEntry.getValue().path.equals(resourcePath)) continue;

            updateResource(resourceEntry.getKey());
        }
    }

    private static class ServerResource {
        String name;
        Path path;

        Instant modified;
    }
}

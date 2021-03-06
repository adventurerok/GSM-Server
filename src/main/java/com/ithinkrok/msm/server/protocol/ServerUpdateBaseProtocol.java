package com.ithinkrok.msm.server.protocol;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.util.io.DirectoryListener;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.ServerListener;
import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.InvalidConfigException;
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
public class ServerUpdateBaseProtocol implements ServerListener, DirectoryListener {

    private static final Logger log = LogManager.getLogger(ServerUpdateBaseProtocol.class);
    protected final String protocolName;
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

    public ServerUpdateBaseProtocol(String protocolName, Path serverResourcePath) {
        this.protocolName = protocolName;
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
            if (config == null) return false;
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

            Path fullPath = path.getParent().resolve(fileName.substring(0, dotIndex));
            Path relativePath = serverResourcePath.relativize(fullPath);

            config.set("resource_path", relativePath);
            return config;
        } catch (NumberFormatException ignored) {
        } catch (InvalidConfigException e) {
            log.info("Invalid resource config at " + path, e);
        }

        return null;
    }

    protected Config getDefaultResourceConfig(Path path) {
        Config config = new MemoryConfig();

        config.set("resource_name", serverResourcePath.relativize(path).toString());
        config.set("resource_path", serverResourcePath.relativize(path).toString());

        return config;
    }

    private void updateResourceFromConfig(Path configPath, Config config) throws IOException {
        ServerResource resource = new ServerResource();

        resource.name = config.getString("resource_name");
        resource.path = serverResourcePath.resolve(config.getString("resource_path"));

        resource.modified = getModifiedInstant(resource.path);

        resource.config = config;

        serverResources.put(configPath, resource);

        updateResourcesForAllClients(resource.name);
    }

    protected Instant getModifiedInstant(Path path) throws IOException {
        if(!Files.exists(path)){
            log.info("Skipping resource " + path + " as it does not exist");
            return null;
        }

        return Files.getLastModifiedTime(path).toInstant();
    }

    private void updateResourcesForAllClients(String resourceName) {
        for (String clientName : clientResources.keySet()) {
            Client<?> client = server.getClient(clientName);

            if (client == null) continue;

            updateResourcesForClient(client, resourceName);
        }
    }

    private void updateResourcesForClient(Client<?> client, String resourceName) {
        if (!client.isConnected()) return;

        Map<String, ServerResource> updates = getUpdatableResourcesForClient(client, resourceName);

        Channel channel;
        try {
            channel = client.getConnection().getChannel(protocolName);
        } catch (Exception e) {
            log.warn("Client disconnected during update", e);
            return;
        }

        Map<String, Instant> clientVersions = clientResources.get(client.getName());
        for (ServerResource update : updates.values()) {
            update.install(channel);
            clientVersions.put(update.name, update.modified);
            log.info("Updated resource " + update.name + " for client " + client.getName());
        }
    }

    private Map<String, ServerResource> getUpdatableResourcesForClient(Client<?> client, String resourceName) {
        Map<String, Instant> clientVersions = this.clientResources.get(client.getName());

        Map<String, ServerResource> updates = new HashMap<>();

        //Find the most recent resources that apply to the client
        //Only one resource per resource name
        for (ServerResource resource : serverResources.values()) {
            //The resource is currently missing
            if(resource.modified == null) continue;

            if (resourceName != null && !resourceName.equals(resource.name)) continue;

            if (!resource.applies(client, clientVersions)) continue;

            ServerResource other = updates.get(resource.name);
            if (other != null && resource.modified.isBefore(other.modified)) continue;

            updates.put(resource.name, resource);
        }
        return updates;
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
        if (mode == null) return;

        switch (mode) {
            case "ResourceInfo":
                handleResourceInfo(connection.getClient(), payload);
        }
    }

    private void handleResourceInfo(Client<?> client, Config payload) {
        Map<String, Instant> clientResources = new ConcurrentHashMap<>();

        Config versions = payload.getConfigOrEmpty("versions");

        for (String name : versions.getKeys(true)) {
            clientResources.put(name, Instant.ofEpochMilli(versions.getLong(name)));
        }

        this.clientResources.put(client.getName(), clientResources);

        updateResourcesForClient(client, null);
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
                    log.info("Removing resource " + next.getKey() + " as it was deleted");
                    iterator.remove();
                }
            }
            return;
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

    protected static class ServerResource {
        String name;
        Path path;

        Instant modified;

        Config config;

        public boolean applies(Client<?> client, Map<String, Instant> clientVersions) {
            Instant clientVersion = clientVersions.get(name);

            if (clientVersion == null) {
                return allowInstall(client);
            } else if (clientVersion.isBefore(modified)) {
                return allowUpdate(client);
            } else {
                return false;
            }
        }

        private boolean allowInstall(Client<?> client) {
            if (!config.contains("install_criteria")) return config.getBoolean("install", false);

            Config installCriteria = config.getConfigOrNull("install_criteria");
            return checkCriteria(installCriteria, client);
        }

        private boolean allowUpdate(Client<?> client) {
            if (!config.contains("update_criteria")) return config.getBoolean("update", true);

            Config updateCriteria = config.getConfigOrNull("update_criteria");
            return checkCriteria(updateCriteria, client);
        }

        private boolean checkCriteria(Config criteria, Client<?> client) {
            if (criteria.contains("server_name")) {
                String nameRegex = criteria.getString("server_name");
                if (!client.getName().matches(nameRegex)) return false;
            }

            if (criteria.contains("server_type")) {
                String typeRegex = criteria.getString("server_type");
                if (!client.getType().matches(typeRegex)) return false;
            }

            return true;
        }

        public boolean install(Channel channel) {
            Config payload = new MemoryConfig();

            payload.set("mode", "ResourceUpdate");

            payload.set("resource", name);
            payload.set("version", modified.toEpochMilli());

            byte[] bytes;
            try {
                bytes = Files.readAllBytes(path);
            } catch (IOException e) {
                log.warn("Failed to read updated resource", e);
                return false;
            }

            payload.set("bytes", bytes);
            payload.set("index", 0);
            payload.set("length", bytes.length);

            channel.write(payload);
            return true;
        }
    }
}

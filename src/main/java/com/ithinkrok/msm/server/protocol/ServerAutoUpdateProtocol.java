package com.ithinkrok.msm.server.protocol;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.common.Packet;
import com.ithinkrok.msm.common.util.io.DirectoryListener;
import com.ithinkrok.msm.common.util.io.DirectoryWatcher;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.MinecraftServer;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.ServerListener;
import com.ithinkrok.util.FIleUtil;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.config.YamlConfigIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by paul on 05/02/16.
 */
public class ServerAutoUpdateProtocol implements ServerListener, DirectoryListener {

    private final Logger log = LogManager.getLogger(ServerAutoUpdateProtocol.class);

    private final Map<String, ServerVersionInfo> updatesMap = new ConcurrentHashMap<>();
    private final Map<MinecraftServer, Map<String, Instant>> clientVersionsMap = new ConcurrentHashMap<>();

    private final Path updatedPluginsPath;
    private final Map<Path, Future<?>> modifiedPaths = new ConcurrentHashMap<>();
    private Server server;

    public ServerAutoUpdateProtocol(Path updatedPluginsPath) {
        this.updatedPluginsPath = updatedPluginsPath;

        if (!Files.exists(updatedPluginsPath)) {
            try {
                Files.createDirectories(updatedPluginsPath);
            } catch (IOException e) {
                log.warn("Failed to create missing updated plugins directory", e);
                return;
            }
        }

        try {
            DirectoryWatcher directoryWatcher = new DirectoryWatcher(updatedPluginsPath.getFileSystem());

            directoryWatcher.registerListener(updatedPluginsPath, this);
        } catch (IOException ignored) {
            throw new RuntimeException("Failed to create directory watcher");
        }

        updatePluginVersions(updatedPluginsPath);
    }

    private void updatePluginVersions(Path updatedPluginsPath) {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(updatedPluginsPath, "**.jar")) {
            for (Path pluginPath : directoryStream) {
                updatePluginVersion(pluginPath);
            }
        } catch (IOException e) {
            log.warn("Failed to do full plugin updates folder check", e);
        }
    }

    private void updatePluginVersion(Path pluginPath) {

        try (FileSystem jarFile = FIleUtil.createZipFileSystem(pluginPath)) {
            Path pluginYmlPath = jarFile.getPath("/plugin.yml");

            if (!Files.exists(pluginYmlPath)) return;

            Config pluginYml = YamlConfigIO.loadToConfig(pluginYmlPath, new MemoryConfig('/'));

            String name = pluginYml.getString("name");
            if (name == null) return;

            Instant dateModified = Files.getLastModifiedTime(pluginPath).toInstant();

            ServerVersionInfo versionInfo = updatesMap.get(name);

            if (versionInfo == null) versionInfo = new ServerVersionInfo();
            else if (!versionInfo.pluginPath.equals(pluginPath)) {

                //Skip if we already have a more up to date version of this plugin with a different path.
                if (versionInfo.dateModified.isAfter(dateModified)) return;
            }

            versionInfo.dateModified = dateModified;
            versionInfo.pluginPath = pluginPath;

            updatesMap.put(name, versionInfo);

            checkUpdates(name);
        } catch (IOException e) {
            log.warn("Failed to update version info for plugin: " + pluginPath, e);
        }
    }

    private void checkUpdates(String pluginName) {
        for (MinecraftServer minecraftServer : clientVersionsMap.keySet()) {
            if (!minecraftServer.isConnected()) continue;

            checkUpdate(minecraftServer, pluginName);
        }
    }

    private void checkUpdate(MinecraftServer minecraftServer, String pluginName) {
        if (!updatesMap.containsKey(pluginName)) return;

        Map<String, Instant> clientVersions = clientVersionsMap.get(minecraftServer);
        if (clientVersions == null || !clientVersions.containsKey(pluginName)) return;

        ServerVersionInfo serverVersionInfo = updatesMap.get(pluginName);

        Instant serverVersion = serverVersionInfo.dateModified;
        Instant clientVersion = clientVersions.get(pluginName);

        if (!clientVersion.isBefore(serverVersion)) return;

        clientVersions.put(pluginName, serverVersion);

        log.info("Updating plugin \"" + pluginName + "\" on bukkit/spigot server: " + minecraftServer);
        doUpdate(minecraftServer.getConnection().getChannel("MSMAutoUpdate"), pluginName, serverVersionInfo.pluginPath);
    }

    private void doUpdate(Channel channel, String plugin, Path pluginPath) {
        Config payload = new MemoryConfig();

        payload.set("mode", "PluginInstall");
        payload.set("name", plugin);
        payload.set("append", false);
        payload.set("final", true);

        try {
            payload.set("bytes", Files.readAllBytes(pluginPath));
        } catch (IOException e) {
            log.warn("Failed to read plugin update file: " + pluginPath, e);
            return;
        }

        channel.write(payload);
    }

    @Override
    public void connectionOpened(Connection connection, Channel channel) {
        if (server == null) server = connection.getConnectedTo();
    }

    @Override
    public void connectionClosed(Connection connection) {

    }

    @Override
    public void packetRecieved(Connection connection, Channel channel, Config payload) {

        String mode = payload.getString("mode");
        if (mode == null) return;

        switch (mode) {
            case "PluginInfo":
                handlePluginInfo(connection, payload);
                break;
        }
    }

    private void handlePluginInfo(Connection connection, Config payload) {
        Map<String, Instant> newPlugins = new ConcurrentHashMap<>();

        for (Config pluginInfo : payload.getConfigList("plugins")) {
            String name = pluginInfo.getString("name");

            Instant modified = Instant.ofEpochMilli(pluginInfo.getLong("modified"));

            newPlugins.put(name, modified);

            log.trace("Client plugin: " + name + ", " + modified);
        }

        clientVersionsMap.put(connection.getMinecraftServer(), newPlugins);

        //Check for plugin updates. This cannot be done in the loop above as it uses the clientVersionsMap
        for (String pluginName : newPlugins.keySet()) {
            checkUpdate(connection.getMinecraftServer(), pluginName);
        }
    }

    @Override
    public void fileChanged(Path changed, WatchEvent.Kind<?> event) {
        if (Files.isDirectory(changed) || !changed.toString().toLowerCase().endsWith(".jar")) return;

        if (event == StandardWatchEventKinds.ENTRY_DELETE) {
            Iterator<ServerVersionInfo> versionInfoIterator = updatesMap.values().iterator();

            while (versionInfoIterator.hasNext()) {
                ServerVersionInfo versionInfo = versionInfoIterator.next();

                if (versionInfo.pluginPath.equals(changed)) versionInfoIterator.remove();
            }

            updatePluginVersions(updatedPluginsPath);
            return;
        }

        Future<?> oldFuture = modifiedPaths.get(changed);
        if (oldFuture != null) oldFuture.cancel(false);

        Future<?> newFuture = server.scheduleAsync(() -> {
            updatePluginVersion(changed);
            modifiedPaths.remove(changed);
        }, 5, TimeUnit.SECONDS);

        modifiedPaths.put(changed, newFuture);
    }

    private static class ServerVersionInfo {
        private Instant dateModified;
        private Path pluginPath;
    }
}

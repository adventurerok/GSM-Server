package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.common.util.ConfigUtils;
import com.ithinkrok.msm.common.util.FIleUtil;
import com.ithinkrok.msm.common.util.io.DirectoryListener;
import com.ithinkrok.msm.common.util.io.DirectoryWatcher;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.MinecraftServer;
import com.ithinkrok.msm.server.ServerListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by paul on 05/02/16.
 */
public class ServerAutoUpdateProtocol implements ServerListener, DirectoryListener {

    private final Logger log = LogManager.getLogger(ServerAutoUpdateProtocol.class);

    private final Map<String, ServerVersionInfo> updatesMap = new ConcurrentHashMap<>();
    private final Map<MinecraftServer, Map<String, Instant>> clientVersionsMap = new ConcurrentHashMap<>();

    private final Path updatedPluginsPath;

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
        } catch (IOException e) {
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

            YamlConfiguration pluginYml = new YamlConfiguration();

            try (Reader reader = Files.newBufferedReader(pluginYmlPath)) {
                pluginYml.load(reader);
            }

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
        } catch (IOException | InvalidConfigurationException e) {
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
        ConfigurationSection payload = new MemoryConfiguration();

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

    }

    @Override
    public void connectionClosed(Connection connection) {

    }

    @Override
    public void packetRecieved(Connection connection, Channel channel, ConfigurationSection payload) {

        String mode = payload.getString("mode");
        if (mode == null) return;

        switch (mode) {
            case "PluginInfo":
                handlePluginInfo(connection, payload);
                break;
        }
    }

    private void handlePluginInfo(Connection connection, ConfigurationSection payload) {
        Map<String, Instant> newPlugins = new ConcurrentHashMap<>();

        for (ConfigurationSection pluginInfo : ConfigUtils.getConfigList(payload, "plugins")) {
            String name = pluginInfo.getString("name");

            Instant modified = Instant.ofEpochMilli(pluginInfo.getLong("modified"));

            newPlugins.put(name, modified);

            log.trace("Client plugin: " + name + ", " + modified);

            checkUpdate(connection.getMinecraftServer(), name);
        }

        clientVersionsMap.put(connection.getMinecraftServer(), newPlugins);
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
        }

        updatePluginVersion(changed);
    }

    private static class ServerVersionInfo {
        private Instant dateModified;
        private Path pluginPath;
    }
}
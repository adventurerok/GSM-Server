package com.ithinkrok.msm.server;

import com.ithinkrok.msm.server.command.ServerCommandInfo;
import com.ithinkrok.msm.server.event.player.PlayerCommandEvent;
import com.ithinkrok.msm.server.impl.MSMPluginLoader;
import com.ithinkrok.msm.server.permission.PermissionInfo;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.event.CustomEventHandler;
import com.ithinkrok.util.event.CustomListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by paul on 02/02/16.
 */
public abstract class MSMServerPlugin {

    private final Logger logger;
    //This field is accessed by reflection.
    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private boolean configured = false;
    //Accessed by reflection
    @SuppressWarnings("unused")
    private String name;
    //Accessed by reflection
    @SuppressWarnings("unused")
    private Config pluginYml;

    //Accessed by reflection
    @SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
    private boolean enabled = false;

    private Map<String, ServerCommandInfo> commands;

    private List<PermissionInfo> permissions;

    private Server server;

    public MSMServerPlugin() {
        logger = LogManager.getLogger(getClass());

        try {
            MSMPluginLoader.configurePlugin(this);
        } catch (ReflectiveOperationException e) {
            logger.warn("Failed to configure plugin: " + name, e);
        }
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getVersion() {
        return pluginYml.getString("version");
    }

    public List<String> getDependencies() {
        List<String> dependencies = pluginYml.getStringList("depend");

        return dependencies != null ? dependencies : Collections.emptyList();
    }

    public List<String> getSoftDependencies() {
        List<String> softDependencies = pluginYml.getStringList("softdepend");

        return softDependencies != null ? softDependencies : Collections.emptyList();
    }

    public String getName() {
        return name;
    }

    public void onEnable() {

    }

    public void onCommand(PlayerCommandEvent event) {

    }

    public Path getDataDirectory() {
        return Paths.get("plugins", getName());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, ServerListener> getProtocols() {
        return new HashMap<>();
    }

    public Map<String, ServerCommandInfo> getCommands() {
        if (commands == null) {
            Map<String, ServerCommandInfo> commands = new ConcurrentHashMap<>();

            Config commandConfigs = pluginYml.getConfigOrEmpty("commands");

            CustomListener listener = new MSMPluginListener();

            for (String commandName : commandConfigs.getKeys(false)) {
                Config commandConfig = commandConfigs.getConfigOrNull(commandName);

                ServerCommandInfo commandInfo = new ServerCommandInfo(commandName, commandConfig, listener);

                commands.put(commandName, commandInfo);
            }

            this.commands = commands;
        }

        return commands;
    }

    public List<PermissionInfo> getPermissions() {
        if (permissions == null) {
            List<PermissionInfo> permissions = new ArrayList<>();

            Config permissionConfigs = pluginYml.getConfigOrEmpty("permissions");

            for (String permissionName : permissionConfigs.getKeys(false)) {
                Config permissionConfig = permissionConfigs.getConfigOrNull(permissionName);
                PermissionInfo permissionInfo = new PermissionInfo(permissionName, permissionConfig);

                permissions.add(permissionInfo);
            }

            this.permissions = permissions;
        }

        return permissions;
    }

    private class MSMPluginListener implements CustomListener {

        @CustomEventHandler
        public void onPlayerCommand(PlayerCommandEvent event) {
            onCommand(event);
        }

    }
}

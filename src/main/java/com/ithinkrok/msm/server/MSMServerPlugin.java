package com.ithinkrok.msm.server;

import com.ithinkrok.msm.server.command.CommandInfo;
import com.ithinkrok.msm.server.event.player.PlayerCommandEvent;
import com.ithinkrok.msm.server.impl.MSMPluginLoader;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.event.CustomEventHandler;
import com.ithinkrok.util.event.CustomListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private Map<String, CommandInfo> commands;

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

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, ServerListener> getProtocols() {
        return new HashMap<>();
    }

    public Map<String, CommandInfo> getCommands() {
        if(commands == null) {
            Map<String, CommandInfo> commands = new ConcurrentHashMap<>();

            Config commandConfigs = pluginYml.getConfigOrEmpty("commands");

            CustomListener listener = new MSMPluginListener();

            for(String commandName : commandConfigs.getKeys(false)) {
                Config commandConfig = commandConfigs.getConfigOrNull(commandName);

                String usage = commandConfig.getString("usage");
                String description = commandConfig.getString("description");
                String permission = commandConfig.getString("permission");

                CommandInfo commandInfo = new CommandInfo(commandName, usage, description, permission, listener);

                commands.put(commandName, commandInfo);
            }

            this.commands = commands;
        }

        return commands;
    }

    private class MSMPluginListener implements CustomListener {

        @CustomEventHandler
        public void onPlayerCommand(PlayerCommandEvent event) {
            onCommand(event);
        }

    }
}

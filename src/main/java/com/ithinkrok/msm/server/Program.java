package com.ithinkrok.msm.server;

import com.ithinkrok.msm.server.command.CommandInfo;
import com.ithinkrok.msm.server.command.ConsoleCommandSender;
import com.ithinkrok.msm.server.event.ConsoleCommandEvent;
import com.ithinkrok.msm.server.impl.MSMPluginLoader;
import com.ithinkrok.msm.server.impl.MSMServer;
import com.ithinkrok.msm.server.protocol.ServerAPIProtocol;
import com.ithinkrok.msm.server.protocol.ServerAutoUpdateProtocol;
import com.ithinkrok.util.command.CustomCommand;
import com.ithinkrok.util.event.CustomEventExecutor;
import jline.console.ConsoleReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by paul on 27/01/16.
 */
public class Program {

    private static final Logger log = LogManager.getLogger(Program.class);

    public static void main(String[] args) {
        log.info("Starting MSM Server");

        MSMServer server = load();

        log.info("Supported protocols: " + server.getAvailableProtocols());

        server.start();

        runConsole(server);
    }

    private static void runConsole(MSMServer server) {
        ConsoleReader reader;
        try {
            reader = new ConsoleReader();
        } catch (IOException e) {
            log.error("Failed to create console reader. Console input will be disabled", e);
            return;
        }

        reader.setPrompt("> ");

        ConsoleCommandSender commandSender = new ConsoleCommandSender(server);

        String line;
        try {
            while((line = reader.readLine()) != null) {
                CustomCommand command = new CustomCommand(line);

                CommandInfo commandInfo = server.getCommand(command.getCommand());

                if(commandInfo == null) {
                    commandSender.sendMessage("Unknown MSM command: " + command.getCommand());
                    continue;
                }

                ConsoleCommandEvent commandEvent = new ConsoleCommandEvent(commandSender, command);

                CustomEventExecutor.executeEvent(commandEvent, commandInfo.getCommandListener());

                if(!commandEvent.isValidCommand()) {
                    commandSender.sendMessage("Usage: " + commandInfo.getUsage());
                } else if(!commandEvent.isHandled()) {
                    commandSender.sendMessage("This command does not support the console");
                }
            }
        } catch (IOException e) {
            log.error("Error while reading console line. Future console input disabled", e);
        }
    }

    private static MSMServer load() {
        MSMPluginLoader pluginLoader = new MSMPluginLoader();

        log.info("Loading plugins...");
        List<MSMServerPlugin> plugins = pluginLoader.loadPlugins();
        log.info("Finished loading plugins");

        Map<String, ServerListener> listenerMap = new HashMap<>();

        //Add all default protocols (except MSMLogin)
        listenerMap.put("MSMAutoUpdate", new ServerAutoUpdateProtocol(Paths.get("updates/bukkit_plugins")));
        listenerMap.put("MSMAPI", new ServerAPIProtocol());

        MSMServer server = new MSMServer(30824, listenerMap);

        for(MSMServerPlugin plugin : plugins) {
            plugin.setServer(server);
        }

        log.info("Enabling plugins...");
        pluginLoader.enablePlugins(plugins);
        log.info("Finished enabling plugins");

        for(MSMServerPlugin plugin : plugins) {
            server.registerProtocols(plugin.getProtocols());
        }
        return server;
    }
}

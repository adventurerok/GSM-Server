package com.ithinkrok.msm.server;

import com.ithinkrok.msm.server.command.*;
import com.ithinkrok.msm.server.console.ConsoleHandler;
import com.ithinkrok.msm.server.impl.MSMPluginLoader;
import com.ithinkrok.msm.server.impl.MSMServer;
import com.ithinkrok.msm.server.minecraft.ServerMinecraftRequestProtocol;
import com.ithinkrok.msm.server.protocol.ServerAPIProtocol;
import com.ithinkrok.msm.server.protocol.ServerAutoUpdateProtocol;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.config.YamlConfigIO;
import jline.console.ConsoleReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
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

        registerDefaultCommands(server);

        server.start();

        ConsoleReader reader;
        try {
            reader = new ConsoleReader();
        } catch (IOException e) {
            log.error("Failed to create console reader. Console input will be disabled", e);
            return;
        }

        ConsoleHandler consoleHandler = new ConsoleHandler(server, reader);

        server.setConsoleHandler(consoleHandler);

        consoleHandler.runConsole();
    }

    private static void registerDefaultCommands(Server server) {
        Config stopConfig = new MemoryConfig();
        stopConfig.set("usage", "/<command>");
        stopConfig.set("description", "Stops the MSM server");
        stopConfig.set("permission", "msmserver.stop");

        ServerCommandInfo stopInfo = new ServerCommandInfo("mstop", stopConfig, new StopCommand());

        CommandHandler commandHandler = server.getCommandHandler();
        commandHandler.registerCommand(stopInfo);

        Config restartConfig = new MemoryConfig();
        stopConfig.set("usage", "/<command>");
        stopConfig.set("description", "Stops the MSM server");
        stopConfig.set("permission", "msmserver.restart");

        ServerCommandInfo restartInfo = new ServerCommandInfo("mrestart", restartConfig, new RestartCommand());

        commandHandler.registerCommand(restartInfo);

        commandHandler.registerCommand(ExecCommand.createCommandInfo());
        commandHandler.registerCommand(LoadCommand.createCommandInfo());
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
        listenerMap.put("MinecraftRequest", new ServerMinecraftRequestProtocol());

        MSMServer server = new MSMServer(loadConfig(), listenerMap);

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

    private static Config loadConfig() {
        Path configPath = Paths.get("config.yml");

        try{
            return YamlConfigIO.loadToConfig(configPath, new MemoryConfig());
        } catch (IOException e) {
            log.warn("Failed to load config", e);
        }

        return new MemoryConfig();
    }

}

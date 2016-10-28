package com.ithinkrok.msm.server;

import com.ithinkrok.msm.server.command.*;
import com.ithinkrok.msm.server.console.ConsoleHandler;
import com.ithinkrok.msm.server.external.DiscordChat;
import com.ithinkrok.msm.server.impl.MSMPluginLoader;
import com.ithinkrok.msm.server.impl.MSMServer;
import com.ithinkrok.msm.server.listener.ClientDownListener;
import com.ithinkrok.msm.server.minecraft.ServerMinecraftRequestProtocol;
import com.ithinkrok.msm.server.protocol.ServerAPIProtocol;
import com.ithinkrok.msm.server.protocol.ServerAutoUpdateProtocol;
import com.ithinkrok.msm.server.web.WebPanel;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.config.YamlConfigIO;
import jline.console.ConsoleReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sx.blah.discord.util.DiscordException;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ithinkrok.msm.server.impl.MSMServer.DEFAULT_PORT;

/**
 * Created by paul on 27/01/16.
 */
public class Program {

    private static final Logger log = LogManager.getLogger(Program.class);

    private static boolean portAvailable(int port) {
        try (Socket ignored = new Socket("localhost", port)) {
            return false;
        } catch (IOException ignored) {
            return true;
        }
    }

    public static void main(String[] args) {
        log.info("Starting MSM Server");

        Config config = loadConfig();

        if(!portAvailable(config.getInt("port", DEFAULT_PORT))) {
            log.fatal("Port not available, stopping");

            System.exit(1);
            return;
        }

        MSMServer server = load(config);

        log.info("Supported protocols: " + server.getAvailableProtocols());

        registerCustomListeners(server, config);
        registerDefaultCommands(server);
        registerExternals(server, config);

        server.start();

        ConsoleReader reader;
        try {
            reader = new ConsoleReader();
        } catch (IOException e) {
            log.error("Failed to create console reader. Console input will be disabled", e);
            return;
        }

        WebPanel webPanel = new WebPanel(server, Paths.get("web"));
        try {
            webPanel.start();
        } catch (IOException e) {
            log.warn("Failed to start web panel", e);
        }

        ConsoleHandler consoleHandler = new ConsoleHandler(server, reader);

        server.setConsoleHandler(consoleHandler);

        consoleHandler.runConsole();
    }

    private static Config loadConfig() {
        Path configPath = Paths.get("config.yml");

        try {
            return YamlConfigIO.loadToConfig(configPath, new MemoryConfig());
        } catch (IOException e) {
            log.warn("Failed to load config", e);
        }

        return new MemoryConfig();
    }

    private static MSMServer load(Config config) {
        MSMPluginLoader pluginLoader = new MSMPluginLoader();

        log.info("Loading plugins...");
        List<MSMServerPlugin> plugins = pluginLoader.loadPlugins();
        log.info("Finished loading plugins");

        Map<String, ServerListener> listenerMap = new HashMap<>();

        //Add all default protocols (except MSMLogin)
        listenerMap.put("BukkitPluginUpdate",
                        new ServerAutoUpdateProtocol("BukkitPluginUpdate", Paths.get("updates/bukkit_plugins")));
        listenerMap.put("MSMAPI", new ServerAPIProtocol());
        listenerMap.put("MinecraftRequest", new ServerMinecraftRequestProtocol());

        MSMServer server = new MSMServer(config, listenerMap);

        for (MSMServerPlugin plugin : plugins) {
            plugin.setServer(server);
        }

        log.info("Enabling plugins...");
        pluginLoader.enablePlugins(plugins);
        log.info("Finished enabling plugins");

        for (MSMServerPlugin plugin : plugins) {
            server.registerProtocols(plugin.getProtocols());
        }
        return server;
    }

    private static void registerCustomListeners(Server server, Config config) {
        if (!config.contains("scripts")) return;

        Config scriptsConfig = config.getConfigOrEmpty("scripts");

        if (scriptsConfig.contains("client_down")) {
            log.info("Registered the client down listener");
            server.registerListener(new ClientDownListener(scriptsConfig.getConfigOrEmpty("client_down")));
        }
    }

    private static void registerDefaultCommands(Server server) {
        CommandHandler commandHandler = server.getCommandHandler();

        commandHandler.registerCommand(StopCommand.createCommandInfo());
        commandHandler.registerCommand(RestartCommand.createCommandInfo());
        commandHandler.registerCommand(ExecCommand.createCommandInfo());
        commandHandler.registerCommand(LoadCommand.createCommandInfo());
    }

    private static void registerExternals(MSMServer server, Config config) {
        if (!config.contains("external")) return;

        Config externalConfig = config.getConfigOrEmpty("external");

        if (externalConfig.contains("discord")) {
            Config discordConfig = externalConfig.getConfigOrEmpty("discord");

            try {
                DiscordChat chat = new DiscordChat(server, discordConfig);

                server.addExternal(chat);
            } catch (DiscordException e) {
                log.warn("Failed to create DiscordChat", e);
            }
        }
    }

}

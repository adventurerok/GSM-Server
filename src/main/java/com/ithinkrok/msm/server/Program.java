package com.ithinkrok.msm.server;

import com.ithinkrok.msm.server.impl.MSMPluginLoader;
import com.ithinkrok.msm.server.impl.MSMServer;
import com.ithinkrok.msm.server.protocol.ServerAutoUpdateProtocol;
import com.ithinkrok.msm.server.protocol.ServerAPIProtocol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

        MSMPluginLoader pluginLoader = new MSMPluginLoader();

        log.info("Loading plugins...");
        List<MSMServerPlugin> plugins = pluginLoader.loadPlugins();
        log.info("Finished loading plugins");

        Map<String, ServerListener> listenerMap = new HashMap<>();

        //Add all default protocols (except MSMLogin)
        listenerMap.put("MSMAutoUpdate", new ServerAutoUpdateProtocol(Paths.get("updates/bukkit_plugins")));
        listenerMap.put("MSMAPI", new ServerAPIProtocol());

        //Add plugin protocols
        for(MSMServerPlugin plugin : plugins) {
            listenerMap.putAll(plugin.getProtocols());
        }

        MSMServer server = new MSMServer(30824, listenerMap);

        for(MSMServerPlugin plugin : plugins) {
            plugin.setServer(server);
        }

        log.info("Enabling plugins...");
        pluginLoader.enablePlugins(plugins);
        log.info("Finished enabling plugins");

        server.start();
    }
}

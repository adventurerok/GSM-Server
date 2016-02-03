package com.ithinkrok.msm.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

        Map<String, MSMServerListener> listenerMap = new HashMap<>();

        for(MSMServerPlugin plugin : plugins) {
            if(plugin.hasProtocol()) listenerMap.put(plugin.getProtocol(), plugin);
        }

        MSMServer server = new MSMServer(30824, listenerMap);

        server.start();
    }
}

package com.ithinkrok.msm.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by paul on 27/01/16.
 */
public class Program {

    private static final Logger log = LogManager.getLogger(Program.class);

    public static void main(String[] args) {
        log.info("Starting MSM Server");

        MSMPluginLoader pluginLoader = new MSMPluginLoader();

        log.info("Loading plugins...");
        pluginLoader.loadPlugins();
        log.info("Finished loading plugins");

        MSMServer server = new MSMServer(30824);

        server.start();
    }
}

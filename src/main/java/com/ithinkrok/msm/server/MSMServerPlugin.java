package com.ithinkrok.msm.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Created by paul on 02/02/16.
 */
public abstract class MSMServerPlugin implements MSMServerListener {

    //This field is accessed by reflection.
    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private boolean configured = false;

    //Accessed by reflection
    @SuppressWarnings("unused")
    private String name;

    //Accessed by reflection
    @SuppressWarnings("unused")
    private FileConfiguration pluginYml;

    private Logger logger;

    public MSMServerPlugin()  {
        logger = LogManager.getLogger(getClass());

        try {
            MSMPluginLoader.configurePlugin(this);
        } catch (ReflectiveOperationException e) {
            logger.warn("Failed to configure plugin: " + name, e);
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public String getVersion() {
        return pluginYml.getString("version");
    }

    public String getProtocol() {
        return pluginYml.getString("protocol");
    }

    public String getName() {
        return name;
    }

    @Override
    public void connectionOpened(MSMConnection connection) {

    }

    @Override
    public void connectionClosed(MSMConnection connection) {

    }

    public boolean hasProtocol() {
        return pluginYml.contains("protocol");
    }
}

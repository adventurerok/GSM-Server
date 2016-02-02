package com.ithinkrok.msm.server;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Created by paul on 02/02/16.
 */
public abstract class MSMServerPlugin implements MSMServerListener {

    private boolean configured = false;

    private String name;
    private FileConfiguration pluginYml;

    public MSMServerPlugin() throws ReflectiveOperationException {
        MSMPluginLoader.configurePlugin(this);
    }

    public String getVersion() {
        return pluginYml.getString("version");
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

}

package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.ServerListener;
import org.bukkit.configuration.ConfigurationSection;

import java.nio.file.Path;

/**
 * Created by paul on 05/02/16.
 */
public class ServerAutoUpdateProtocol implements ServerListener {

    private final Path updatedPluginsPath;

    public ServerAutoUpdateProtocol(Path updatedPluginsPath) {
        this.updatedPluginsPath = updatedPluginsPath;


    }

    @Override
    public void connectionOpened(Connection connection, Channel channel) {

    }

    @Override
    public void connectionClosed(Connection connection, Channel channel) {

    }

    @Override
    public void packetRecieved(Connection connection, Channel channel, ConfigurationSection payload) {

        String mode = payload.getString("mode");
        if(mode == null) return;

        switch (mode) {
            case "PluginInfo":
                handlePluginInfo(payload);
                break;
        }
    }

    private void handlePluginInfo(ConfigurationSection payload) {

    }
}

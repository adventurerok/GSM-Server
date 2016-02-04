package com.ithinkrok.msm.server;

import com.ithinkrok.msm.common.Channel;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Created by paul on 02/02/16.
 */
public interface ServerListener {

    void connectionOpened(Connection connection);

    void connectionClosed(Connection connection);

    void packetRecieved(Connection connection, Channel channel, ConfigurationSection payload);
}

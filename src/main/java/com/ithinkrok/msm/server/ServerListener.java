package com.ithinkrok.msm.server;

import com.ithinkrok.msm.common.Channel;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Created by paul on 02/02/16.
 */
public interface ServerListener {

    void connectionOpened(Connection connection, Channel channel);

    void connectionClosed(Connection connection, Channel channel);

    void packetRecieved(Connection connection, Channel channel, ConfigurationSection payload);
}

package com.ithinkrok.msm.server;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.util.config.Config;

/**
 * Created by paul on 02/02/16.
 */
public interface ServerListener {

    default void serverStarted(Server server){

    }

    default void serverStopped(Server server){

    }

    void connectionOpened(Connection connection, Channel channel);

    void connectionClosed(Connection connection);

    void packetRecieved(Connection connection, Channel channel, Config payload);
}

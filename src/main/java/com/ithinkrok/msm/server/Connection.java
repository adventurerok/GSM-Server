package com.ithinkrok.msm.server;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.server.data.MinecraftClient;

import java.util.Collection;

/**
 * Created by paul on 04/02/16.
 */
public interface Connection {

    Channel getChannel(String protocol);

    /**
     * @return The Server that this Connection is connected to
     */
    Server getConnectedTo();

    /**
     * @return The MinecraftClient this Connection is representing.
     */
    MinecraftClient getMinecraftServer();

    Collection<String> getSupportedProtocols();

    void close();
}

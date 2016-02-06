package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.common.MinecraftServerInfo;
import com.ithinkrok.msm.server.MinecraftServer;

/**
 * Created by paul on 05/02/16.
 */
public class MSMMinecraftServer implements MinecraftServer {

    private MSMConnection connection;

    private MinecraftServerInfo serverInfo;

    @Override
    public MSMConnection getConnection() {
        return connection;
    }

    @Override
    public MinecraftServerInfo getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(MinecraftServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    public void setConnection(MSMConnection connection) {
        this.connection = connection;
    }
}

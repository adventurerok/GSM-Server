package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.server.MinecraftServer;

/**
 * Created by paul on 05/02/16.
 */
public class MSMMinecraftServer implements MinecraftServer {

    private MSMConnection connection;

    @Override
    public MSMConnection getConnection() {
        return connection;
    }

    public void setConnection(MSMConnection connection) {
        this.connection = connection;
    }
}

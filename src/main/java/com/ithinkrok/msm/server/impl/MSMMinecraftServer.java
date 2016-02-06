package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.common.MinecraftServerInfo;
import com.ithinkrok.msm.common.MinecraftServerType;
import com.ithinkrok.msm.server.MinecraftServer;

import java.util.List;

/**
 * Created by paul on 05/02/16.
 */
public class MSMMinecraftServer implements MinecraftServer {

    private MSMConnection connection;

    private final MinecraftServerInfo serverInfo;

    public MSMMinecraftServer(MinecraftServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    @Override
    public MSMConnection getConnection() {
        return connection;
    }

    @Override
    public MinecraftServerInfo getServerInfo() {
        return serverInfo;
    }

    @Override
    public String getName() {
        return serverInfo.getName();
    }

    @Override
    public MinecraftServerType getType() {
        return getServerInfo().getType();
    }

    @Override
    public boolean hasBungee() {
        return getServerInfo().hasBungee();
    }

    @Override
    public int getMaxPlayerCount() {
        return getServerInfo().getMaxPlayerCount();
    }

    @Override
    public List<String> getPlugins() {
        return getServerInfo().getPlugins();
    }

    public void setConnection(MSMConnection connection) {
        this.connection = connection;
    }
}

package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.common.MinecraftServerInfo;
import com.ithinkrok.msm.common.MinecraftServerType;
import com.ithinkrok.msm.server.MinecraftServer;
import com.ithinkrok.msm.server.Player;
import com.ithinkrok.msm.server.Server;

import java.util.*;

/**
 * Created by paul on 05/02/16.
 */
public class MSMMinecraftServer implements MinecraftServer {

    private final MinecraftServerInfo serverInfo;
    private final MSMServer server;

    private MSMConnection connection;

    private final List<MSMPlayer> players = new ArrayList<>();

    private Collection<String> supportedProtocols;

    public MSMMinecraftServer(MinecraftServerInfo serverInfo, MSMServer server) {
        this.serverInfo = serverInfo;
        this.server = server;
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

    @Override
    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public void addPlayer(MSMPlayer player) {
        players.add(player);
    }

    public void setConnection(MSMConnection connection) {
        this.connection = connection;

        setSupportedProtocols(connection.getSupportedProtocols());
    }

    @Override
    public String toString() {
        return "MSMMinecraftServer{" + "name=" + serverInfo.getName() + "}";
    }

    public MSMPlayer removePlayer(UUID playerUUID) {
        Iterator<MSMPlayer> playerIterator = players.iterator();

        while(playerIterator.hasNext()){
            MSMPlayer next = playerIterator.next();

            if(!next.getUUID().equals(playerUUID)) continue;

            playerIterator.remove();
            return next;
        }

        return null;
    }

    @Override
    public Collection<String> getSupportedProtocols() {
        return supportedProtocols;
    }

    public void setSupportedProtocols(Collection<String> supportedProtocols) {
        this.supportedProtocols = supportedProtocols;
    }

    @Override
    public Server getConnectedTo() {
        return server;
    }
}

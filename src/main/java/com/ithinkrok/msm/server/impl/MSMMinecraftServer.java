package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.common.MinecraftServerInfo;
import com.ithinkrok.msm.common.MinecraftServerType;
import com.ithinkrok.msm.server.MinecraftServer;
import com.ithinkrok.msm.server.Player;
import com.ithinkrok.msm.server.Server;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by paul on 05/02/16.
 */
public class MSMMinecraftServer implements MinecraftServer {

    private final MinecraftServerInfo serverInfo;
    private final MSMServer server;

    private MSMConnection connection;

    private final Map<UUID, MSMPlayer> players = new ConcurrentHashMap<>();

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
    public Collection<? extends Player> getPlayers() {
        return players.values();
    }

    public void addPlayer(MSMPlayer player) {
        players.put(player.getUUID(), player);
    }

    public void setConnection(MSMConnection connection) {
        this.connection = connection;

        if(connection != null) setSupportedProtocols(connection.getSupportedProtocols());
    }

    @Override
    public String toString() {
        return "MSMMinecraftServer{" + "name=" + serverInfo.getName() + "}";
    }

    public MSMPlayer removePlayer(UUID playerUUID) {
        Iterator<MSMPlayer> playerIterator = players.values().iterator();

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

    private Channel getAPIChannel() {
        return connection.getChannel("MSMAPI");
    }

    public boolean isConnected() {
        return connection != null;
    }

    @Override
    public Server getConnectedTo() {
        return server;
    }

    @Override
    public void messagePlayers(String message, Collection<? extends Player> players) {
        List<String> playerUUIDs = new ArrayList<>();

        for(Player player : players) {
            playerUUIDs.add(player.getUUID().toString());
        }

        ConfigurationSection payload = new MemoryConfiguration();

        payload.set("recipients", playerUUIDs);
        payload.set("message", message);
        payload.set("mode", "Message");

        getAPIChannel().write(payload);
    }

    @Override
    public void broadcast(String message) {
        ConfigurationSection payload = new MemoryConfiguration();

        payload.set("message", message);
        payload.set("mode", "Broadcast");

        getAPIChannel().write(payload);
    }

    @Override
    public MSMPlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }
}

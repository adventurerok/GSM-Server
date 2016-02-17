package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.common.MinecraftServerInfo;
import com.ithinkrok.msm.common.MinecraftServerType;
import com.ithinkrok.msm.server.MinecraftServer;
import com.ithinkrok.msm.server.Player;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;

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

    private final Map<String, MSMPlayer> namedPlayers = new ConcurrentHashMap<>();

    private Collection<String> supportedProtocols;

    private double tps;

    private double ramUsage;

    private double allocatedRam;

    private double maxRam;

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

        namedPlayers.put(player.getName(), player);
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
        MSMPlayer player = players.remove(playerUUID);

        if(player != null) {
            namedPlayers.remove(player.getName());
        }

        return player;
    }

    public void handleResourceUsagePacket(Config payload) {
        tps = payload.getDouble("average_tps");
        ramUsage = payload.getDouble("average_ram");
        maxRam = payload.getDouble("max_ram");
        allocatedRam = payload.getDouble("allocated_ram");
    }

    @Override
    public double getTPS() {
        return tps;
    }

    @Override
    public double getRamUsage() {
        return ramUsage;
    }

    @Override
    public double getMaxRam() {
        return maxRam;
    }

    @Override
    public double getAllocatedRam() {
        return allocatedRam;
    }

    @Override
    public Collection<String> getSupportedProtocols() {
        return supportedProtocols;
    }

    public void setSupportedProtocols(Collection<String> supportedProtocols) {
        this.supportedProtocols = supportedProtocols;
    }

    private Channel getAPIChannel() {
        if(connection == null) return null;
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
        if(getAPIChannel() == null) return;
        List<String> playerUUIDs = new ArrayList<>();

        for(Player player : players) {
            playerUUIDs.add(player.getUUID().toString());
        }

        Config payload = new MemoryConfig();

        payload.set("recipients", playerUUIDs);
        payload.set("message", message);
        payload.set("mode", "Message");

        getAPIChannel().write(payload);
    }

    @Override
    public void broadcast(String message) {
        if(getAPIChannel() == null) return;

        Config payload = new MemoryConfig();

        payload.set("message", message);
        payload.set("mode", "Broadcast");

        getAPIChannel().write(payload);
    }

    @Override
    public MSMPlayer getPlayer(UUID uuid) {
        if(uuid == null) return null;
        return players.get(uuid);
    }

    @Override
    public MSMPlayer getPlayer(String name) {
        if(name == null) return null;
        return namedPlayers.get(name);
    }
}

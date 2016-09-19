package com.ithinkrok.msm.server.minecraft.impl;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.common.MinecraftClientInfo;
import com.ithinkrok.msm.common.MinecraftClientType;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.data.Ban;
import com.ithinkrok.msm.server.data.Player;
import com.ithinkrok.msm.server.impl.MSMServer;
import com.ithinkrok.msm.server.minecraft.MinecraftClient;
import com.ithinkrok.msm.server.minecraft.MinecraftPlayer;
import com.ithinkrok.util.command.CustomCommandSender;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.lang.LanguageLookup;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by paul on 05/02/16.
 */
public class MSMMinecraftClient implements MinecraftClient {

    private final MinecraftClientInfo serverInfo;
    private final MSMServer server;
    private final Map<UUID, MinecraftPlayer> players = new ConcurrentHashMap<>();
    private final Map<UUID, Ban> bans = new ConcurrentHashMap<>();
    private final Map<String, MinecraftPlayer> namedPlayers = new ConcurrentHashMap<>();
    private final MinecraftCommandSender commandSender = new MinecraftCommandSender(this);
    private Connection connection;
    private Collection<String> supportedProtocols;

    private double tps;

    private double ramUsage;

    private double allocatedRam;

    private double maxRam;

    private boolean restartScheduled;

    public MSMMinecraftClient(MinecraftClientInfo serverInfo, MSMServer server) {
        this.serverInfo = serverInfo;
        this.server = server;
    }

    @Override
    public String toString() {
        return "MSMMinecraftClient{" + "name=" + serverInfo.getName() + "}";
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
    public MinecraftClientInfo getClientInfo() {
        return serverInfo;
    }

    @Override
    public MinecraftClientType getSubType() {
        return getClientInfo().getSubType();
    }

    @Override
    public boolean hasBungee() {
        return getClientInfo().hasBungee();
    }

    @Override
    public List<String> getPlugins() {
        return getClientInfo().getPlugins();
    }

    public boolean isConnected() {
        return connection != null;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;

        if (connection != null){
            //If the server reconnected it just restarted
            restartScheduled = false;

            setSupportedProtocols(connection.getSupportedProtocols());
        }
    }

    public MinecraftPlayer removePlayer(UUID playerUUID) {
        MinecraftPlayer player = players.remove(playerUUID);

        if (player != null) {
            namedPlayers.remove(player.getName());
        }

        return player;
    }

    public void addPlayer(MinecraftPlayer player) {
        players.put(player.getUUID(), player);

        namedPlayers.put(player.getName(), player);
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
    public double getPerformance() {
        return getTPS() / 20d;
    }

    @Override
    public Collection<String> getSupportedProtocols() {
        return supportedProtocols;
    }

    @Override
    public String getName() {
        return serverInfo.getName();
    }

    @Override
    public int getMaxPlayerCount() {
        return getClientInfo().getMaxPlayerCount();
    }

    @Override
    public Server getConnectedTo() {
        return server;
    }

    @Override
    public CustomCommandSender getConsoleCommandSender() {
        return commandSender;
    }

    @Override
    public Collection<? extends MinecraftPlayer> getPlayers() {
        return players.values();
    }

    @Override
    public void messagePlayers(String message, Collection<? extends Player<?>> players) {
        if (getAPIChannel() == null) return;
        List<String> playerUUIDs = new ArrayList<>();

        for (Player<?> player : players) {
            playerUUIDs.add(player.getUUID().toString());
        }

        Config payload = new MemoryConfig();

        payload.set("recipients", playerUUIDs);
        payload.set("message", message);
        payload.set("mode", "Message");

        getAPIChannel().write(payload);
    }

    @Override
    public void scheduleRestart(int delayMs, int maxRestartPlayers) {
        Channel channel = getAPIChannel();
        if(channel == null) return;

        setRestartScheduled();

        Config payload = new MemoryConfig();
        payload.set("delay_ms", delayMs);
        payload.set("max_players", maxRestartPlayers);
        payload.set("mode", "Restart");

        channel.write(payload);
    }

    private Channel getAPIChannel() {
        if (connection == null) return null;
        return connection.getChannel("MSMAPI");
    }

    @Override
    public MinecraftPlayer getPlayer(UUID uuid) {
        if (uuid == null) return null;
        return players.get(uuid);
    }

    @Override
    public MinecraftPlayer getPlayer(String name) {
        if (name == null) return null;
        return namedPlayers.get(name);
    }

    @Override
    public void broadcast(String message) {
        if (getAPIChannel() == null) return;

        Config payload = new MemoryConfig();

        payload.set("message", message);
        payload.set("mode", "Broadcast");

        getAPIChannel().write(payload);
    }

    @Override
    public Ban getBan(UUID playerUUID) {
        return bans.get(playerUUID);
    }

    @Override
    public boolean isBanned(UUID playerUUID) {
        return bans.containsKey(playerUUID);
    }

    @Override
    public boolean unbanPlayer(UUID playerUUID) {
        if (!isBanned(playerUUID)) return true;

        Channel channel = getAPIChannel();
        if (channel == null) return false;

        bans.remove(playerUUID);

        Config payload = new MemoryConfig();
        payload.set("player", playerUUID.toString());
        payload.set("mode", "Unban");

        channel.write(payload);
        return true;
    }

    @Override
    public boolean banPlayer(Ban ban) {
        if (ban.equals(getBan(ban.getPlayerUUID()))) return true;

        Channel channel = getAPIChannel();
        if (channel == null) return false;

        addBan(ban);

        Config payload = ban.toConfig();
        payload.set("mode", "Ban");

        channel.write(payload);
        return true;
    }

    @Override
    public MinecraftPlayer createPlayer(Config config) {
        return new MSMMinecraftPlayer(this, config);
    }

    public void addBan(Ban ban) {
        bans.put(ban.getPlayerUUID(), ban);
    }

    public void setSupportedProtocols(Collection<String> supportedProtocols) {
        this.supportedProtocols = supportedProtocols;
    }

    @Override
    public void sendMessage(String message) {
        sendMessageNoPrefix(message);
    }

    @Override
    public void sendMessageNoPrefix(String message) {
        broadcast(message);
    }

    @Override
    public void sendLocale(String locale, Object... args) {
        sendLocaleNoPrefix(locale, args);
    }

    @Override
    public void sendLocaleNoPrefix(String locale, Object... args) {
        sendMessageNoPrefix(server.getLocale(locale, args));
    }

    @Override
    public LanguageLookup getLanguageLookup() {
        return server.getLanguageLookup();
    }

    @Override
    public boolean isRestartScheduled() {
        return restartScheduled;
    }

    @Override
    public void setRestartScheduled() {
        this.restartScheduled = true;
    }
}

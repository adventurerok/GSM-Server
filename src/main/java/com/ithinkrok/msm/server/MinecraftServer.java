package com.ithinkrok.msm.server;

import com.ithinkrok.msm.common.MinecraftServerInfo;
import com.ithinkrok.msm.common.MinecraftServerType;
import com.ithinkrok.msm.server.impl.MSMPlayer;

import java.util.Collection;
import java.util.List;

/**
 * Created by paul on 05/02/16.
 * <p>
 * Represents any kind of Minecraft Server (Vanilla/Bukkit/Spigot/Bungee etc...) that could be connected to this MSM
 * Server.
 */
public interface MinecraftServer {

    /**
     * @return If this MinecraftServer is currently connected to the MSM Server
     */
    default boolean isConnected() {
        return getConnection() != null;
    }

    /**
     * @return The Connection representing this MinecraftServer, or {@code null} if none exists
     */
    Connection getConnection();

    Collection<String> getSupportedProtocols();

    MinecraftServerInfo getServerInfo();

    String getName();


    MinecraftServerType getType();

    boolean hasBungee();

    int getMaxPlayerCount();

    List<String> getPlugins();

    List<Player> getPlayers();

    Server getConnectedTo();

    void messagePlayers(String message, Player...players);

    void broadcast(String message);
}

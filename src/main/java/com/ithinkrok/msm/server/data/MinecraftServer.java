package com.ithinkrok.msm.server.data;

import com.ithinkrok.msm.common.MinecraftServerInfo;
import com.ithinkrok.msm.common.MinecraftServerType;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.util.command.CustomCommandSender;
import com.ithinkrok.util.lang.Messagable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Created by paul on 05/02/16.
 * <p>
 * Represents any kind of Minecraft Server (Vanilla/Bukkit/Spigot/Bungee etc...) that could be connected to this MSM
 * Server.
 */
public interface MinecraftServer extends Messagable {

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

    /**
     * Gets an estimate of the average tps of the server over the last 60 seconds.
     *
     * @return An estimate of the average tps of the server over the last 60 seconds, within 0.2 of the actual value
     */
    double getTPS();

    /**
     * @return An estimate of the average ram usage (in MiB) of the server over the last 60 seconds, within 10% of the
     * actual value
     */
    double getRamUsage();

    /**
     * @return The max ram (in MiB) that the server can use
     */
    double getMaxRam();

    /**
     * @return The current allocated ram (in MiB) of the server
     */
    double getAllocatedRam();

    Collection<String> getSupportedProtocols();

    MinecraftServerInfo getServerInfo();

    String getName();


    MinecraftServerType getType();

    boolean hasBungee();

    int getMaxPlayerCount();

    List<String> getPlugins();

    Collection<? extends Player> getPlayers();

    Server getConnectedTo();

    default void messagePlayers(String message, Player... players) {
        messagePlayers(message, Arrays.asList(players));
    }

    void messagePlayers(String message, Collection<? extends Player> players);

    void broadcast(String message);

    Player getPlayer(UUID uuid);

    Player getPlayer(String name);

    Ban getBan(UUID playerUUID);

    boolean isBanned(UUID playerUUID);

    boolean unbanPlayer(UUID playerUUID);

    boolean banPlayer(Ban ban);

    CustomCommandSender getConsoleCommandSender();
}

package com.ithinkrok.msm.server.data;

import com.ithinkrok.msm.common.ClientInfo;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.util.command.CustomCommandSender;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.lang.Messagable;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

/**
 * Created by paul on 06/03/16.
 */
public interface Client<T extends Player<?>> extends Messagable {
    /**
     * @return If this MinecraftClient is currently connected to the MSM Server
     */
    default boolean isConnected() {
        return getConnection() != null;
    }

    /**
     * @return The Connection representing this MinecraftClient, or {@code null} if none exists
     */
    Connection getConnection();

    void setConnection(Connection connection);

    T removePlayer(UUID playerUUID);

    void addPlayer(T player);

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

    /**
     * Returns a measure of how well the server is performing, with 1.0 being maximum performance, and 0.0 being
     * minimum performance.
     *
     * For minecraft servers, this should return tps/20
     *
     * @return The performance of the server, 1.0 indicating max performance (no issues), 0.0 indicating not performing.
     */
    double getPerformance();

    Collection<String> getSupportedProtocols();

    default String getName(){
        return getClientInfo().getName();
    }

    default String getType(){
        return getClientInfo().getType();
    }

    default int getMaxPlayerCount(){
        return getClientInfo().getMaxPlayerCount();
    }

    Server getConnectedTo();

    CustomCommandSender getConsoleCommandSender();

    Collection<? extends T> getPlayers();

    default void messagePlayers(String message, Player<?>... players) {
        messagePlayers(message, Arrays.asList(players));
    }

    void messagePlayers(String message, Collection<? extends Player<?>> players);

    T getPlayer(UUID uuid);

    T getPlayer(String name);

    void broadcast(String message);

    Ban getBan(UUID playerUUID);

    boolean isBanned(UUID playerUUID);

    boolean unbanPlayer(UUID playerUUID);

    boolean banPlayer(Ban ban);

    ClientInfo getClientInfo();

    T createPlayer(Config config);

    /**
     * Schedules a restart of this client
     *
     * @param delayMs The number of milliseconds the client should wait before restarting
     * @param maxRestartPlayers If more than this number of players are on the client, the client will wait until
     *                          this number of players or less are on the server before restarting
     */
    void scheduleRestart(int delayMs, int maxRestartPlayers);

    void setRestartScheduled();

    boolean isRestartScheduled();
}

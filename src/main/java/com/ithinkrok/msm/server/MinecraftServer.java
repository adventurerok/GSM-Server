package com.ithinkrok.msm.server;

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
}

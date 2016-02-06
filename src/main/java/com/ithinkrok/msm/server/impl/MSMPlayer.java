package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.server.MinecraftServer;
import com.ithinkrok.msm.server.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

/**
 * Created by paul on 06/02/16.
 */
public class MSMPlayer implements Player {

    private final UUID uuid;
    private String name;
    private MinecraftServer minecraftServer;

    public MSMPlayer(MinecraftServer server, ConfigurationSection config) {
        uuid = UUID.fromString(config.getString("uuid"));
        minecraftServer = server;

        fromConfig(config);
    }

    public void fromConfig(ConfigurationSection config) {
        name = config.getString("name");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public void sendMessage(String message) {

    }

    @Override
    public MinecraftServer getServer() {
        return minecraftServer;
    }

    public void setServer(MinecraftServer minecraftServer) {
        this.minecraftServer = minecraftServer;
    }
}

package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.server.MinecraftServer;
import com.ithinkrok.msm.server.Player;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import org.apache.commons.lang.Validate;

import java.util.UUID;

/**
 * Created by paul on 06/02/16.
 */
public class MSMPlayer implements Player {

    private final UUID uuid;
    private String name;
    private MinecraftServer minecraftServer;

    public MSMPlayer(MinecraftServer server, Config config) {
        uuid = UUID.fromString(config.getString("uuid"));
        minecraftServer = server;

        fromConfig(config);
    }

    public void fromConfig(Config config) {
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
        if(!isConnected()) return;

        minecraftServer.messagePlayers(message, this);
    }

    public boolean isConnected() {
        return minecraftServer != null && minecraftServer.getConnection() != null;
    }

    private Channel getAPIChannel() {
        return minecraftServer.getConnection().getChannel("MSMAPI");
    }

    @Override
    public MinecraftServer getServer() {
        return minecraftServer;
    }

    public void setServer(MinecraftServer minecraftServer) {
        this.minecraftServer = minecraftServer;
    }

    @Override
    public void changeServer(MinecraftServer newServer) {
        Validate.notNull(newServer, "newServer cannot be null");

        if(!minecraftServer.hasBungee() || !newServer.hasBungee()){
            throw new UnsupportedOperationException("Cannot change a player between non bungee servers");
        }

        Config payload = new MemoryConfig();

        payload.set("player", uuid);
        payload.set("target", newServer.getName());
        payload.set("mode", "ChangeServer");

        getAPIChannel().write(payload);
    }
}

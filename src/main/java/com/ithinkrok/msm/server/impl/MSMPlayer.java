package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.server.data.MinecraftClient;
import com.ithinkrok.msm.server.data.Player;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.lang.LanguageLookup;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * Created by paul on 06/02/16.
 */
public class MSMPlayer implements Player {

    private static final Logger log = LogManager.getLogger(MSMPlayer.class);

    private final UUID uuid;
    private String name;
    private MinecraftClient minecraftClient;

    public MSMPlayer(MinecraftClient server, Config config) {
        uuid = UUID.fromString(config.getString("uuid"));
        minecraftClient = server;

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
    public MinecraftClient getServer() {
        return minecraftClient;
    }

    public void setServer(MinecraftClient minecraftClient) {
        this.minecraftClient = minecraftClient;
    }

    @Override
    public void changeServer(MinecraftClient newServer) {
        Validate.notNull(newServer, "newServer cannot be null");

        log.trace("ChangeServer player \"" + name + "\": " + minecraftClient.getName() + " -> " + newServer.getName());
        if (!minecraftClient.hasBungee() || !newServer.hasBungee()) {
            log.debug("Blocked ChangeServer request as one of the servers involved does not support bungee");
            return;
        }

        Config payload = new MemoryConfig();

        payload.set("player", uuid.toString());
        payload.set("target", newServer.getName());
        payload.set("mode", "ChangeServer");

        getAPIChannel().write(payload);
    }

    @Override
    public void kick(String reason) {
        Channel channel = getAPIChannel();
        if(channel == null) return;

        Config payload = new MemoryConfig();
        payload.set("player", uuid.toString());
        payload.set("reason", reason);
        payload.set("mode", "Kick");

        channel.write(payload);
    }

    private Channel getAPIChannel() {
        return minecraftClient.getConnection().getChannel("MSMAPI");
    }

    @Override
    public void sendMessage(String message) {
        sendMessageNoPrefix(message);
    }

    @Override
    public void sendMessageNoPrefix(String message) {
        if (!isConnected()) return;

        minecraftClient.messagePlayers(message, this);
    }

    @Override
    public void sendLocale(String locale, Object... args) {
        sendLocaleNoPrefix(locale, args);
    }

    @Override
    public void sendLocaleNoPrefix(String locale, Object... args) {
        sendMessageNoPrefix(minecraftClient.getLanguageLookup().getLocale(locale, args));
    }

    @Override
    public LanguageLookup getLanguageLookup() {
        return minecraftClient.getLanguageLookup();
    }

    public boolean isConnected() {
        return minecraftClient != null && minecraftClient.getConnection() != null;
    }
}

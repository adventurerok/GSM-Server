package com.ithinkrok.msm.server.protocol;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.common.util.ConfigUtils;
import com.ithinkrok.msm.server.*;
import com.ithinkrok.msm.server.event.player.PlayerJoinEvent;
import com.ithinkrok.msm.server.event.player.PlayerQuitEvent;
import com.ithinkrok.msm.server.impl.MSMMinecraftServer;
import com.ithinkrok.msm.server.impl.MSMPlayer;
import io.netty.channel.EventLoop;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Created by paul on 06/02/16.
 */
public class ServerAPIProtocol implements ServerListener {


    @Override
    public void connectionOpened(Connection connection, Channel channel) {

    }

    @Override
    public void connectionClosed(Connection connection) {

    }

    @Override
    public void packetRecieved(Connection connection, Channel channel, ConfigurationSection payload) {
        String mode = payload.getString("mode");

        if(mode == null) return;

        switch(mode) {
            case "PlayerJoin":
                handlePlayerJoin(connection.getMinecraftServer(), payload, false);
                return;
            case "PlayerQuit":
                handlePlayerQuit(connection.getMinecraftServer(), payload);
                return;
            case "PlayerInfo":
                handlePlayerInfo(connection.getMinecraftServer(), payload);
                return;
            case "Message":
                handleMessage(connection.getConnectedTo(), payload);
        }
    }

    private void handleMessage(Server connectedTo, ConfigurationSection payload) {
        Map<MinecraftServer, Set<Player>> messagePackets = new HashMap<>();

        for(String uuidString : payload.getStringList("recipients")) {
            Player player = connectedTo.getPlayer(UUID.fromString(uuidString));

            if(player == null || player.getServer() == null) continue;

            Set<Player> playersForServer = messagePackets.get(player.getServer());

            if(playersForServer == null) {
                playersForServer = new HashSet<>();
                messagePackets.put(player.getServer(), playersForServer);
            }

            playersForServer.add(player);
        }

        String message = payload.getString("message");

        for(Map.Entry<MinecraftServer, Set<Player>> messageEntry : messagePackets.entrySet()) {
            if(!messageEntry.getKey().isConnected()) continue;

            messageEntry.getKey().messagePlayers(message, messageEntry.getValue());
        }
    }

    private void handlePlayerInfo(MinecraftServer minecraftServer, ConfigurationSection payload) {
        for(ConfigurationSection playerInfo : ConfigUtils.getConfigList(payload, "players")) {
            handlePlayerJoin(minecraftServer, playerInfo, true);
        }
    }

    private void handlePlayerQuit(MinecraftServer minecraftServer, ConfigurationSection payload) {
        UUID playerUUID = UUID.fromString(payload.getString("uuid"));

        Player player = ((MSMMinecraftServer)minecraftServer).removePlayer(playerUUID);
        if(player == null) return;

        minecraftServer.getConnectedTo().callEvent(new PlayerQuitEvent(minecraftServer, player));
    }

    private void handlePlayerJoin(MinecraftServer minecraftServer, ConfigurationSection payload, boolean alreadyOn) {
        MSMPlayer player = new MSMPlayer(minecraftServer, payload);
        ((MSMMinecraftServer)minecraftServer).addPlayer(player);

        if(!alreadyOn) minecraftServer.getConnectedTo().callEvent(new PlayerJoinEvent(minecraftServer, player));
    }
}

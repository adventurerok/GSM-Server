package com.ithinkrok.msm.server.protocol;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.common.util.ConfigUtils;
import com.ithinkrok.msm.server.*;
import com.ithinkrok.msm.server.command.MSMCommandInfo;
import com.ithinkrok.msm.server.event.player.PlayerCommandEvent;
import com.ithinkrok.msm.server.event.player.PlayerJoinEvent;
import com.ithinkrok.msm.server.event.player.PlayerQuitEvent;
import com.ithinkrok.msm.server.impl.MSMMinecraftServer;
import com.ithinkrok.msm.server.impl.MSMPlayer;
import com.ithinkrok.util.command.CustomCommand;
import com.ithinkrok.util.event.CustomEventExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.*;

/**
 * Created by paul on 06/02/16.
 */
public class ServerAPIProtocol implements ServerListener {


    @Override
    public void connectionOpened(Connection connection, Channel channel) {
        List<ConfigurationSection> commands = new ArrayList<>();

        for(MSMCommandInfo commandInfo : connection.getConnectedTo().getRegisteredCommands()) {
            commands.add(commandInfo.toConfig());
        }

        ConfigurationSection payload = new MemoryConfiguration();

        payload.set("commands", commands);
        payload.set("mode", "RegisterCommands");

        channel.write(payload);
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
                return;
            case "HasPlayers":
                handleHasPlayers(connection.getConnectedTo(), channel, payload);
                return;
            case "PlayerCommand":
                handlePlayerCommand(connection.getMinecraftServer(), payload);
        }
    }

    private void handlePlayerCommand(MinecraftServer minecraftServer, ConfigurationSection payload) {
        String playerUUID = payload.getString("player");
        Player player = minecraftServer.getPlayer(UUID.fromString(playerUUID));
        if(player == null) return;

        String fullCommand = payload.getString("command");
        CustomCommand command = new CustomCommand(fullCommand);

        MSMCommandInfo commandInfo = minecraftServer.getConnectedTo().getCommand(command.getCommand());

        if(commandInfo == null) {
            player.sendMessage("Unknown MSM command: " + command.getCommand());
            return;
        }

        PlayerCommandEvent commandEvent = new PlayerCommandEvent(player, command);

        CustomEventExecutor.executeEvent(commandEvent, commandInfo.getCommandListener());

        if(!commandEvent.isValidCommand()) {
            player.sendMessage("Usage: " + commandInfo.getUsage());
        }
    }

    private void handleHasPlayers(Server connectedTo, Channel channel, ConfigurationSection payload) {
        ConfigurationSection players = new MemoryConfiguration();

        for(String uuidString : payload.getStringList("players")) {
            Player player = connectedTo.getPlayer(UUID.fromString(uuidString));

            if(player == null) players.set(uuidString, "NONE");
            else players.set(uuidString, player.getServer().getName());
        }

        ConfigurationSection reply = new MemoryConfiguration();

        reply.set("players", players);
        reply.set("mode", "HasPlayersResponse");
        if(payload.contains("tag")) reply.set("tag", payload.getString("tag"));

        channel.write(reply);
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

        minecraftServer.getConnectedTo().callEvent(new PlayerQuitEvent(player));
    }

    private void handlePlayerJoin(MinecraftServer minecraftServer, ConfigurationSection payload, boolean alreadyOn) {
        MSMPlayer player = new MSMPlayer(minecraftServer, payload);
        ((MSMMinecraftServer)minecraftServer).addPlayer(player);

        if(!alreadyOn) minecraftServer.getConnectedTo().callEvent(new PlayerJoinEvent(player));
    }
}

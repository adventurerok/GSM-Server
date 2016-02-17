package com.ithinkrok.msm.server.protocol;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.server.*;
import com.ithinkrok.msm.server.command.CommandInfo;
import com.ithinkrok.msm.server.event.player.PlayerChangeServerEvent;
import com.ithinkrok.msm.server.event.player.PlayerCommandEvent;
import com.ithinkrok.msm.server.event.player.PlayerJoinEvent;
import com.ithinkrok.msm.server.event.player.PlayerQuitEvent;
import com.ithinkrok.msm.server.impl.MSMMinecraftServer;
import com.ithinkrok.msm.server.impl.MSMPlayer;
import com.ithinkrok.msm.server.impl.MSMServer;
import com.ithinkrok.msm.server.permission.PermissionInfo;
import com.ithinkrok.util.command.CustomCommand;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.event.CustomEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.*;

/**
 * Created by paul on 06/02/16.
 */
public class ServerAPIProtocol implements ServerListener {

    private final Logger log = LogManager.getLogger(ServerAPIProtocol.class);

    @Override
    public void connectionOpened(Connection connection, Channel channel) {
        sendPermissionsPacket(connection.getConnectedTo(), channel);
        sendCommandsPacket(connection.getConnectedTo(), channel);
    }

    private void sendPermissionsPacket(Server server, Channel channel) {
        List<Config> permissions = new ArrayList<>();

        for(PermissionInfo permissionInfo : server.getRegisteredPermissions()){
            permissions.add(permissionInfo.toConfig());
        }

        Config payload = new MemoryConfig();

        payload.set("permissions", permissions);
        payload.set("mode", "RegisterPermissions");

        channel.write(payload);
    }

    private void sendCommandsPacket(Server server, Channel channel) {
        List<Config> commands = new ArrayList<>();

        for (CommandInfo commandInfo : server.getRegisteredCommands()) {
            commands.add(commandInfo.toConfig());
        }

        Config payload = new MemoryConfig();

        payload.set("commands", commands);
        payload.set("mode", "RegisterCommands");

        channel.write(payload);
    }

    @Override
    public void connectionClosed(Connection connection) {

    }

    @Override
    public void packetRecieved(Connection connection, Channel channel, Config payload) {
        String mode = payload.getString("mode");

        if (mode == null) return;

        switch (mode) {
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
                return;
            case "ChangeServer":
                handleChangeServer(connection.getConnectedTo(), payload);
                return;
            case "ResourceUsage":
                ((MSMMinecraftServer)connection.getMinecraftServer()).handleResourceUsagePacket(payload);
        }
    }

    private void handleChangeServer(Server connectedTo, Config payload) {
        UUID playerUUID = UUID.fromString(payload.getString("player"));
        Player player = connectedTo.getPlayer(playerUUID);

        if(player == null) {
            log.debug("Unknown player " + playerUUID + " in ChangeServer request");
            return;
        }

        String targetName = payload.getString("target");
        MinecraftServer target = connectedTo.getMinecraftServer(targetName);

        if(target == null) {
            log.debug("Unknown minecraft server " + targetName + " in ChangeServer request");
            return;
        }

        player.changeServer(target);
    }

    private void handlePlayerJoin(MinecraftServer minecraftServer, Config payload, boolean alreadyOn) {
        UUID playerUUID = UUID.fromString(payload.getString("uuid"));

        MSMPlayer player = ((MSMServer) minecraftServer.getConnectedTo()).removeQuittingPlayer(playerUUID);

        if (player != null) {
            MinecraftServer oldServer = player.getServer();

            if (oldServer != null) {
                ((MSMMinecraftServer) oldServer).removePlayer(playerUUID);
            }

            player.setServer(minecraftServer);
            ((MSMMinecraftServer) minecraftServer).addPlayer(player);

            if (!alreadyOn) minecraftServer.getConnectedTo().callEvent(new PlayerChangeServerEvent(player, oldServer));
        } else {
            player = new MSMPlayer(minecraftServer, payload);
            ((MSMMinecraftServer) minecraftServer).addPlayer(player);

            if (!alreadyOn) minecraftServer.getConnectedTo().callEvent(new PlayerJoinEvent(player));
        }
    }

    private void handlePlayerQuit(MinecraftServer minecraftServer, Config payload) {
        UUID playerUUID = UUID.fromString(payload.getString("uuid"));

        Player player = ((MSMMinecraftServer) minecraftServer).removePlayer(playerUUID);

        //The connect packet from the new server was received before the disconnect packet from this server
        if(player == null) return;

        if (!minecraftServer.getServerInfo().hasBungee()) {
            minecraftServer.getConnectedTo().callEvent(new PlayerQuitEvent(player));
            ((MSMPlayer) player).setServer(null);
        } else {
            ((MSMServer) minecraftServer.getConnectedTo()).addQuittingPlayer((MSMPlayer) player);
        }
    }

    private void handlePlayerInfo(MinecraftServer minecraftServer, Config payload) {
        for (Config playerInfo : payload.getConfigList("players")) {
            handlePlayerJoin(minecraftServer, playerInfo, true);
        }
    }

    private void handleMessage(Server connectedTo, Config payload) {
        Map<MinecraftServer, Set<Player>> messagePackets = new HashMap<>();

        for (String uuidString : payload.getStringList("recipients")) {
            Player player = connectedTo.getPlayer(UUID.fromString(uuidString));

            if (player == null || player.getServer() == null) continue;

            Set<Player> playersForServer = messagePackets.get(player.getServer());

            if (playersForServer == null) {
                playersForServer = new HashSet<>();
                messagePackets.put(player.getServer(), playersForServer);
            }

            playersForServer.add(player);
        }

        String message = payload.getString("message");

        for (Map.Entry<MinecraftServer, Set<Player>> messageEntry : messagePackets.entrySet()) {
            if (!messageEntry.getKey().isConnected()) continue;

            messageEntry.getKey().messagePlayers(message, messageEntry.getValue());
        }
    }

    private void handleHasPlayers(Server connectedTo, Channel channel, Config payload) {
        ConfigurationSection players = new MemoryConfiguration();

        for (String uuidString : payload.getStringList("players")) {
            Player player = connectedTo.getPlayer(UUID.fromString(uuidString));

            if (player == null) players.set(uuidString, "NONE");
            else players.set(uuidString, player.getServer().getName());
        }

        Config reply = new MemoryConfig();

        reply.set("players", players);
        reply.set("mode", "HasPlayersResponse");
        if (payload.contains("tag")) reply.set("tag", payload.getString("tag"));

        channel.write(reply);
    }

    private void handlePlayerCommand(MinecraftServer minecraftServer, Config payload) {
        String playerUUID = payload.getString("player");
        Player player = minecraftServer.getPlayer(UUID.fromString(playerUUID));
        if (player == null) return;

        String fullCommand = payload.getString("command");
        CustomCommand command = new CustomCommand(fullCommand);

        CommandInfo commandInfo = minecraftServer.getConnectedTo().getCommand(command.getCommand());

        if (commandInfo == null) {
            player.sendMessage("Unknown MSM command: " + command.getCommand());
            return;
        }

        PlayerCommandEvent commandEvent = new PlayerCommandEvent(player, command);

        CustomEventExecutor.executeEvent(commandEvent, commandInfo.getCommandListener());

        if (!commandEvent.isValidCommand()) {
            player.sendMessage("Usage: " + commandInfo.getUsage());
        }
    }
}

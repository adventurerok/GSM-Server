package com.ithinkrok.msm.server.protocol;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.common.util.ConfigUtils;
import com.ithinkrok.msm.server.*;
import com.ithinkrok.msm.server.data.Ban;
import com.ithinkrok.msm.server.data.MinecraftClient;
import com.ithinkrok.msm.server.data.MinecraftPlayer;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.event.MSMCommandEvent;
import com.ithinkrok.msm.server.event.MSMEvent;
import com.ithinkrok.msm.server.event.minecraftserver.MinecraftServerCommandEvent;
import com.ithinkrok.msm.server.event.minecraftserver.MinecraftServerConnectEvent;
import com.ithinkrok.msm.server.event.minecraftserver.MinecraftServerDisconnectEvent;
import com.ithinkrok.msm.server.event.player.PlayerChangeServerEvent;
import com.ithinkrok.msm.server.event.player.PlayerCommandEvent;
import com.ithinkrok.msm.server.event.player.PlayerJoinEvent;
import com.ithinkrok.msm.server.event.player.PlayerQuitEvent;
import com.ithinkrok.msm.server.impl.MSMMinecraftClient;
import com.ithinkrok.msm.server.impl.MSMMinecraftPlayer;
import com.ithinkrok.msm.server.impl.MSMServer;
import com.ithinkrok.util.command.CustomCommand;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        Config payload = new MemoryConfig();

        List<Config> permissions = ConfigUtils.collectionToConfigList(server.getRegisteredPermissions());
        payload.set("permissions", permissions);
        payload.set("mode", "RegisterPermissions");

        channel.write(payload);
    }

    private void sendCommandsPacket(Server server, Channel channel) {
        Config payload = new MemoryConfig();

        List<Config> commands = ConfigUtils.collectionToConfigList(server.getRegisteredCommands());
        payload.set("commands", commands);
        payload.set("mode", "RegisterCommands");

        channel.write(payload);
    }

    @Override
    public void connectionClosed(Connection connection) {
        MSMEvent event = new MinecraftServerDisconnectEvent(connection.getMinecraftServer());
        connection.getConnectedTo().callEvent(event);
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
            case "BanInfo":
                handleBanInfo(connection.getMinecraftServer(), payload);
                return;
            case "Message":
                handleMessage(connection.getConnectedTo(), payload);
                return;
            case "ConsoleMessage":
                handleConsoleMessage(payload);
                return;
            case "MinecraftConsoleMessage":
                handleMinecraftConsoleMessage(connection.getConnectedTo(), payload);
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
                ((MSMMinecraftClient)connection.getMinecraftServer()).handleResourceUsagePacket(payload);
                return;
            case "ConnectInfo":
                handleConnectInfo(connection.getMinecraftServer(), payload);
                return;
            case "ConsoleCommand":
                handleConsoleCommand(connection.getMinecraftServer(), payload);

        }
    }

    private void handleMinecraftConsoleMessage(Server connectedTo, Config payload) {
        String serverName = payload.getString("server");
        MinecraftClient server = connectedTo.getMinecraftServer(serverName);
        if(server == null) return;

        server.getConsoleCommandSender().sendMessage(payload.getString("message"));
    }

    private void handleConsoleMessage(Config payload) {
        String message = payload.getString("message");

        log.info("[Command] " + message);
    }

    private void handleConsoleCommand(MinecraftClient minecraftClient, Config payload) {
        String fullCommand = payload.getString("command");
        CustomCommand command = new CustomCommand(fullCommand);

        MSMCommandEvent commandEvent = new MinecraftServerCommandEvent(minecraftClient, command);

        if(!minecraftClient.getConnectedTo().executeCommand(commandEvent)) return;

        if(!commandEvent.isHandled()) {
            commandEvent.getCommandSender().sendMessage("This command does not support minecraft consoles");
        }
    }

    private void handleConnectInfo(MinecraftClient minecraftClient, Config payload) {
        handlePlayerInfo(minecraftClient, payload);
        handleBanInfo(minecraftClient, payload);

        MSMEvent event = new MinecraftServerConnectEvent(minecraftClient);
        minecraftClient.getConnectedTo().callEvent(event);
    }

    private void handleBanInfo(MinecraftClient minecraftClient, Config payload) {
        List<Config> banConfigs = payload.getConfigList("bans");

        for(Config banConfig : banConfigs) {
            ((MSMMinecraftClient) minecraftClient).addBan(new Ban(banConfig));
        }
    }

    private void handleChangeServer(Server connectedTo, Config payload) {
        UUID playerUUID = UUID.fromString(payload.getString("player"));
        MinecraftPlayer player = connectedTo.getPlayer(playerUUID);

        if(player == null) {
            log.debug("Unknown player " + playerUUID + " in ChangeServer request");
            return;
        }

        String targetName = payload.getString("target");
        MinecraftClient target = connectedTo.getMinecraftServer(targetName);

        if(target == null) {
            log.debug("Unknown minecraft server " + targetName + " in ChangeServer request");
            return;
        }

        player.changeServer(target);
    }

    private void handlePlayerJoin(MinecraftClient minecraftClient, Config payload, boolean alreadyOn) {
        UUID playerUUID = UUID.fromString(payload.getString("uuid"));

        MSMMinecraftPlayer player = ((MSMServer) minecraftClient.getConnectedTo()).removeQuittingPlayer(playerUUID);

        if (player != null) {
            MinecraftClient oldServer = player.getServer();

            if (oldServer != null) {
                ((MSMMinecraftClient) oldServer).removePlayer(playerUUID);
            }

            player.setServer(minecraftClient);
            ((MSMMinecraftClient) minecraftClient).addPlayer(player);

            if (!alreadyOn) minecraftClient.getConnectedTo().callEvent(new PlayerChangeServerEvent(player, oldServer));
        } else {
            player = new MSMMinecraftPlayer(minecraftClient, payload);
            ((MSMMinecraftClient) minecraftClient).addPlayer(player);

            if (!alreadyOn) minecraftClient.getConnectedTo().callEvent(new PlayerJoinEvent(player));
        }
    }

    private void handlePlayerQuit(MinecraftClient minecraftClient, Config payload) {
        UUID playerUUID = UUID.fromString(payload.getString("uuid"));

        MinecraftPlayer player = ((MSMMinecraftClient) minecraftClient).removePlayer(playerUUID);

        //The connect packet from the new server was received before the disconnect packet from this server
        if(player == null) return;

        if (!minecraftClient.getServerInfo().hasBungee()) {
            minecraftClient.getConnectedTo().callEvent(new PlayerQuitEvent(player));
            ((MSMMinecraftPlayer) player).setServer(null);
        } else {
            ((MSMServer) minecraftClient.getConnectedTo()).addQuittingPlayer((MSMMinecraftPlayer) player);
        }
    }

    private void handlePlayerInfo(MinecraftClient minecraftClient, Config payload) {
        for (Config playerInfo : payload.getConfigList("players")) {
            handlePlayerJoin(minecraftClient, playerInfo, true);
        }
    }

    private void handleMessage(Server connectedTo, Config payload) {
        Map<MinecraftClient, Set<MinecraftPlayer>> messagePackets = new HashMap<>();

        for (String uuidString : payload.getStringList("recipients")) {
            MinecraftPlayer player = connectedTo.getPlayer(UUID.fromString(uuidString));

            if (player == null || player.getServer() == null) continue;

            Set<MinecraftPlayer> playersForServer = messagePackets.get(player.getServer());

            if (playersForServer == null) {
                playersForServer = new HashSet<>();
                messagePackets.put(player.getServer(), playersForServer);
            }

            playersForServer.add(player);
        }

        String message = payload.getString("message");

        for (Map.Entry<MinecraftClient, Set<MinecraftPlayer>> messageEntry : messagePackets.entrySet()) {
            if (!messageEntry.getKey().isConnected()) continue;

            messageEntry.getKey().messagePlayers(message, messageEntry.getValue());
        }
    }

    private void handleHasPlayers(Server connectedTo, Channel channel, Config payload) {
        Config players = new MemoryConfig();

        for (String uuidString : payload.getStringList("players")) {
            MinecraftPlayer player = connectedTo.getPlayer(UUID.fromString(uuidString));

            if (player == null) players.set(uuidString, "NONE");
            else players.set(uuidString, player.getServer().getName());
        }

        Config reply = new MemoryConfig();

        reply.set("players", players);
        reply.set("mode", "HasPlayersResponse");
        if (payload.contains("tag")) reply.set("tag", payload.getString("tag"));

        channel.write(reply);
    }

    private void handlePlayerCommand(MinecraftClient minecraftClient, Config payload) {
        String playerUUID = payload.getString("player");
        MinecraftPlayer player = minecraftClient.getPlayer(UUID.fromString(playerUUID));
        if (player == null) return;

        String fullCommand = payload.getString("command");
        CustomCommand command = new CustomCommand(fullCommand);

        PlayerCommandEvent commandEvent = new PlayerCommandEvent(player, command);

        if(!minecraftClient.getConnectedTo().executeCommand(commandEvent)) return;

        if(!commandEvent.isHandled()) {
            player.sendMessage("This command does not support players");
        }
    }
}

package com.ithinkrok.msm.server.protocol;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.util.config.ConfigUtils;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.ServerListener;
import com.ithinkrok.msm.server.command.CommandHandler;
import com.ithinkrok.msm.server.data.Ban;
import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.data.Player;
import com.ithinkrok.msm.server.data.PlayerIdentifier;
import com.ithinkrok.msm.server.event.MSMEvent;
import com.ithinkrok.msm.server.event.command.MSMCommandEvent;
import com.ithinkrok.msm.server.event.minecraftserver.ClientCommandEvent;
import com.ithinkrok.msm.server.event.minecraftserver.ClientConnectEvent;
import com.ithinkrok.msm.server.event.minecraftserver.ClientDisconnectEvent;
import com.ithinkrok.msm.server.event.player.PlayerChangeServerEvent;
import com.ithinkrok.msm.server.event.player.PlayerCommandEvent;
import com.ithinkrok.msm.server.event.player.PlayerJoinEvent;
import com.ithinkrok.msm.server.impl.MSMServer;
import com.ithinkrok.msm.server.minecraft.impl.MSMMinecraftClient;
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

    public static final String TABSET_GSM_PLAYERS = "gsmPlayers";
    public static final String TABSET_GSM_GAMES = "gsmGames";
    public static final String TABSET_GSM_SERVERS = "gsmServers";

    private final Logger log = LogManager.getLogger(ServerAPIProtocol.class);

    @Override
    public void connectionOpened(Connection connection, Channel channel) {
        sendPermissionsPacket(connection.getConnectedTo(), channel);
        sendCommandsPacket(connection.getConnectedTo(), channel);
        sendTabCompletionPacket(connection.getConnectedTo(), channel);

        CommandHandler commandHandler = connection.getConnectedTo().getCommandHandler();
        commandHandler.addTabCompletionItems(TABSET_GSM_GAMES, connection.getClient().getType());

        commandHandler.addTabCompletionItems(TABSET_GSM_SERVERS, connection.getClient().getName());
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

        CommandHandler commandHandler = server.getCommandHandler();
        List<Config> commands = ConfigUtils.collectionToConfigList(commandHandler.getRegisteredCommands());
        payload.set("commands", commands);
        payload.set("mode", "RegisterCommands");

        channel.write(payload);
    }

    private void sendTabCompletionPacket(Server connectedTo, Channel channel) {
        Config payload = new MemoryConfig();

        payload.set("mode", "TabSets");

        Config tabSets = new MemoryConfig();

        CommandHandler commandHandler = connectedTo.getCommandHandler();

        for (String setName : commandHandler.getTabCompletionSetNames()) {
            Set<String> tabSet = commandHandler.getTabCompletionItems(setName);

            tabSets.set(setName, tabSet);
        }

        payload.set("tab_sets", tabSets);

        channel.write(payload);
    }

    @Override
    public void connectionClosed(Connection connection) {
        MSMEvent event = new ClientDisconnectEvent(connection.getClient());
        connection.getConnectedTo().callEvent(event);

        Collection<String> playerNames = new ArrayList<>();

        for(Player<?> player : connection.getClient().getPlayers()) {
            playerNames.add(player.getName());
        }

        CommandHandler commandHandler = connection.getConnectedTo().getCommandHandler();
        commandHandler.removeTabCompletionItems(TABSET_GSM_PLAYERS, playerNames);

        commandHandler.removeTabCompletionItems(TABSET_GSM_SERVERS, connection.getClient().getName());
    }

    @Override
    public void packetRecieved(Connection connection, Channel channel, Config payload) {
        String mode = payload.getString("mode");

        if (mode == null) return;

        switch (mode) {
            case "PlayerJoin":
                handlePlayerJoin(connection.getClient(), payload, false, true);
                return;
            case "PlayerQuit":
                handlePlayerQuit(connection.getClient(), payload);
                return;
            case "PlayerInfo":
                handlePlayerInfo(connection.getClient(), payload);
                return;
            case "BanInfo":
                handleBanInfo(connection.getClient(), payload);
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
                handlePlayerCommand(connection.getClient(), payload);
                return;
            case "ResourceUsage":
                ((MSMMinecraftClient) connection.getClient()).handleResourceUsagePacket(payload);
                return;
            case "ConnectInfo":
                handleConnectInfo(connection.getClient(), payload);
                return;
            case "ConsoleCommand":
                handleConsoleCommand(connection.getClient(), payload);
                return;
            case "RestartScheduled":
                handleRestartScheduled(connection.getClient());

        }
    }

    private void handleRestartScheduled(Client<?> client) {
        client.setRestartScheduled();
    }

    @SuppressWarnings("unchecked")
    private <T extends Player<?>> T handlePlayerJoin(Client<T> minecraftClient, Config payload, boolean alreadyOn,
                                                        boolean addTabCompletion) {
        UUID playerUUID = UUID.fromString(payload.getString("uuid"));
        PlayerIdentifier identifier = new PlayerIdentifier(minecraftClient.getType(), playerUUID);

        Player<Client<?>> player =
                (Player<Client<?>>) ((MSMServer) minecraftClient.getConnectedTo()).removeQuittingPlayer(identifier);

        if (player != null) {
            Client<?> oldServer = player.getClient();

            if (oldServer != null) {
                oldServer.removePlayer(playerUUID);
            }

            player.setClient(minecraftClient);
            (minecraftClient).addPlayer((T) player);

            if (!alreadyOn) minecraftClient.getConnectedTo().callEvent(new PlayerChangeServerEvent(player, oldServer));
        } else {
            player = (Player<Client<?>>) minecraftClient.createPlayer(payload);
            minecraftClient.addPlayer((T) player);

            if (!alreadyOn) minecraftClient.getConnectedTo().callEvent(new PlayerJoinEvent(player));
        }

        if(!addTabCompletion) return (T) player;

        CommandHandler commandHandler = minecraftClient.getConnectedTo().getCommandHandler();
        commandHandler.addTabCompletionItems(TABSET_GSM_PLAYERS, player.getName());

        return (T) player;
    }

    private void handlePlayerQuit(Client<?> minecraftClient, Config payload) {
        UUID playerUUID = UUID.fromString(payload.getString("uuid"));

        Player<?> player = minecraftClient.removePlayer(playerUUID);

        //The connect packet from the new server was received before the disconnect packet from this server
        if (player == null) return;

        ((MSMServer) minecraftClient.getConnectedTo()).addQuittingPlayer(player);
    }

    private void handlePlayerInfo(Client<?> minecraftClient, Config payload) {
        Collection<String> playerNames = new ArrayList<>();

        for (Config playerInfo : payload.getConfigList("players")) {
            Player<?> player = handlePlayerJoin(minecraftClient, playerInfo, true, false);

            playerNames.add(player.getName());
        }

        CommandHandler commandHandler = minecraftClient.getConnectedTo().getCommandHandler();
        commandHandler.addTabCompletionItems(TABSET_GSM_PLAYERS, playerNames);
    }

    private void handleBanInfo(Client<?> minecraftClient, Config payload) {
        List<Config> banConfigs = payload.getConfigList("bans");

        for (Config banConfig : banConfigs) {
            ((MSMMinecraftClient) minecraftClient).addBan(new Ban(banConfig));
        }
    }

    private void handleMessage(Server connectedTo, Config payload) {
        Map<Client<?>, Set<Player<?>>> messagePackets = new HashMap<>();

        for (String uuidString : payload.getStringList("recipients")) {
            Player<?> player = connectedTo.getPlayer("minecraft", UUID.fromString(uuidString));

            if (player == null || player.getClient() == null) continue;

            Set<Player<?>> playersForServer = messagePackets.get(player.getClient());

            if (playersForServer == null) {
                playersForServer = new HashSet<>();
                messagePackets.put(player.getClient(), playersForServer);
            }

            playersForServer.add(player);
        }

        String message = payload.getString("message");

        for (Map.Entry<Client<?>, Set<Player<?>>> messageEntry : messagePackets.entrySet()) {
            if (!messageEntry.getKey().isConnected()) continue;

            messageEntry.getKey().messagePlayers(message, messageEntry.getValue());
        }
    }

    private void handleConsoleMessage(Config payload) {
        String message = payload.getString("message");

        log.info("[Command] " + message);
    }

    private void handleMinecraftConsoleMessage(Server connectedTo, Config payload) {
        String serverName = payload.getString("server");
        Client<?> server = connectedTo.getClient(serverName);
        if (server == null) return;

        server.getConsoleCommandSender().sendMessage(payload.getString("message"));
    }

    private void handleHasPlayers(Server connectedTo, Channel channel, Config payload) {
        Config players = new MemoryConfig();

        for (String uuidString : payload.getStringList("players")) {
            Player<?> player = connectedTo.getPlayer("minecraft", UUID.fromString(uuidString));

            if (player == null) players.set(uuidString, "NONE");
            else players.set(uuidString, player.getClient().getName());
        }

        Config reply = new MemoryConfig();

        reply.set("players", players);
        reply.set("mode", "HasPlayersResponse");
        if (payload.contains("tag")) reply.set("tag", payload.getString("tag"));

        channel.write(reply);
    }

    private void handlePlayerCommand(Client<?> minecraftClient, Config payload) {
        String playerUUID = payload.getString("player");
        Player<?> player = minecraftClient.getPlayer(UUID.fromString(playerUUID));
        if (player == null) return;

        String fullCommand = payload.getString("command");
        CustomCommand command = new CustomCommand(fullCommand);

        PlayerCommandEvent commandEvent = new PlayerCommandEvent(player, command);

        CommandHandler commandHandler = minecraftClient.getConnectedTo().getCommandHandler();
        if (!commandHandler.executeCommand(commandEvent)) return;

        if (!commandEvent.isHandled()) {
            player.sendMessage("This command does not support players");
        }
    }

    private void handleConnectInfo(Client<?> minecraftClient, Config payload) {
        handlePlayerInfo(minecraftClient, payload);
        handleBanInfo(minecraftClient, payload);

        MSMEvent event = new ClientConnectEvent(minecraftClient);
        minecraftClient.getConnectedTo().callEvent(event);
    }

    private void handleConsoleCommand(Client<?> minecraftClient, Config payload) {
        String fullCommand = payload.getString("command");
        CustomCommand command = new CustomCommand(fullCommand);

        MSMCommandEvent commandEvent = new ClientCommandEvent(minecraftClient, command);

        CommandHandler commandHandler = minecraftClient.getConnectedTo().getCommandHandler();
        if (!commandHandler.executeCommand(commandEvent)) return;

        if (!commandEvent.isHandled()) {
            commandEvent.getCommandSender().sendMessage("This command does not support minecraft consoles");
        }
    }
}

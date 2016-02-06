package com.ithinkrok.msm.server.protocol;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.common.MinecraftServerInfo;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.ServerListener;
import com.ithinkrok.msm.server.impl.MSMConnection;
import com.ithinkrok.msm.server.impl.MSMMinecraftServer;
import com.ithinkrok.msm.server.impl.MSMServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.*;

/**
 * Created by paul on 04/02/16.
 */
public class ServerLoginProtocol implements ServerListener {

    private static final Logger log = LogManager.getLogger(ServerLoginProtocol.class);

    @Override
    public void connectionOpened(Connection connection, Channel channel) {

    }

    @Override
    public void connectionClosed(Connection connection) {
        ((MSMMinecraftServer)connection.getServer()).setConnection(null);
    }

    @Override
    public void packetRecieved(Connection connection, Channel channel, ConfigurationSection payload) {
        int version = payload.getInt("version", -1);
        if (version != 0) throw new RuntimeException("Unsupported version: " + version);

        ConfigurationSection serverInfoConfig = payload.getConfigurationSection("server_info");
        MinecraftServerInfo minecraftServerInfo = new MinecraftServerInfo(serverInfoConfig);

        ((MSMServer) connection.getServer())
                .assignMinecraftServerToConnection(serverInfoConfig, (MSMConnection) connection);

        List<String> clientProtocols = payload.getStringList("protocols");
        Set<String> serverProtocols = connection.getServer().getAvailableProtocols();

        //Get the protocols supported by both the server and the client
        Set<String> sharedProtocols = new LinkedHashSet<>();
        for (String protocol : clientProtocols) {
            if (serverProtocols.contains(protocol)) sharedProtocols.add(protocol);
        }

        log.info("New client " + minecraftServerInfo + " connected with protocols: " + sharedProtocols);

        ((MSMConnection) connection).setSupportedProtocols(new ArrayList<>(sharedProtocols));

        ConfigurationSection replyPayload = new MemoryConfiguration();
        replyPayload.set("protocols", new ArrayList<>(sharedProtocols));

        channel.write(replyPayload);

        for (String protocol : sharedProtocols) {
            ServerListener listener = ((MSMConnection) connection).getServer().getListenerForProtocol(protocol);

            Channel otherChannel = connection.getChannel(protocol);

            listener.connectionOpened(connection, otherChannel);
        }
    }
}

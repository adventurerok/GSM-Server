package com.ithinkrok.msm.server.protocol;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.common.MinecraftServerInfo;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.ServerListener;
import com.ithinkrok.msm.server.impl.MSMConnection;
import com.ithinkrok.msm.server.impl.MSMMinecraftServer;
import com.ithinkrok.msm.server.impl.MSMServer;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    }

    @Override
    public void packetRecieved(Connection connection, Channel channel, Config payload) {
        //Check we support the clients protocol version
        int version = payload.getInt("version", -1);
        if (version != 0) throw new RuntimeException("Unsupported version: " + version);

        //Get the clients server info
        Config serverInfoConfig = payload.getConfigOrNull("server_info");
        MinecraftServerInfo minecraftServerInfo = new MinecraftServerInfo(serverInfoConfig);

        //Assign a MinecraftServer object to the connection
        ((MSMServer) connection.getConnectedTo())
                .assignMinecraftServerToConnection(serverInfoConfig, (MSMConnection) connection);

        //Get the client protocol list and the server protocol list
        List<String> clientProtocols = payload.getStringList("protocols");
        Set<String> serverProtocols = connection.getConnectedTo().getAvailableProtocols();

        //Get the protocols supported by both the server and the client
        Set<String> sharedProtocols = new LinkedHashSet<>();
        for (String protocol : clientProtocols) {
            if (serverProtocols.contains(protocol)) sharedProtocols.add(protocol);
        }

        log.info("New client " + minecraftServerInfo + " connected with protocols: " + sharedProtocols);

        ((MSMConnection) connection).setSupportedProtocols(new ArrayList<>(sharedProtocols));

        //Reply with our supported protocols
        Config replyPayload = new MemoryConfig();
        replyPayload.set("protocols", new ArrayList<>(sharedProtocols));

        channel.write(replyPayload);

        //call connectionOpened events for supported protocols
        for (String protocol : sharedProtocols) {
            ServerListener listener = ((MSMConnection) connection).getConnectedTo().getListenerForProtocol(protocol);

            Channel otherChannel = connection.getChannel(protocol);

            listener.connectionOpened(connection, otherChannel);
        }
    }
}

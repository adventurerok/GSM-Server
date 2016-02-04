package com.ithinkrok.msm.server.protocol;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.ServerListener;
import com.ithinkrok.msm.server.impl.MSMConnection;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by paul on 04/02/16.
 */
public class ServerLoginProtocol implements ServerListener {

    @Override
    public void connectionOpened(Connection connection, Channel channel) {

    }

    @Override
    public void connectionClosed(Connection connection, Channel channel) {

    }

    @Override
    public void packetRecieved(Connection connection, Channel channel, ConfigurationSection payload) {
        int version = payload.getInt("version", -1);
        if(version != 0) throw new RuntimeException("Unsupported version: " + version);

        List<String> clientProtocols = payload.getStringList("protocols");
        Set<String> serverProtocols = connection.getServer().getSupportedProtocols();

        //Get the protocols supported by both the server and the client
        Set<String> sharedProtocols = new HashSet<>();
        for(String protocol : clientProtocols) {
            if(serverProtocols.contains(protocol)) sharedProtocols.add(protocol);
        }

        ConfigurationSection replyPayload = new MemoryConfiguration();
        replyPayload.set("protocols", new ArrayList<>(sharedProtocols));

        channel.write(replyPayload);

        for(String protocol : sharedProtocols) {
            ServerListener listener = ((MSMConnection)connection).getServer().getListenerForProtocol(protocol);

            Channel otherChannel = connection.getChannel(protocol);

            listener.connectionOpened(connection, otherChannel);
        }
    }
}

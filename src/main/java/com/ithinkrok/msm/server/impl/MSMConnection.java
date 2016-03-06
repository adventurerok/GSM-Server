package com.ithinkrok.msm.server.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.common.Packet;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.minecraft.MinecraftClient;
import com.ithinkrok.msm.server.minecraft.impl.MSMMinecraftClient;
import com.ithinkrok.util.config.Config;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by paul on 02/02/16.
 */
public class MSMConnection extends ChannelInboundHandlerAdapter implements Connection, ChannelFutureListener {

    private static final Logger log = LogManager.getLogger(MSMServer.class);

    private final MSMServer msmServer;
    private final BiMap<Integer, String> idToProtocolMap = HashBiMap.create();
    private final Map<Integer, MSMConnectionChannel> channelMap = new HashMap<>();

    private Client<?> minecraftServer;

    private io.netty.channel.Channel channel;

    public MSMConnection(MSMServer msmServer) {
        this.msmServer = msmServer;

        //Add to the protocol map to make logins work
        idToProtocolMap.put(0, "MSMLogin");
    }

    @Override
    public Channel getChannel(String protocol) {
        return getChannel(idToProtocolMap.inverse().get(protocol));
    }

    @Override
    public MSMServer getConnectedTo() {
        return msmServer;
    }

    @Override
    public Client<?> getClient() {
        return minecraftServer;
    }

    @Override
    public void setClient(Client<?> minecraftServer) {
        if (minecraftServer.isConnected() && minecraftServer.getConnection() != this) {
            throw new RuntimeException("Minecraft server " + minecraftServer + " is already connected");
        }

        this.minecraftServer = minecraftServer;

        if(minecraftServer.getConnection() == this) return;
        minecraftServer.setConnection(this);
    }

    @Override
    public Collection<String> getSupportedProtocols() {
        return idToProtocolMap.values();
    }

    @Override
    public void close() {
        channel.close();
    }

    public void setSupportedProtocols(List<String> supportedProtocols) {
        idToProtocolMap.clear();

        int counter = 0;

        for (String protocol : supportedProtocols) {
            idToProtocolMap.put(counter++, protocol);
        }
    }

    private MSMConnectionChannel getChannel(int id) {
        MSMConnectionChannel channel = channelMap.get(id);

        if (channel == null) {
            channel = new MSMConnectionChannel(id);
            channelMap.put(id, channel);
        }

        return channel;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();

        channel.closeFuture().addListener(this);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //Pass on Objects that are not Packets to the next handler
        if (!Packet.class.isInstance(msg)) {
            super.channelRead(ctx, msg);
            return;
        }

        Packet packet = (Packet) msg;
        String protocol = idToProtocolMap.get(packet.getId());

        log.trace("Received packet for protocol " + protocol);

        MSMConnectionChannel channel = getChannel(packet.getId());

        //Send the packet to the listener for the specified protocol
        msmServer.getListenerForProtocol(protocol).packetRecieved(MSMConnection.this, channel, packet.getPayload());
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (minecraftServer == null) {
            //This server is being disconnected due to invalid password.
            // We already log a message to console about this.
            return;
        }


        log.info("MinecraftClient " + minecraftServer.getName() + " disconnected");

        minecraftServer.setConnection(null);

        for (Map.Entry<Integer, String> entry : idToProtocolMap.entrySet()) {
            msmServer.getListenerForProtocol(entry.getValue()).connectionClosed(this);
        }
    }

    private class MSMConnectionChannel implements Channel {

        private final int id;

        public MSMConnectionChannel(int id) {
            this.id = id;
        }

        @Override
        public void write(Config packet) {
            log.trace("Sent packet for protocol " + idToProtocolMap.get(id));
            channel.writeAndFlush(new Packet(id, packet));
        }
    }
}

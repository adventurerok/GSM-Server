package com.ithinkrok.msm.server.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.common.Packet;
import com.ithinkrok.msm.server.Connection;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by paul on 02/02/16.
 */
public class MSMConnection extends ChannelInboundHandlerAdapter implements Connection {

    private static final Logger log = LogManager.getLogger(MSMServer.class);

    private final MSMServer msmServer;
    private final BiMap<Byte, String> idToProtocolMap = HashBiMap.create();
    private final Map<Byte, MSMConnectionChannel> channelMap = new HashMap<>();
    private io.netty.channel.Channel channel;

    public MSMConnection(MSMServer msmServer) {
        this.msmServer = msmServer;

        //Add to the protocol map to make logins work
        idToProtocolMap.put((byte) 0, "MSMLogin");
    }

    @Override
    public Channel getChannel(String protocol) {
        return channelMap.get(idToProtocolMap.inverse().get(protocol));
    }

    @Override
    public MSMServer getServer() {
        return msmServer;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();
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

        log.info("Received packet for protocol " + protocol);

        MSMConnectionChannel channel = getChannel(packet.getId());

        //Send the packet to the listener for the specified protocol
        msmServer.getListenerForProtocol(protocol).packetRecieved(MSMConnection.this, channel, packet.getPayload());
    }

    private MSMConnectionChannel getChannel(byte id) {
        MSMConnectionChannel channel = channelMap.get(id);

        if (channel == null) {
            channel = new MSMConnectionChannel(id);
            channelMap.put(id, channel);
        }

        return channel;
    }

    private class MSMConnectionChannel implements Channel {

        private final byte id;

        public MSMConnectionChannel(byte id) {
            this.id = id;
        }

        @Override
        public void write(ConfigurationSection packet) {
            channel.writeAndFlush(new Packet(id, packet));
        }
    }
}

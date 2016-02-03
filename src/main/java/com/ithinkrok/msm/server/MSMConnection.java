package com.ithinkrok.msm.server;

import com.ithinkrok.msm.common.MSMChannel;
import com.ithinkrok.msm.common.Packet;
import io.netty.channel.Channel;
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
public class MSMConnection {

    private static final Logger log = LogManager.getLogger(MSMServer.class);

    private final MSMServer msmServer;
    private final HandlerAdapter handlerAdapter;
    private Channel channel;

    private final Map<Byte, String> idToProtocolMap = new HashMap<>();

    public MSMConnection(MSMServer msmServer) {
        this.msmServer = msmServer;
        handlerAdapter = new HandlerAdapter();
    }

    public MSMServer getServer() {
        return msmServer;
    }

    public HandlerAdapter getHandlerAdapter() {
        return handlerAdapter;
    }

    private class HandlerAdapter extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
            channel = ctx.channel();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            //Pass on Objects that are not Packets to the next handler
            if(!Packet.class.isInstance(msg)){
                super.channelRead(ctx, msg);
                return;
            }

            Packet packet = (Packet) msg;
            String protocol = idToProtocolMap.get(packet.getId());

            log.info("Received packet for protocol " + protocol);

            //Send the packet to the listener for the specified protocol
            msmServer.getListenerForProtocol(protocol).packetRecieved(MSMConnection.this, packet);
        }
    }

    private class MSMConnectionChannel implements MSMChannel {

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

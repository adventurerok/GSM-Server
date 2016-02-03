package com.ithinkrok.msm.server;

import com.ithinkrok.msm.common.Packet;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by paul on 02/02/16.
 */
public class MSMConnection extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(MSMServer.class);

    private final MSMServer msmServer;
    private final HandlerAdapter handlerAdapter;
    private Channel channel;

    private Map<Byte, String> idToProtocolMap = new HashMap<>();

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

    public void sendPacket(Packet packet) {
        channel.writeAndFlush(packet);
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
}

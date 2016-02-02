package com.ithinkrok.msm.server;

import com.ithinkrok.msm.common.Packet;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by paul on 02/02/16.
 */
public class MSMConnection extends ChannelInboundHandlerAdapter {

    private static final Logger log = LogManager.getLogger(MSMServer.class);

    private final HandlerAdapter handlerAdapter;
    private Channel channel;

    public MSMConnection() {
        handlerAdapter = new HandlerAdapter();
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
            System.out.println(msg);
        }
    }
}

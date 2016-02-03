package com.ithinkrok.msm.server;

import com.ithinkrok.msm.common.handler.MSMFrameDecoder;
import com.ithinkrok.msm.common.handler.MSMFrameEncoder;
import com.ithinkrok.msm.common.handler.MSMPacketDecoder;
import com.ithinkrok.msm.common.handler.MSMPacketEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by paul on 01/02/16.
 *
 * The server class for MSM
 */
public class MSMServer {

    private static final Logger log = LogManager.getLogger(MSMServer.class);

    private final int port;

    private final Map<String, MSMServerListener> protocolToPluginMap = new HashMap<>();

    public MSMServer(int port, Map<String, ? extends MSMServerListener> listeners) {
        this.port = port;

        protocolToPluginMap.putAll(listeners);
    }

    public MSMServerListener getListenerForProtocol(String protocol) {
        return protocolToPluginMap.get(protocol);
    }

    public Set<String> getSupportedProtocols(){
        return new HashSet<>(protocolToPluginMap.keySet());
    }

    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup);
        b.channel(NioServerSocketChannel.class);
        b.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                //TODO move this method to better location
                setupPipeline(ch.pipeline());
            }
        });
        b.childOption(ChannelOption.SO_KEEPALIVE, true);

        try {
            //TODO move MSMClient and MSMServer to Base and Controller
            ChannelFuture future = b.bind(port).sync();
            log.info("Server started on port " + port);

            future.channel().closeFuture().addListener(unused1 -> {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setupPipeline(ChannelPipeline pipeline) {
        //inbound
        pipeline.addLast("MSMFrameDecoder", new MSMFrameDecoder());
        pipeline.addLast("MSMPacketDecoder", new MSMPacketDecoder());

        //outbound
        pipeline.addLast("MSMFrameEncoder", new MSMFrameEncoder());
        pipeline.addLast("MSMPacketEncoder", new MSMPacketEncoder());

        pipeline.addLast("MSMConnection", new MSMConnection(this));
    }

}

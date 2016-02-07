package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.common.MinecraftServerInfo;
import com.ithinkrok.msm.common.handler.MSMFrameDecoder;
import com.ithinkrok.msm.common.handler.MSMFrameEncoder;
import com.ithinkrok.msm.common.handler.MSMPacketDecoder;
import com.ithinkrok.msm.common.handler.MSMPacketEncoder;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.ServerListener;
import com.ithinkrok.msm.server.event.MSMEvent;
import com.ithinkrok.msm.server.protocol.ServerLoginProtocol;
import com.ithinkrok.util.event.CustomEventExecutor;
import com.ithinkrok.util.event.CustomListener;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by paul on 01/02/16.
 * <p>
 * The server class for MSM
 */
public class MSMServer implements Server {

    private static final Logger log = LogManager.getLogger(MSMServer.class);

    private final int port;

    private final Map<String, ServerListener> protocolToPluginMap = new HashMap<>();

    private final Map<String, MSMMinecraftServer> minecraftServerMap = new ConcurrentHashMap<>();

    private final ScheduledThreadPoolExecutor mainThreadExecutor, asyncThreadExecutor;

    private final Map<CustomListener, HashSet<String>> listeners = new ConcurrentHashMap<>();

    public MSMServer(int port, Map<String, ? extends ServerListener> listeners) {
        this.port = port;

        //Add the ServerLoginProtocol
        protocolToPluginMap.put("MSMLogin", new ServerLoginProtocol());

        protocolToPluginMap.putAll(listeners);

        mainThreadExecutor = new ScheduledThreadPoolExecutor(1);
        asyncThreadExecutor = new ScheduledThreadPoolExecutor(4);
    }

    public ServerListener getListenerForProtocol(String protocol) {
        return protocolToPluginMap.get(protocol);
    }

    @Override
    public Set<String> getAvailableProtocols() {
        return new HashSet<>(protocolToPluginMap.keySet());
    }

    @Override
    public MSMMinecraftServer getMinecraftServer(String name) {
        return minecraftServerMap.get(name);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return mainThreadExecutor.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return mainThreadExecutor.schedule(command, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleRepeat(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return mainThreadExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public <V> ScheduledFuture<V> scheduleAsync(Callable<V> callable, long delay, TimeUnit unit) {
        return asyncThreadExecutor.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAsync(Runnable command, long delay, TimeUnit unit) {
        return asyncThreadExecutor.schedule(command, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleRepeatAsync(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return asyncThreadExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public void callEvent(MSMEvent event) {
        List<CustomListener> listenersToCall = new ArrayList<>();

        for(Map.Entry<CustomListener, HashSet<String>> listenerEntry : listeners.entrySet()) {
            if(!event.getMinecraftServer().getSupportedProtocols().containsAll(listenerEntry.getValue())) continue;

            listenersToCall.add(listenerEntry.getKey());
        }

        CustomEventExecutor.executeEvent(event, listenersToCall);
    }

    @Override
    public void registerListener(CustomListener listener, String... requireProtocols) {
        listeners.put(listener, new HashSet<>(Arrays.asList(requireProtocols)));
    }

    @Override
    public void unregisterListener(CustomListener listener) {
        listeners.remove(listener);
    }

    public MSMMinecraftServer assignMinecraftServerToConnection(ConfigurationSection config, MSMConnection connection) {
        MSMMinecraftServer server = getMinecraftServer(config.getString("name"));

        if(server == null) {
            server = new MSMMinecraftServer(new MinecraftServerInfo(config), this);
            minecraftServerMap.put(config.getString("name"), server);
        } else server.getServerInfo().fromConfig(config);

        connection.setMinecraftServer(server);

        return server;
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

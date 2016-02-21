package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.common.MinecraftServerInfo;
import com.ithinkrok.msm.common.handler.MSMFrameDecoder;
import com.ithinkrok.msm.common.handler.MSMFrameEncoder;
import com.ithinkrok.msm.common.handler.MSMPacketDecoder;
import com.ithinkrok.msm.common.handler.MSMPacketEncoder;
import com.ithinkrok.msm.common.util.io.DirectoryWatcher;
import com.ithinkrok.msm.server.auth.PasswordManager;
import com.ithinkrok.msm.server.command.RegisterServerCommand;
import com.ithinkrok.msm.server.console.ConsoleHandler;
import com.ithinkrok.msm.server.data.MinecraftServer;
import com.ithinkrok.msm.server.data.Player;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.ServerListener;
import com.ithinkrok.msm.server.command.CommandInfo;
import com.ithinkrok.msm.server.event.MSMCommandEvent;
import com.ithinkrok.msm.server.event.MSMEvent;
import com.ithinkrok.msm.server.event.player.PlayerQuitEvent;
import com.ithinkrok.msm.server.permission.PermissionInfo;
import com.ithinkrok.msm.server.protocol.ServerLoginProtocol;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.event.CustomEventExecutor;
import com.ithinkrok.util.event.CustomEventHandler;
import com.ithinkrok.util.event.CustomListener;
import com.ithinkrok.util.lang.LanguageLookup;
import com.ithinkrok.util.lang.MultipleLanguageLookup;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    private final Map<String, CommandInfo> commandMap = new ConcurrentHashMap<>();

    private final Map<String, CommandInfo> commandAliasMap = new ConcurrentHashMap<>();

    private final Map<String, PermissionInfo> permissionMap = new ConcurrentHashMap<>();

    private final Map<UUID, MSMPlayer> quittingPlayers = new ConcurrentHashMap<>();

    private final MultipleLanguageLookup languageLookup = new MultipleLanguageLookup();

    private final DirectoryWatcher directoryWatcher;

    private final String restartScript;
    private Channel channel;

    private boolean stopped = false;
    private ConsoleHandler consoleHandler;

    public MSMServer(Config config, Map<String, ? extends ServerListener> listeners) {
        this.port = config.getInt("port", 30824);

        this.restartScript = config.getString("restart_script");

        //Add the ServerLoginProtocol and the registerserver command
        PasswordManager passwordManager = new PasswordManager(Paths.get("passwords.dat"));
        protocolToPluginMap.put("MSMLogin", new ServerLoginProtocol(passwordManager));
        registerCommand(RegisterServerCommand.createCommandInfo(passwordManager));

        protocolToPluginMap.putAll(listeners);

        mainThreadExecutor = new ScheduledThreadPoolExecutor(1);
        asyncThreadExecutor = new ScheduledThreadPoolExecutor(4);

        try {
            directoryWatcher = new DirectoryWatcher(FileSystems.getDefault());
        } catch (IOException e) {
            log.fatal("Failed to create directory watcher", e);
            throw new RuntimeException("Failed to create directory watcher", e);
        }
    }

    public void registerProtocol(String name, ServerListener listener) {
        protocolToPluginMap.put(name, listener);
    }

    public void registerProtocols(Map<String, ServerListener> protocols) {
        protocolToPluginMap.putAll(protocols);
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
    public Collection<MinecraftServer> getMinecraftServers() {
        return new ArrayList<>(minecraftServerMap.values());
    }

    @Override
    public MSMPlayer getPlayer(UUID uuid) {
        for (MSMMinecraftServer minecraftServer : minecraftServerMap.values()) {
            MSMPlayer player = minecraftServer.getPlayer(uuid);

            if (player != null) return player;
        }

        return null;
    }

    @Override
    public Player getPlayer(String name) {
        for (MSMMinecraftServer minecraftServer : minecraftServerMap.values()) {
            MSMPlayer player = minecraftServer.getPlayer(name);

            if (player != null) return player;
        }

        return null;
    }

    @CustomEventHandler
    public boolean executeCommand(MSMCommandEvent event) {
        event.setHandled(false);

        CommandInfo commandInfo = getCommand(event.getCommand().getCommand());
        if(commandInfo == null) {
            event.getCommandSender().sendMessage("Unknown MSM command: " + event.getCommand().getCommand());
            return false;
        }

        CustomEventExecutor.executeEvent(event, commandInfo.getCommandListener());

        if(!event.isValidCommand()) {
            event.getCommandSender().sendMessage("Usage: " + commandInfo.getUsage());
            return false;
        }

        return true;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return scheduleOnService(callable, delay, unit, mainThreadExecutor);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return mainThreadExecutor.schedule(() -> {
            try {
                command.run();
            } catch (Exception e) {
                log.warn("Error in scheduled task", e);
            }
        }, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleRepeat(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return mainThreadExecutor.scheduleAtFixedRate(() -> {
            try {
                command.run();
            } catch (Exception e) {
                log.warn("Error in scheduled task", e);
            }
        }, initialDelay, period, unit);
    }

    @Override
    public <V> ScheduledFuture<V> scheduleAsync(Callable<V> callable, long delay, TimeUnit unit) {
        return scheduleOnService(callable, delay, unit, asyncThreadExecutor);
    }



    @Override
    public ScheduledFuture<?> scheduleAsync(Runnable command, long delay, TimeUnit unit) {
        return asyncThreadExecutor.schedule(() -> {
            try {
                command.run();
            } catch (Exception e) {
                log.warn("Error in scheduled task", e);
            }
        }, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleRepeatAsync(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return asyncThreadExecutor.scheduleAtFixedRate(() -> {
            try {
                command.run();
            } catch (Exception e) {
                log.warn("Error in scheduled task", e);
            }
        }, initialDelay, period, unit);
    }

    @Override
    public void callEvent(MSMEvent event) {
        List<CustomListener> listenersToCall = new ArrayList<>();

        for (Map.Entry<CustomListener, HashSet<String>> listenerEntry : listeners.entrySet()) {
            if (!event.getMinecraftServer().getSupportedProtocols().containsAll(listenerEntry.getValue())) continue;

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

    @Override
    public void registerCommand(CommandInfo command) {
        commandMap.put(command.getName(), command);

        commandAliasMap.put(command.getName(), command);

        for (String alias : command.getAliases()) {
            commandAliasMap.put(alias, command);
        }
    }

    @Override
    public CommandInfo getCommand(String name) {
        if (name == null) return null;

        return commandAliasMap.get(name);
    }

    @Override
    public Collection<CommandInfo> getRegisteredCommands() {
        return commandMap.values();
    }

    @Override
    public Collection<PermissionInfo> getRegisteredPermissions() {
        return permissionMap.values();
    }

    @Override
    public void registerPermission(PermissionInfo permission) {
        permissionMap.put(permission.getName(), permission);
    }

    @Override
    public void broadcast(String message) {
        for (MinecraftServer server : minecraftServerMap.values()) {
            server.broadcast(message);
        }
    }

    @Override
    public DirectoryWatcher getDirectoryWatcher() {
        return directoryWatcher;
    }

    @Override
    public void addLanguageLookup(LanguageLookup lookup) {
        languageLookup.addLanguageLookup(lookup);
    }

    private <V> ScheduledFuture<V> scheduleOnService(Callable<V> callable, long delay, TimeUnit unit,
                                                     ScheduledExecutorService service) {
        return service.schedule(() -> {
            try {
                return callable.call();
            } catch (Exception e) {
                log.warn("Error in scheduled task", e);
                return null;
            }
        }, delay, unit);
    }

    public void stop() {
        if(stopped) return;
        stopped = true;

        consoleHandler.setStopped(true);

        if(channel != null) channel.close();

        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    public void restart() {
        if (restartScript == null || !Files.exists(Paths.get(restartScript))) {
            log.warn("No restart script specified");
        } else {
            Thread shutdownHook = new Thread() {
                @Override
                public void run() {
                    try {
                        Runtime.getRuntime().exec(new String[]{"sh", restartScript});
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            shutdownHook.setDaemon(true);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }

        stop();
    }

    public void addQuittingPlayer(MSMPlayer quitting) {
        quittingPlayers.put(quitting.getUUID(), quitting);

        schedule(() -> {
            if (quittingPlayers.remove(quitting.getUUID()) == null) return;

            callEvent(new PlayerQuitEvent(quitting));
        }, 1, TimeUnit.SECONDS);
    }

    public MSMPlayer removeQuittingPlayer(UUID uuid) {
        MSMPlayer player = quittingPlayers.remove(uuid);

        if (player != null) return player;

        return getPlayer(uuid);
    }

    public MSMMinecraftServer assignMinecraftServerToConnection(Config config, MSMConnection connection) {
        MSMMinecraftServer server = getMinecraftServer(config.getString("name"));

        if (server == null) {
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
                setupPipeline(ch.pipeline());
            }
        });
        b.childOption(ChannelOption.SO_KEEPALIVE, true);

        try {
            ChannelFuture future = b.bind(port).sync();
            log.info("Server started on port " + port);

            future.channel().closeFuture().addListener(unused1 -> {
                for (ServerListener protocol : protocolToPluginMap.values()) {
                    protocol.serverStopped(this);
                }

                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            });

            this.channel = future.channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (ServerListener protocol : protocolToPluginMap.values()) {
            protocol.serverStarted(this);
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

    @Override
    public void sendMessage(String message) {
        sendMessageNoPrefix(message);
    }

    @Override
    public void sendMessageNoPrefix(String message) {
        broadcast(message);
    }

    @Override
    public void sendLocale(String locale, Object... args) {
        sendLocaleNoPrefix(locale, args);
    }

    @Override
    public void sendLocaleNoPrefix(String locale, Object... args) {
        sendMessageNoPrefix(languageLookup.getLocale(locale, args));
    }

    @Override
    public LanguageLookup getLanguageLookup() {
        return languageLookup;
    }

    @Override
    public String getLocale(String name) {
        return languageLookup.getLocale(name);
    }

    @Override
    public String getLocale(String name, Object... args) {
        return languageLookup.getLocale(name, args);
    }

    @Override
    public boolean hasLocale(String name) {
        return languageLookup.hasLocale(name);
    }

    public void setConsoleHandler(ConsoleHandler consoleHandler) {
        this.consoleHandler = consoleHandler;
    }
}

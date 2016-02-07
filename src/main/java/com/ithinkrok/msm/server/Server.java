package com.ithinkrok.msm.server;

import com.ithinkrok.msm.server.command.MSMCommandInfo;
import com.ithinkrok.msm.server.event.MSMEvent;
import com.ithinkrok.util.event.CustomListener;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by paul on 04/02/16.
 */
public interface Server {

    Set<String> getAvailableProtocols();

    MinecraftServer getMinecraftServer(String name);

    Collection<MinecraftServer> getMinecraftServers();

    Player getPlayer(UUID uuid);

    <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

    ScheduledFuture<?> scheduleRepeat(Runnable command, long initialDelay, long period, TimeUnit unit);

    <V> ScheduledFuture<V> scheduleAsync(Callable<V> callable, long delay, TimeUnit unit);

    ScheduledFuture<?> scheduleAsync(Runnable command, long delay, TimeUnit unit);

    ScheduledFuture<?> scheduleRepeatAsync(Runnable command, long initialDelay, long period, TimeUnit unit);

    void callEvent(MSMEvent event);

    /**
     * Registers a listener with the server
     *
     * @param listener The listener to register with the server
     * @param requireProtocols The protocols that connected minecraft servers must support for their events to be
     *                         sent to this listener
     */
    void registerListener(CustomListener listener, String...requireProtocols);

    void unregisterListener(CustomListener listener);

    void registerCommand(MSMCommandInfo command);

    MSMCommandInfo getCommand(String name);

    Collection<MSMCommandInfo> getRegisteredCommands();

    void broadcast(String message);
}

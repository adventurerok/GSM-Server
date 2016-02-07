package com.ithinkrok.msm.server;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by paul on 04/02/16.
 */
public interface Server {

    Set<String> getAvailableProtocols();

    MinecraftServer getMinecraftServer(String name);

    <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

    ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

    ScheduledFuture<?> scheduleRepeat(Runnable command, long initialDelay, long period, TimeUnit unit);

    <V> ScheduledFuture<V> scheduleAsync(Callable<V> callable, long delay, TimeUnit unit);

    ScheduledFuture<?> scheduleAsync(Runnable command, long delay, TimeUnit unit);

    ScheduledFuture<?> scheduleRepeatAsync(Runnable command, long initialDelay, long period, TimeUnit unit);
}

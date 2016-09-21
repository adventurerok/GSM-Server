package com.ithinkrok.msm.server;

import com.ithinkrok.msm.server.external.DiscordChat;
import com.ithinkrok.msm.server.external.External;
import com.ithinkrok.util.io.DirectoryWatcher;
import com.ithinkrok.msm.server.command.CommandHandler;
import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.data.Player;
import com.ithinkrok.msm.server.data.PlayerIdentifier;
import com.ithinkrok.msm.server.event.MSMEvent;
import com.ithinkrok.msm.server.permission.PermissionInfo;
import com.ithinkrok.util.event.CustomListener;
import com.ithinkrok.util.lang.LanguageLookup;
import com.ithinkrok.util.lang.Messagable;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by paul on 04/02/16.
 */
public interface Server extends Messagable, LanguageLookup {

    Set<String> getAvailableProtocols();

    Client<?> getClient(String name);

    Collection<Client<?>> getClients();

    default Player<?> getPlayer(String clientType, UUID uuid){
        return getPlayer(new PlayerIdentifier(clientType, uuid));
    }

    Player<?> getPlayer(PlayerIdentifier playerIdentifier);

    Player<?> getPlayer(String clientType, String name);

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

    Collection<PermissionInfo> getRegisteredPermissions();

    void registerPermission(PermissionInfo permission);

    void broadcast(String message);

    DirectoryWatcher getDirectoryWatcher();

    void addLanguageLookup(LanguageLookup lookup);

    CommandHandler getCommandHandler();

    void addExternal(External external);

    External getExternal(String name);

    Collection<External> getExternals();

    void broadcastExternalChat(String message);
}

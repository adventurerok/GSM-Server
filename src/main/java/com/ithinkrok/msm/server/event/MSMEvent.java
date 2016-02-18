package com.ithinkrok.msm.server.event;

import com.ithinkrok.msm.server.MinecraftServer;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.util.event.CustomEvent;

/**
 * Created by paul on 07/02/16.
 */
public abstract class MSMEvent implements CustomEvent {

    private final MinecraftServer minecraftServer;

    public MSMEvent(MinecraftServer minecraftServer) {
        this.minecraftServer = minecraftServer;
    }

    public MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    public Server getMSMServer() {
        return minecraftServer.getConnectedTo();
    }
}

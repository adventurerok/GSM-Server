package com.ithinkrok.msm.server.event.minecraftserver;

import com.ithinkrok.msm.server.MinecraftServer;
import com.ithinkrok.msm.server.event.MSMEvent;

/**
 * Created by paul on 17/02/16.
 */
public class MinecraftServerDisconnectEvent extends MSMEvent {

    public MinecraftServerDisconnectEvent(MinecraftServer minecraftServer) {
        super(minecraftServer);
    }
}

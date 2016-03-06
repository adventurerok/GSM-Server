package com.ithinkrok.msm.server.event.minecraftserver;

import com.ithinkrok.msm.server.data.MinecraftClient;
import com.ithinkrok.msm.server.event.MSMEvent;

/**
 * Created by paul on 17/02/16.
 */
public class MinecraftServerDisconnectEvent extends MSMEvent {

    public MinecraftServerDisconnectEvent(MinecraftClient minecraftClient) {
        super(minecraftClient);
    }
}

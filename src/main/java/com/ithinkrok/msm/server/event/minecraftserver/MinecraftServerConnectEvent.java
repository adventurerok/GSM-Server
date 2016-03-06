package com.ithinkrok.msm.server.event.minecraftserver;

import com.ithinkrok.msm.server.data.MinecraftClient;
import com.ithinkrok.msm.server.event.MSMEvent;

/**
 * Created by paul on 17/02/16.
 */
public class MinecraftServerConnectEvent extends MSMEvent {

    public MinecraftServerConnectEvent(MinecraftClient minecraftClient) {
        super(minecraftClient);
    }
}

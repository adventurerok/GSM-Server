package com.ithinkrok.msm.server.event.minecraftserver;

import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.minecraft.MinecraftClient;
import com.ithinkrok.msm.server.event.MSMEvent;

/**
 * Created by paul on 17/02/16.
 */
public class MinecraftServerConnectEvent extends MSMEvent {

    public MinecraftServerConnectEvent(Client<?> minecraftClient) {
        super(minecraftClient);
    }
}

package com.ithinkrok.msm.server.event.minecraftserver;

import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.minecraft.MinecraftClient;
import com.ithinkrok.msm.server.event.MSMEvent;

/**
 * Created by paul on 17/02/16.
 */
public class MinecraftServerDisconnectEvent extends MSMEvent {

    public MinecraftServerDisconnectEvent(Client<?> minecraftClient) {
        super(minecraftClient);
    }
}

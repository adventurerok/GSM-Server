package com.ithinkrok.msm.server.event;

import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.minecraft.MinecraftClient;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.util.event.CustomEvent;

/**
 * Created by paul on 07/02/16.
 */
public abstract class MSMEvent implements CustomEvent {

    private final Client<?> minecraftClient;

    public MSMEvent(Client<?> minecraftClient) {
        this.minecraftClient = minecraftClient;
    }

    public Client<?> getMinecraftClient() {
        return minecraftClient;
    }

    public Server getMSMServer() {
        return minecraftClient.getConnectedTo();
    }
}

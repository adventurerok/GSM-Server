package com.ithinkrok.msm.server.event;

import com.ithinkrok.msm.server.data.MinecraftClient;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.util.event.CustomEvent;

/**
 * Created by paul on 07/02/16.
 */
public abstract class MSMEvent implements CustomEvent {

    private final MinecraftClient minecraftClient;

    public MSMEvent(MinecraftClient minecraftClient) {
        this.minecraftClient = minecraftClient;
    }

    public MinecraftClient getMinecraftClient() {
        return minecraftClient;
    }

    public Server getMSMServer() {
        return minecraftClient.getConnectedTo();
    }
}

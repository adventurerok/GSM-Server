package com.ithinkrok.msm.server.event.player;

import com.ithinkrok.msm.server.data.MinecraftClient;
import com.ithinkrok.msm.server.data.MinecraftPlayer;

/**
 * Created by paul on 07/02/16.
 */
public class PlayerChangeServerEvent extends PlayerEvent {

    private final MinecraftClient oldServer;

    public PlayerChangeServerEvent(MinecraftPlayer player, MinecraftClient oldServer) {
        super(player);
        this.oldServer = oldServer;
    }

    public MinecraftClient getOldServer() {
        return oldServer;
    }

    public MinecraftClient getNewServer() {
        return getMinecraftClient();
    }
}

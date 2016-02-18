package com.ithinkrok.msm.server.event.player;

import com.ithinkrok.msm.server.data.MinecraftServer;
import com.ithinkrok.msm.server.data.Player;

/**
 * Created by paul on 07/02/16.
 */
public class PlayerChangeServerEvent extends PlayerEvent {

    private final MinecraftServer oldServer;

    public PlayerChangeServerEvent(Player player, MinecraftServer oldServer) {
        super(player);
        this.oldServer = oldServer;
    }

    public MinecraftServer getOldServer() {
        return oldServer;
    }

    public MinecraftServer getNewServer() {
        return getMinecraftServer();
    }
}

package com.ithinkrok.msm.server.event.player;

import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.data.Player;
import com.ithinkrok.msm.server.minecraft.MinecraftClient;
import com.ithinkrok.msm.server.minecraft.MinecraftPlayer;

/**
 * Created by paul on 07/02/16.
 */
public class PlayerChangeServerEvent extends PlayerEvent {

    private final Client<?> oldServer;

    public PlayerChangeServerEvent(Player<?> player, Client<?> oldServer) {
        super(player);
        this.oldServer = oldServer;
    }

    public Client<?> getOldServer() {
        return oldServer;
    }

    public Client<?> getNewServer() {
        return getMinecraftClient();
    }
}

package com.ithinkrok.msm.server.event.player;

import com.ithinkrok.msm.server.MinecraftServer;
import com.ithinkrok.msm.server.Player;

/**
 * Created by paul on 07/02/16.
 */
public class PlayerJoinEvent extends PlayerEvent {

    public PlayerJoinEvent(MinecraftServer minecraftServer, Player player) {
        super(minecraftServer, player);
    }
}

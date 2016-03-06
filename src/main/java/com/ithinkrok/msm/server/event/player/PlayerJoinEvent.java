package com.ithinkrok.msm.server.event.player;

import com.ithinkrok.msm.server.minecraft.MinecraftPlayer;

/**
 * Created by paul on 07/02/16.
 */
public class PlayerJoinEvent extends PlayerEvent {

    public PlayerJoinEvent(MinecraftPlayer player) {
        super(player);
    }
}

package com.ithinkrok.msm.server.event.player;

import com.ithinkrok.msm.server.data.MinecraftPlayer;
import com.ithinkrok.msm.server.event.MSMEvent;

/**
 * Created by paul on 07/02/16.
 */
public class PlayerEvent extends MSMEvent {

    private final MinecraftPlayer player;

    public PlayerEvent(MinecraftPlayer player) {
        super(player.getServer());
        this.player = player;
    }

    public MinecraftPlayer getPlayer() {
        return player;
    }
}

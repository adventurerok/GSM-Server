package com.ithinkrok.msm.server.event.player;

import com.ithinkrok.msm.server.Player;

/**
 * Created by paul on 07/02/16.
 */
public class PlayerQuitEvent extends PlayerEvent {

    public PlayerQuitEvent(Player player) {
        super(player);
    }
}

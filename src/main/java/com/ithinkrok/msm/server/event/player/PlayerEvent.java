package com.ithinkrok.msm.server.event.player;

import com.ithinkrok.msm.server.MinecraftServer;
import com.ithinkrok.msm.server.Player;
import com.ithinkrok.msm.server.event.MSMEvent;

/**
 * Created by paul on 07/02/16.
 */
public class PlayerEvent extends MSMEvent {

    private final Player player;

    public PlayerEvent(MinecraftServer minecraftServer, Player player) {
        super(minecraftServer);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}

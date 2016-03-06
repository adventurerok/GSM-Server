package com.ithinkrok.msm.server.minecraft;

import com.ithinkrok.msm.server.data.Player;

/**
 * Created by paul on 06/02/16.
 */
public interface MinecraftPlayer extends Player<MinecraftClient> {

    void changeServer(MinecraftClient newServer);

}

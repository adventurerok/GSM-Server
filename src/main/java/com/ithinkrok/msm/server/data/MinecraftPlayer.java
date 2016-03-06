package com.ithinkrok.msm.server.data;

/**
 * Created by paul on 06/02/16.
 */
public interface MinecraftPlayer extends Player<MinecraftClient> {

    void changeServer(MinecraftClient newServer);

}

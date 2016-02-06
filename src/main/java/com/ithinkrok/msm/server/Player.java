package com.ithinkrok.msm.server;

import java.util.UUID;

/**
 * Created by paul on 06/02/16.
 */
public interface Player {

    String getName();

    UUID getUUID();

    void sendMessage(String message);

    MinecraftServer getServer();
}

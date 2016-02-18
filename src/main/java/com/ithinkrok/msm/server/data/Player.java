package com.ithinkrok.msm.server.data;

import com.ithinkrok.msm.server.data.MinecraftServer;
import com.ithinkrok.util.command.CustomCommandSender;

import java.util.UUID;

/**
 * Created by paul on 06/02/16.
 */
public interface Player extends CustomCommandSender {

    String getName();

    UUID getUUID();

    MinecraftServer getServer();

    void changeServer(MinecraftServer newServer);

    void kick(String reason);
}

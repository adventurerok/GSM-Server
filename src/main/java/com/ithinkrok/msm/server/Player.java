package com.ithinkrok.msm.server;

import com.ithinkrok.util.command.CustomCommandSender;

import java.util.UUID;

/**
 * Created by paul on 06/02/16.
 */
public interface Player extends CustomCommandSender {

    String getName();

    UUID getUUID();

    MinecraftServer getServer();
}

package com.ithinkrok.msm.server.data;

import com.ithinkrok.util.command.CustomCommandSender;

import java.util.UUID;

/**
 * Created by paul on 06/03/16.
 */
public interface Player<T extends Client<?>> extends CustomCommandSender {

    String getName();

    UUID getUUID();

    PlayerIdentifier getIdentifier();

    void kick(String reason);

    T getClient();

    void setClient(T client);
}

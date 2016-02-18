package com.ithinkrok.msm.server.impl;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.data.MinecraftServer;
import com.ithinkrok.util.command.CustomCommandSender;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.lang.LanguageLookup;

/**
 * Created by paul on 18/02/16.
 */
public class MinecraftCommandSender implements CustomCommandSender {

    private final MinecraftServer minecraftServer;

    public MinecraftCommandSender(MinecraftServer minecraftServer) {
        this.minecraftServer = minecraftServer;
    }

    @Override
    public void sendMessage(String message) {
        sendMessageNoPrefix(message);
    }

    @Override
    public void sendMessageNoPrefix(String message) {
        Connection connection = minecraftServer.getConnection();
        if(connection == null) return;

        Channel channel = connection.getChannel("MSMAPI");
        if(channel == null) return;

        Config payload = new MemoryConfig();
        payload.set("mode", "ConsoleMessage");
        payload.set("message", message);

        channel.write(payload);
    }

    @Override
    public void sendLocale(String locale, Object... args) {
        sendLocaleNoPrefix(locale, args);
    }

    @Override
    public void sendLocaleNoPrefix(String locale, Object... args) {
        sendMessageNoPrefix(getLanguageLookup().getLocale(locale, args));
    }

    @Override
    public LanguageLookup getLanguageLookup() {
        return minecraftServer.getLanguageLookup();
    }
}

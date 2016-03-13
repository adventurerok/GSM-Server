package com.ithinkrok.msm.server.command;

import com.ithinkrok.msm.server.Server;
import com.ithinkrok.util.command.CustomCommandSender;
import com.ithinkrok.util.lang.LanguageLookup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by paul on 18/02/16.
 */
public class ConsoleCommandSender implements CustomCommandSender {

    private final Logger log = LogManager.getLogger(ConsoleCommandSender.class);

    private final Server server;

    public ConsoleCommandSender(Server server) {
        this.server = server;
    }

    @Override
    public void sendMessage(String message) {
        sendMessageNoPrefix("[Command] " + message);
    }

    @Override
    public void sendMessageNoPrefix(String message) {
        log.info(message);
    }

    @Override
    public void sendLocale(String locale, Object... args) {
        sendMessage(server.getLocale(locale, args));
    }

    @Override
    public void sendLocaleNoPrefix(String locale, Object... args) {
        sendMessageNoPrefix(server.getLocale(locale, args));
    }

    @Override
    public LanguageLookup getLanguageLookup() {
        return server.getLanguageLookup();
    }

    public Server getServer() {
        return server;
    }
}

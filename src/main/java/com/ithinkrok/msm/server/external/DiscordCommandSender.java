package com.ithinkrok.msm.server.external;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

/**
 * Created by paul on 21/09/16.
 */
public class DiscordCommandSender extends ExternalCommandSender {

    private final Logger log = LogManager.getLogger(DiscordCommandSender.class);

    private final IChannel outputChannel;

    public DiscordCommandSender(External external, IChannel outputChannel) {
        super(external);
        this.outputChannel = outputChannel;
    }

    @Override
    public void sendMessageNoPrefix(String message) {
        try {
            outputChannel.sendMessage(message);
        } catch (MissingPermissionsException | RateLimitException | DiscordException e) {
            log.warn("Failed to send discord reply to command", e);
        }
    }
}

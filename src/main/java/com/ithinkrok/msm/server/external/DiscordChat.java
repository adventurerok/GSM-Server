package com.ithinkrok.msm.server.external;

import com.ithinkrok.msm.server.Server;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.lang.LanguageLookup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

/**
 * Created by paul on 21/09/16.
 */
public class DiscordChat implements ExternalChat, IListener<ReadyEvent> {

    private static final Logger log = LogManager.getLogger(DiscordChat.class);

    private final Server connectedTo;

    private final IDiscordClient client;

    private final String generalChannelName;

    private IChannel generalChannel;

    public DiscordChat(Server connectedTo, Config config) throws DiscordException {
        this.connectedTo = connectedTo;
        this.generalChannelName = config.getString("channel");

        ClientBuilder clientBuilder = new ClientBuilder();
        clientBuilder.withToken(config.getString("token"));

        client = clientBuilder.build();

        EventDispatcher dispatcher = client.getDispatcher();
        dispatcher.registerListener(this);

        client.login();
    }

    @Override
    public Server getConnectedTo() {
        return connectedTo;
    }

    @Override
    public void sendMessageNoPrefix(String message) {
        if(generalChannel == null) return;

        try {
            generalChannel.sendMessage(message);
        } catch (MissingPermissionsException | RateLimitException | DiscordException e) {
            log.warn("Discord chat error", e);
        }
    }

    @Override
    public LanguageLookup getLanguageLookup() {
        return connectedTo.getLanguageLookup();
    }

    @Override
    public void handle(ReadyEvent event) {
        generalChannel = client.getChannelByID(generalChannelName);
    }
}

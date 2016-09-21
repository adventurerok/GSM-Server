package com.ithinkrok.msm.server.external;

import com.ithinkrok.msm.common.command.CommandInfo;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.event.command.ExternalCommandEvent;
import com.ithinkrok.msm.server.permission.PermissionDefault;
import com.ithinkrok.msm.server.permission.PermissionInfo;
import com.ithinkrok.util.command.CustomCommand;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.lang.LanguageLookup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

/**
 * Created by paul on 21/09/16.
 */
public class DiscordChat implements ExternalChat {

    private static final Logger log = LogManager.getLogger(DiscordChat.class);

    private final Server connectedTo;

    private final IDiscordClient client;

    private final String inviteCode;
    private final String generalChannelID;

    private IChannel generalChannel;

    public DiscordChat(Server connectedTo, Config config) throws DiscordException {
        this.connectedTo = connectedTo;
        this.inviteCode = config.getString("invite_code", null);
        this.generalChannelID = config.getString("channel");

        ClientBuilder clientBuilder = new ClientBuilder();
        clientBuilder.withToken(config.getString("token"));

        client = clientBuilder.build();

        EventDispatcher dispatcher = client.getDispatcher();
        dispatcher.registerListener(new ReadyEventListener());
        dispatcher.registerListener(new MessageRecievedEventListener());

        client.login();
    }

    @Override
    public Server getConnectedTo() {
        return connectedTo;
    }

    @Override
    public String getName() {
        return "discord";
    }

    @Override
    public void sendMessageNoPrefix(String message) {
        if (generalChannel == null) return;

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

    private class ReadyEventListener implements IListener<ReadyEvent> {
        @Override
        public void handle(ReadyEvent event) {
            if (inviteCode != null) {
                try {
                    client.getInviteForCode(inviteCode).accept();
                } catch (DiscordException | RateLimitException e) {
                    log.warn("Failed to accept discord invite", e);
                }
            }

            generalChannel = client.getChannelByID(generalChannelID);
        }
    }

    private class MessageRecievedEventListener implements IListener<MessageReceivedEvent> {

        @Override
        public void handle(MessageReceivedEvent event) {
            String text = event.getMessage().getContent();

            if(text == null || !text.startsWith("!")) return;

            text = text.substring(1);

            ExternalCommandSender sender = new DiscordCommandSender(DiscordChat.this, event.getMessage().getChannel());
            CustomCommand command = new CustomCommand(text);

            CommandInfo commandInfo = connectedTo.getCommandHandler().getCommand(command.getCommand());

            if(commandInfo != null && commandInfo.getPermission() != null && !commandInfo.getPermission().isEmpty()) {
                PermissionInfo permission = connectedTo.getRegisteredPermission(commandInfo.getPermission());

                if(permission == null || permission.getDefaultValue() == PermissionDefault.FALSE || permission
                        .getDefaultValue() == PermissionDefault.OP) {

                    sender.sendMessage("This command requires permissions, and Discord does not support permissions");

                    return;
                }
            }

            ExternalCommandEvent commandEvent = new ExternalCommandEvent(sender, command);

            if(!connectedTo.getCommandHandler().executeCommand(commandEvent)) return;

            if(!commandEvent.isHandled()) {
                sender.sendMessage("This command does not support Discord");
            }
        }
    }


}

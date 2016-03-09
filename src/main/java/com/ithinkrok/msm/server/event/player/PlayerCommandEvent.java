package com.ithinkrok.msm.server.event.player;

import com.ithinkrok.msm.server.data.Player;
import com.ithinkrok.msm.server.event.command.MSMCommandEvent;
import com.ithinkrok.util.command.CustomCommand;
import com.ithinkrok.util.command.CustomCommandSender;

/**
 * Created by paul on 07/02/16.
 */
public class PlayerCommandEvent extends PlayerEvent implements MSMCommandEvent {


    private final CustomCommand command;

    private boolean handled = false;

    private boolean validCommand = true;

    public PlayerCommandEvent(Player<?> player, CustomCommand command) {
        super(player);
        this.command = command;
    }

    @Override
    public CustomCommand getCommand() {
        return command;
    }

    @Override
    public CustomCommandSender getCommandSender() {
        return getPlayer();
    }

    @Override
    public boolean isHandled() {
        return handled;
    }

    @Override
    public void setHandled(boolean handled) {
        this.handled = handled;
    }

    @Override
    public boolean isValidCommand() {
        return validCommand;
    }

    @Override
    public void setValidCommand(boolean validCommand) {
        this.validCommand = validCommand;
    }
}

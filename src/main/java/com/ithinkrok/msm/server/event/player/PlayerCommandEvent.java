package com.ithinkrok.msm.server.event.player;

import com.ithinkrok.msm.server.Player;
import com.ithinkrok.util.command.CustomCommand;
import com.ithinkrok.util.command.CustomCommandSender;
import com.ithinkrok.util.command.event.CustomCommandEvent;

/**
 * Created by paul on 07/02/16.
 */
public class PlayerCommandEvent extends PlayerEvent implements CustomCommandEvent {


    private final CustomCommand command;

    private boolean handled = false;

    private boolean validCommand = true;

    public PlayerCommandEvent(Player player, CustomCommand command) {
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

package com.ithinkrok.msm.server.event.command;

import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.command.ConsoleCommandSender;
import com.ithinkrok.util.command.CustomCommand;
import com.ithinkrok.util.command.CustomCommandSender;

/**
 * Created by paul on 18/02/16.
 */
public class ConsoleCommandEvent implements MSMCommandEvent {

    private final ConsoleCommandSender sender;
    private final CustomCommand command;

    private boolean handled = false;

    private boolean validCommand = true;

    public ConsoleCommandEvent(ConsoleCommandSender sender, CustomCommand command) {
        this.sender = sender;
        this.command = command;
    }

    @Override
    public Server getMSMServer() {
        return sender.getServer();
    }

    @Override
    public CustomCommand getCommand() {
        return command;
    }

    @Override
    public CustomCommandSender getCommandSender() {
        return sender;
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

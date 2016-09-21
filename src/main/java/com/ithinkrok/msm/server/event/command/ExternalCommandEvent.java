package com.ithinkrok.msm.server.event.command;

import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.external.ExternalCommandSender;
import com.ithinkrok.util.command.CustomCommand;

/**
 * Created by paul on 21/09/16.
 */
public class ExternalCommandEvent implements MSMCommandEvent {
    private final ExternalCommandSender sender;
    private final CustomCommand command;

    private boolean handled = false;

    private boolean validCommand = true;

    public ExternalCommandEvent(ExternalCommandSender sender, CustomCommand command) {
        this.sender = sender;
        this.command = command;
    }

    @Override
    public Server getMSMServer() {
        return sender.getExternal().getConnectedTo();
    }

    @Override
    public CustomCommand getCommand() {
        return command;
    }

    @Override
    public ExternalCommandSender getCommandSender() {
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

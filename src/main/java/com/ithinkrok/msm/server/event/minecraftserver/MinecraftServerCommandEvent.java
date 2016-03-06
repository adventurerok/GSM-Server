package com.ithinkrok.msm.server.event.minecraftserver;

import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.minecraft.MinecraftClient;
import com.ithinkrok.msm.server.event.MSMCommandEvent;
import com.ithinkrok.msm.server.event.MSMEvent;
import com.ithinkrok.util.command.CustomCommand;
import com.ithinkrok.util.command.CustomCommandSender;

/**
 * Created by paul on 18/02/16.
 */
public class MinecraftServerCommandEvent extends MSMEvent implements MSMCommandEvent {

    private final CustomCommand command;

    public MinecraftServerCommandEvent(Client<?> minecraftClient, CustomCommand command) {
        super(minecraftClient);
        this.command = command;
    }

    private boolean handled = false;

    private boolean validCommand = true;

    @Override
    public CustomCommand getCommand() {
        return command;
    }

    @Override
    public CustomCommandSender getCommandSender() {
        return getMinecraftClient().getConsoleCommandSender();
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

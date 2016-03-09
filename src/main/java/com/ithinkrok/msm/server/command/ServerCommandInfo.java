package com.ithinkrok.msm.server.command;

import com.ithinkrok.msm.common.command.CommandInfo;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.event.CustomListener;

/**
 * Created by paul on 07/02/16.
 */
public class ServerCommandInfo extends CommandInfo {

    private CustomListener commandListener;

    public ServerCommandInfo(String name, Config config,
                             CustomListener commandListener) {
        super(name, config);
        this.commandListener = commandListener;
    }

    public void setCommandListener(CustomListener commandListener) {
        this.commandListener = commandListener;
    }

    public CustomListener getCommandListener() {
        return commandListener;
    }

}

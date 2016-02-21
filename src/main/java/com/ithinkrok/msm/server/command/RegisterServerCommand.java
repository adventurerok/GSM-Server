package com.ithinkrok.msm.server.command;

import com.google.common.base.Charsets;
import com.ithinkrok.msm.server.auth.PasswordManager;
import com.ithinkrok.msm.server.event.MSMCommandEvent;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.event.CustomEventHandler;
import com.ithinkrok.util.event.CustomListener;

/**
 * Created by paul on 21/02/16.
 */
public class RegisterServerCommand implements CustomListener {

    private final PasswordManager passwordManager;

    public RegisterServerCommand(PasswordManager passwordManager) {
        this.passwordManager = passwordManager;
    }

    @CustomEventHandler
    public void onCommand(MSMCommandEvent event) {
        event.setHandled(true);

        if(!event.getCommand().hasArg(1)) {
            event.setValidCommand(false);
            return;
        }

        String serverName = event.getCommand().getStringArg(0, null);

        if(passwordManager.hasServer(serverName) && !event.getCommand().getBooleanParam("force", false)) {
            event.getCommandSender().sendMessage("Server already registered. To force, use the -force! flag");
            return;
        }

        String password = event.getCommand().getRemainingArgsAsString(1);

        passwordManager.registerServer(serverName, password.getBytes(Charsets.UTF_8));

        event.getCommandSender().sendMessage("Registered server " + serverName + " successfully!");
    }

    public static CommandInfo createCommandInfo(PasswordManager passwordManager) {
        Config config = new MemoryConfig();

        config.set("usage", "/<command> <server> <password...>");
        config.set("description", "Register a server");
        config.set("permission", "msmserver.registerserver");

        return new CommandInfo("registerserver", config, new RegisterServerCommand(passwordManager));
    }
}

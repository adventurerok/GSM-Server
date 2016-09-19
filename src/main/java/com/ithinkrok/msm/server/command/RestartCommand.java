package com.ithinkrok.msm.server.command;

import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.event.command.MSMCommandEvent;
import com.ithinkrok.msm.server.impl.MSMServer;
import com.ithinkrok.util.command.CustomCommand;
import com.ithinkrok.util.event.CustomEventHandler;
import com.ithinkrok.util.event.CustomListener;

import java.util.regex.Pattern;

/**
 * Created by paul on 18/02/16.
 */
public class RestartCommand implements CustomListener {

    @CustomEventHandler
    public void onCommand(MSMCommandEvent event) {
        CustomCommand command = event.getCommand();

        event.setHandled(true);

        if(command.getArgumentCount() == 0) {
            event.getCommandSender().sendMessage("Restarting server");
            ((MSMServer) event.getMSMServer()).restart();
            return;
        }

        String serverName = event.getCommand().getStringArg(0, "");

        if (serverName.startsWith("/") && serverName.endsWith("/")) {
            serverName = serverName.substring(1, serverName.length() - 1);

            Pattern pattern;
            try {
                pattern = Pattern.compile(serverName);
            } catch (Exception ignored) {
                event.getCommandSender().sendMessage("Invalid regex: " + serverName);
                return;
            }

            for (Client<?> server : event.getMSMServer().getClients()) {
                if (!pattern.matcher(server.getName()).matches()) continue;
                if (!server.isConnected()) continue;

                restartClient(event, server);
            }
        } else {
            Client<?> server = event.getMSMServer().getClient(serverName);

            if (server == null || !server.isConnected()) {
                event.getCommandSender().sendMessage("Unknown server: " + serverName);
                return;
            }

            restartClient(event, server);
        }
    }

    public void restartClient(MSMCommandEvent event, Client<?> client) {
        if(client.isRestartScheduled()) {
            event.getCommandSender().sendMessage(client.getName() + " has a restart scheduled, but we will force");
        }

        //Force restart with max players
        client.scheduleRestart(10, 1000);
    }
}

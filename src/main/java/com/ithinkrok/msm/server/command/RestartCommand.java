package com.ithinkrok.msm.server.command;

import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.event.command.MSMCommandEvent;
import com.ithinkrok.msm.server.impl.MSMServer;
import com.ithinkrok.util.command.CustomCommand;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.event.CustomEventHandler;
import com.ithinkrok.util.event.CustomListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by paul on 18/02/16.
 */
public class RestartCommand implements CustomListener {

    public static ServerCommandInfo createCommandInfo() {
        Config config = new MemoryConfig();
        config.set("usage", "/<command> [server/pattern]");
        config.set("description", "Restarts the MSM server or some servers connected to it");
        config.set("permission", "msmserver.restart");

        List<Config> tabCompletion = new ArrayList<>();

        Config allServers = new MemoryConfig();
        allServers.set("pattern", "");
        allServers.set("values", Collections.singletonList("#gsmServers"));

        tabCompletion.add(allServers);

        config.set("tab_complete", tabCompletion);

        return new ServerCommandInfo("mrestart", config, new RestartCommand());
    }

    @CustomEventHandler
    public void onCommand(MSMCommandEvent event) {
        event.setHandled(true);

        CustomCommand command = event.getCommand();

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

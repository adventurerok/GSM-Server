package com.ithinkrok.msm.server.command;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.event.command.ConsoleCommandEvent;
import com.ithinkrok.msm.server.event.command.MSMCommandEvent;
import com.ithinkrok.msm.server.event.minecraftserver.ClientCommandEvent;
import com.ithinkrok.msm.server.event.player.PlayerCommandEvent;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.event.CustomEventHandler;
import com.ithinkrok.util.event.CustomListener;

import java.util.regex.Pattern;

/**
 * Created by paul on 18/02/16.
 */
public class ExecCommand implements CustomListener {

    @CustomEventHandler
    public void onCommand(MSMCommandEvent event) {
        event.setHandled(true);

        if(!event.getCommand().hasArg(1)) {
            event.setValidCommand(false);
            return;
        }

        String serverName = event.getCommand().getStringArg(0, "");

        if(serverName.startsWith("/") && serverName.endsWith("/")) {
            serverName = serverName.substring(1, serverName.length() - 1);

            Pattern pattern;
            try {
                pattern = Pattern.compile(serverName);
            } catch (Exception ignored) {
                event.getCommandSender().sendMessage("Invalid regex: " + serverName);
                return;
            }

            for(Client<?> server : event.getMSMServer().getClients()) {
                if(!pattern.matcher(server.getName()).matches()) continue;
                if(!server.isConnected()) continue;

                execCommand(server, event);
            }
        } else{
            Client<?> server = event.getMSMServer().getClient(serverName);

            if (server == null || !server.isConnected()) {
                event.getCommandSender().sendMessage("Unknown minecraft server: " + serverName);
                return;
            }

            execCommand(server, event);
        }
    }

    private void execCommand(Client<?> server, MSMCommandEvent event) {
        Connection connection = server.getConnection();

        if(connection == null) {
            event.getCommandSender().sendMessage("Minecraft server not connected: " + server.getName());
            return;
        }

        Channel channel = connection.getChannel("MSMAPI");

        Config payload = new MemoryConfig();
        payload.set("mode", "ExecCommand");
        payload.set("command", event.getCommand().getRemainingArgsAndParamsAsString(1, "console", "c"));

        Config sender = new MemoryConfig();

        if(event instanceof PlayerCommandEvent) {
            sender.set("type", "player");
            sender.set("uuid", ((PlayerCommandEvent) event).getPlayer().getUUID());
        } else if(event instanceof ConsoleCommandEvent) {
            sender.set("type", "msm_console");
        } else if(event instanceof ClientCommandEvent) {
            sender.set("type", "minecraft");
            sender.set("name", ((ClientCommandEvent) event).getClient().getName());
        } else {
            sender.set("type", "unknown");
        }

        payload.set("sender", sender);

        //Allows use of vanilla commands
        if(event.getCommand().getBooleanParam("console", false) || event.getCommand().getBooleanParam("c", false)) {
            payload.set("console", true);
        }

        channel.write(payload);
    }

    public static ServerCommandInfo createCommandInfo() {
        Config config = new MemoryConfig();

        config.set("usage", "/<command> <server> <server command...>");
        config.set("description", "Execute a command on a minecraft server");
        config.set("permission", "msmserver.exec");

        return new ServerCommandInfo("mexec", config, new ExecCommand());
    }
}

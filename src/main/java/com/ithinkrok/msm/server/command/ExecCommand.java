package com.ithinkrok.msm.server.command;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.data.MinecraftServer;
import com.ithinkrok.msm.server.event.ConsoleCommandEvent;
import com.ithinkrok.msm.server.event.MSMCommandEvent;
import com.ithinkrok.msm.server.event.minecraftserver.MinecraftServerCommandEvent;
import com.ithinkrok.msm.server.event.player.PlayerCommandEvent;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.event.CustomEventHandler;
import com.ithinkrok.util.event.CustomListener;

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
        MinecraftServer server = event.getMSMServer().getMinecraftServer(serverName);

        if(server == null) {
            event.getCommandSender().sendMessage("Unknown minecraft server: " + serverName);
            return;
        }

        Connection connection = server.getConnection();

        if(connection == null) {
            event.getCommandSender().sendMessage("Minecraft server not connected: " + serverName);
            return;
        }

        Channel channel = connection.getChannel("MSMAPI");

        Config payload = new MemoryConfig();
        payload.set("mode", "ExecCommand");
        payload.set("command", event.getCommand().getRemainingArgsAndParamsAsString(1));

        Config sender = new MemoryConfig();

        if(event instanceof PlayerCommandEvent) {
            sender.set("type", "player");
            sender.set("uuid", ((PlayerCommandEvent) event).getPlayer().getUUID());
        } else if(event instanceof ConsoleCommandEvent) {
            sender.set("type", "msm_console");
        } else if(event instanceof MinecraftServerCommandEvent) {
            sender.set("type", "minecraft");
            sender.set("name", ((MinecraftServerCommandEvent) event).getMinecraftServer().getName());
        } else {
            sender.set("type", "unknown");
        }

        payload.set("sender", sender);

        channel.write(payload);
    }

    public static CommandInfo createCommandInfo() {
        Config config = new MemoryConfig();

        config.set("usage", "/<command> <server> <server command...>");
        config.set("description", "Execute a command on a minecraft server");
        config.set("permission", "msmserver.exec");

        return new CommandInfo("mexec", config, new ExecCommand());
    }
}
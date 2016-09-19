package com.ithinkrok.msm.server.command;

import com.ithinkrok.msm.server.event.command.MSMCommandEvent;
import com.ithinkrok.msm.server.impl.MSMServer;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.event.CustomEventHandler;
import com.ithinkrok.util.event.CustomListener;

/**
 * Created by paul on 18/02/16.
 */
public class StopCommand implements CustomListener {

    public static ServerCommandInfo createCommandInfo() {
        Config config = new MemoryConfig();
        config.set("usage", "/<command>");
        config.set("description", "Stops the MSM server");
        config.set("permission", "msmserver.stop");

        return new ServerCommandInfo("mstop", config, new StopCommand());
    }

    @CustomEventHandler
    public void onCommand(MSMCommandEvent event) {
        event.getCommandSender().sendMessage("Stopping server");
        ((MSMServer) event.getMSMServer()).stop();
    }
}

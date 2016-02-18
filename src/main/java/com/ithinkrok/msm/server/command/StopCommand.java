package com.ithinkrok.msm.server.command;

import com.ithinkrok.msm.server.event.MSMCommandEvent;
import com.ithinkrok.msm.server.impl.MSMServer;
import com.ithinkrok.util.event.CustomEventHandler;
import com.ithinkrok.util.event.CustomListener;

/**
 * Created by paul on 18/02/16.
 */
public class StopCommand implements CustomListener {

    @CustomEventHandler
    public void onCommand(MSMCommandEvent event) {
        event.getCommandSender().sendMessage("Stopping server");
        ((MSMServer) event.getMSMServer()).stop();
    }
}

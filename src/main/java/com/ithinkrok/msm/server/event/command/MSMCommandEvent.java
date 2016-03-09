package com.ithinkrok.msm.server.event.command;

import com.ithinkrok.msm.server.Server;
import com.ithinkrok.util.command.event.CustomCommandEvent;

/**
 * Created by paul on 18/02/16.
 */
public interface MSMCommandEvent extends CustomCommandEvent {

    Server getMSMServer();
}

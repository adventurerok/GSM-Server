package com.ithinkrok.msm.server;

import com.ithinkrok.msm.common.Channel;

/**
 * Created by paul on 04/02/16.
 */
public interface Connection {

    Channel getChannel(String protocol);

    Server getServer();
}

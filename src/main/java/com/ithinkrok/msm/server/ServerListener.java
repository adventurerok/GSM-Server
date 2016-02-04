package com.ithinkrok.msm.server;

import com.ithinkrok.msm.common.Packet;
import com.ithinkrok.msm.server.impl.MSMConnection;

/**
 * Created by paul on 02/02/16.
 */
public interface ServerListener {

    void connectionOpened(MSMConnection connection);
    void connectionClosed(MSMConnection connection);

    void packetRecieved(MSMConnection connection, Packet packet);
}

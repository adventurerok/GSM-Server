package com.ithinkrok.msm.server.event.minecraftserver;

import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.event.MSMEvent;

/**
 * Created by paul on 17/02/16.
 */
public class ClientConnectEvent extends MSMEvent {

    public ClientConnectEvent(Client<?> minecraftClient) {
        super(minecraftClient);
    }
}

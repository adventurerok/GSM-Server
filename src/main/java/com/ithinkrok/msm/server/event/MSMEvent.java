package com.ithinkrok.msm.server.event;

import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.util.event.CustomEvent;

/**
 * Created by paul on 07/02/16.
 */
public abstract class MSMEvent implements CustomEvent {

    private final Client<?> client;

    public MSMEvent(Client<?> client) {
        this.client = client;
    }

    public Client<?> getClient() {
        return client;
    }

    public Server getMSMServer() {
        return client.getConnectedTo();
    }
}

package com.ithinkrok.msm.server.web.api;

import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.web.ApiMethod;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import fi.iki.elonen.NanoHTTPD;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by paul on 12/03/16.
 */
public class ClientNamesMethod implements ApiMethod {

    @Override
    public Config call(NanoHTTPD.IHTTPSession session, Server server) {
        List<String> names = new ArrayList<>();

        for(Client<?> client : server.getClients()) {
            names.add(client.getName());
        }

        Config result = new MemoryConfig();

        result.set("names", names);

        return result;
    }
}

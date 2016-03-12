package com.ithinkrok.msm.server.web.api;

import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.web.ApiMethod;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import fi.iki.elonen.NanoHTTPD;

/**
 * Created by paul on 12/03/16.
 */
public class ClientResourceUsageMethod implements ApiMethod {
    @Override
    public Config call(NanoHTTPD.IHTTPSession session, Server server) {

        String clientName = session.getParms().get("client");

        if(clientName == null || server.getClient(clientName) == null) return null;

        Client<?> client = server.getClient(clientName);

        Config result = new MemoryConfig();

        result.set("client", clientName);
        result.set("ram_free", client.getMaxRam() - client.getRamUsage());
        result.set("ram_used", client.getRamUsage());
        result.set("ram_max", client.getMaxRam());
        result.set("ram_allocated", client.getAllocatedRam());
        result.set("performance", client.getPerformance());

        return result;
    }
}

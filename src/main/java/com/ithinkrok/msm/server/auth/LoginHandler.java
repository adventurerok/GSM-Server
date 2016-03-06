package com.ithinkrok.msm.server.auth;

import com.ithinkrok.msm.common.ClientInfo;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.util.config.Config;

/**
 * Created by paul on 06/03/16.
 */
public interface LoginHandler {

    ClientInfo loadClientInfo(Config config);

    Client<?> createClient(ClientInfo clientInfo, Server server);
}

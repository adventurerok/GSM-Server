package com.ithinkrok.msm.server.minecraft;

import com.ithinkrok.msm.common.ClientInfo;
import com.ithinkrok.msm.common.MinecraftClientInfo;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.auth.LoginHandler;
import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.impl.MSMServer;
import com.ithinkrok.msm.server.minecraft.impl.MSMMinecraftClient;
import com.ithinkrok.util.config.Config;

/**
 * Created by paul on 06/03/16.
 */
public class MinecraftLoginHandler implements LoginHandler {

    @Override
    public ClientInfo loadClientInfo(Config config) {
        return new MinecraftClientInfo(config);
    }

    @Override
    public Client<?> createClient(ClientInfo clientInfo, Server server) {
        return new MSMMinecraftClient((MinecraftClientInfo) clientInfo, (MSMServer) server);
    }
}

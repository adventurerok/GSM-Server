package com.ithinkrok.msm.server.listener;

import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.event.minecraftserver.ClientDisconnectEvent;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.event.CustomEventHandler;
import com.ithinkrok.util.event.CustomListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by paul on 17/09/16.
 */
public class ClientDownListener implements CustomListener {

    private static final Logger logger = LogManager.getLogger(ClientDownListener.class);


    private final String command;
    private final int timeDown;

    public ClientDownListener(Config config) {
        command = config.getString("command");
        timeDown = config.getInt("time", 180);
    }

    @CustomEventHandler
    public void onClientDisconnected(ClientDisconnectEvent event) {
        ClientDownScriptRunnable runnable = new ClientDownScriptRunnable(event.getMSMServer(), event.getClient()
                .getName());

        event.getMSMServer().scheduleAsync(runnable, timeDown, TimeUnit.SECONDS);
    }

    private class ClientDownScriptRunnable implements Runnable {

        private final Server server;
        private final String clientName;

        public ClientDownScriptRunnable(Server server, String clientName) {
            this.server = server;
            this.clientName = clientName;
        }

        @Override
        public void run() {
            Client<?> client = server.getClient(clientName);

            if(client != null && client.isConnected()) return;

            logger.info("Calling the client down script for " + clientName);

            ProcessBuilder processBuilder = new ProcessBuilder(command, clientName);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

            try {
                processBuilder.start();
            } catch (IOException e) {
                logger.warn("Failed to call the client down script for " + clientName, e);
            }

            //Reschedule us to make sure the server is up in five minutes
            server.scheduleAsync(this, timeDown, TimeUnit.SECONDS);
        }
    }
}

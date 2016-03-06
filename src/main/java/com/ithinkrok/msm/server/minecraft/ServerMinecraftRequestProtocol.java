package com.ithinkrok.msm.server.minecraft;

import com.ithinkrok.msm.common.Channel;
import com.ithinkrok.msm.server.Connection;
import com.ithinkrok.msm.server.Server;
import com.ithinkrok.msm.server.ServerListener;
import com.ithinkrok.util.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * Created by paul on 06/03/16.
 */
public class ServerMinecraftRequestProtocol implements ServerListener {

    private final Logger log = LogManager.getLogger(ServerMinecraftRequestProtocol.class);

    @Override
    public void connectionOpened(Connection connection, Channel channel) {

    }

    @Override
    public void connectionClosed(Connection connection) {

    }

    @Override
    public void packetRecieved(Connection connection, Channel channel, Config payload) {
        String mode = payload.getString("mode");

        if (mode == null) return;

        switch (mode) {
            case "ChangeServer":
                handleChangeServer(connection.getConnectedTo(), payload);
        }
    }

    private void handleChangeServer(Server connectedTo, Config payload) {
        UUID playerUUID = UUID.fromString(payload.getString("player"));
        MinecraftPlayer player = (MinecraftPlayer) connectedTo.getPlayer("minecraft", playerUUID);

        if (player == null) {
            log.debug("Unknown player " + playerUUID + " in ChangeServer request");
            return;
        }

        String targetName = payload.getString("target");
        MinecraftClient target = null;
        try {
            target = (MinecraftClient) connectedTo.getClient(targetName);
        } catch (Exception ignored) {
            //The server exists, but is not a minecraft server
        }

        if (target == null) {
            log.debug("Unknown minecraft server " + targetName + " in ChangeServer request");
            return;
        }

        player.changeServer(target);
    }
}

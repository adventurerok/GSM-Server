package com.ithinkrok.msm.server.minecraft;

import com.ithinkrok.msm.common.MinecraftClientInfo;
import com.ithinkrok.msm.common.MinecraftClientType;
import com.ithinkrok.msm.server.data.Client;

import java.util.List;

/**
 * Created by paul on 05/02/16.
 * <p>
 * Represents any kind of Minecraft Server (Vanilla/Bukkit/Spigot/Bungee etc...) that could be connected to this MSM
 * Server.
 */
public interface MinecraftClient extends Client<MinecraftPlayer> {

    /**
     * Gets an estimate of the average tps of the server over the last 60 seconds.
     *
     * @return An estimate of the average tps of the server over the last 60 seconds, within 0.2 of the actual value
     */
    double getTPS();

    MinecraftClientInfo getClientInfo();

    MinecraftClientType getSubType();

    boolean hasBungee();

    List<String> getPlugins();


}

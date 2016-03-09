package com.ithinkrok.msm.server.command;

import com.ithinkrok.msm.server.data.Client;
import com.ithinkrok.msm.server.event.command.MSMCommandEvent;
import com.ithinkrok.msm.server.minecraft.MinecraftClient;
import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.event.CustomEventHandler;
import com.ithinkrok.util.event.CustomListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by paul on 06/03/16.
 */
public class LoadCommand implements CustomListener {

    private final DecimalFormat formatter = new DecimalFormat("0.000");

    public static ServerCommandInfo createCommandInfo() {
        Config config = new MemoryConfig();

        config.set("usage", "/<command> [servers to include]");
        config.set("description", "Check the load");
        config.set("permission", "msmserver.load");

        return new ServerCommandInfo("mload", config, new LoadCommand());
    }

    @CustomEventHandler
    public void onCommand(MSMCommandEvent event) {
        event.setHandled(true);

        Collection<Client<?>> check = new ArrayList<>();

        if (event.getCommand().getArgumentCount() > 0) {
            for (String serverName : event.getCommand().getArgs()) {
                Client<?> client = event.getMSMServer().getClient(serverName);

                if (client == null) {
                    event.getCommandSender().sendMessage("Unknown server: " + serverName);
                }

                check.add(client);
            }
        } else {
            check.addAll(event.getMSMServer().getClients());
        }

        double totalUsedRam = 0;
        double totalFreeRam = 0;
        double totalAllocatedRam = 0;
        double totalMaxRam = 0;

        int tpsCount = 0;
        double totalTPS = 0;

        for (Client<?> client : check) {
            totalUsedRam += client.getRamUsage();
            totalFreeRam += (client.getMaxRam() - client.getRamUsage());
            totalAllocatedRam += client.getAllocatedRam();
            totalMaxRam += client.getMaxRam();

            if (!(client instanceof MinecraftClient)) continue;

            totalTPS += ((MinecraftClient) client).getTPS();
            ++tpsCount;
        }

        double averageUsedRam = totalUsedRam / check.size();
        double averageFreeRam = totalFreeRam / check.size();
        double averageAllocatedRam = totalAllocatedRam / check.size();
        double averageMaxRam = totalMaxRam / check.size();

        event.getCommandSender().sendMessage("Format: average    total    (servers counted)");
        String count = "    (" + check.size() + ")";

        event.getCommandSender().sendMessage(
                "used ram: " + formatter.format(averageUsedRam) + "    " + formatter.format(totalUsedRam) + count);

        event.getCommandSender().sendMessage(
                "free ram: " + formatter.format(averageFreeRam) + "    " + formatter.format(totalFreeRam) + count);

        event.getCommandSender().sendMessage("aloc ram: " + formatter.format(averageAllocatedRam) + "    " +
                formatter.format(totalAllocatedRam) + count);

        event.getCommandSender().sendMessage(
                "max  ram: " + formatter.format(averageMaxRam) + "    " + formatter.format(totalMaxRam) + count);

        if (tpsCount > 0) {
            double averageTPS = totalTPS / tpsCount;

            event.getCommandSender().sendMessage("tps: " + formatter.format(averageTPS) + "    " +
                    formatter.format(totalTPS) + "    " + "(" + tpsCount + ")");
        }

    }
}

package com.ithinkrok.msm.server.command;

import com.ithinkrok.util.event.CustomListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

/**
 * Created by paul on 07/02/16.
 */
public class MSMCommandInfo {

    private final String name;
    private final String usage;
    private final String description;
    private final String permission;
    private final CustomListener commandListener;

    public MSMCommandInfo(String name, String usage, String description, String permission,
                          CustomListener commandListener) {
        this.name = name;
        this.usage = usage;
        this.description = description;
        this.permission = permission;
        this.commandListener = commandListener;
    }

    public String getName() {
        return name;
    }

    public String getUsage() {
        return usage;
    }

    public String getDescription() {
        return description;
    }

    public String getPermission() {
        return permission;
    }

    public CustomListener getCommandListener() {
        return commandListener;
    }

    public ConfigurationSection toConfig() {
        ConfigurationSection result = new MemoryConfiguration();

        result.set("name", name);
        result.set("usage", usage);
        result.set("description", description);
        result.set("permission", permission);

        return result;
    }
}

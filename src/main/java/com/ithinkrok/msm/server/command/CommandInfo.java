package com.ithinkrok.msm.server.command;

import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.event.CustomListener;

/**
 * Created by paul on 07/02/16.
 */
public class CommandInfo {

    private final String name;
    private final String usage;
    private final String description;
    private final String permission;
    private CustomListener commandListener;

    public CommandInfo(String name, String usage, String description, String permission,
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

    public void setCommandListener(CustomListener commandListener) {
        this.commandListener = commandListener;
    }

    public CustomListener getCommandListener() {
        return commandListener;
    }

    public Config toConfig() {
        Config result = new MemoryConfig();

        result.set("name", name);
        result.set("usage", usage);
        result.set("description", description);
        result.set("permission", permission);

        return result;
    }
}

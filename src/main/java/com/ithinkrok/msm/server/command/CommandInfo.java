package com.ithinkrok.msm.server.command;

import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.event.CustomListener;

import java.util.List;

/**
 * Created by paul on 07/02/16.
 */
public class CommandInfo {

    private final String name;
    private final String usage;
    private final String description;
    private final String permission;
    private final List<String> aliases;
    private CustomListener commandListener;

    public CommandInfo(String name, Config config,
                       CustomListener commandListener) {
        this.name = name;
        this.usage = config.getString("usage");
        this.description = config.getString("description");
        this.permission = config.getString("permission");
        this.aliases = config.getStringList("aliases");
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
        result.set("aliases", aliases);

        return result;
    }
}

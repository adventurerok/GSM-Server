package com.ithinkrok.msm.server.command;

import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.ConfigSerializable;
import com.ithinkrok.util.config.MemoryConfig;
import com.ithinkrok.util.event.CustomListener;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by paul on 07/02/16.
 */
public class CommandInfo implements ConfigSerializable {

    private final String name;
    private final String usage;
    private final String description;
    private final String permission;
    private final List<String> aliases;
    private CustomListener commandListener;

    private final Map<String, List<String>> tabCompletion = new HashMap<>();

    public CommandInfo(String name, Config config,
                       CustomListener commandListener) {
        this.name = name;
        this.usage = config.getString("usage");
        this.description = config.getString("description");
        this.permission = config.getString("permission");
        this.aliases = config.getStringList("aliases");
        this.commandListener = commandListener;

        if(!config.contains("tab_complete")) return;

        for(Config tabConfig : config.getConfigList("tab_complete")) {
            String pattern = tabConfig.getString("pattern");

            List<String> values = tabConfig.getStringList("values");

            tabCompletion.put(pattern, values);
        }
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

    public List<String> getAliases() {
        return aliases;
    }

    public Config toConfig() {
        Config result = new MemoryConfig();

        result.set("name", name);
        result.set("usage", usage);
        result.set("description", description);
        result.set("permission", permission);
        result.set("aliases", aliases);

        List<Config> tabConfigs = new ArrayList<>();

        for(Map.Entry<String, List<String>> tabEntry : tabCompletion.entrySet()) {
            Config tabConfig = new MemoryConfig();

            tabConfig.set("pattern", tabEntry.getKey());
            tabConfig.set("values", tabEntry.getValue());

            tabConfigs.add(tabConfig);
        }

        result.set("tab_complete", tabConfigs);

        return result;
    }
}

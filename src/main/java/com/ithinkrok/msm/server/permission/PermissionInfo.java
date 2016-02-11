package com.ithinkrok.msm.server.permission;

import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.MemoryConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by paul on 11/02/16.
 */
public class PermissionInfo {

    private final String name;
    private final Map<String, Boolean> children = new LinkedHashMap<>();
    private final PermissionDefault defaultValue;
    private final String description;

    public PermissionInfo(String name, Config config) {
        this.name = name;

        description = config.getString("description");
        if(config.contains("default")) defaultValue = PermissionDefault.getByName(config.getString("default"));
        else defaultValue = PermissionDefault.OP;

        Config children = config.getConfigOrEmpty("children");

        for(String childName : children.getKeys(false)) {
            this.children.put(childName, children.getBoolean(childName));
        }
    }

    public String getName() {
        return name;
    }

    public Map<String, Boolean> getChildren() {
        return children;
    }

    public PermissionDefault getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public Config toConfig() {
        Config result = new MemoryConfig('/');

        result.set("name", name);
        result.set("description", description);
        result.set("default", defaultValue.toString());

        result.set("children", new MemoryConfig(children, '/'));

        return result;
    }
}

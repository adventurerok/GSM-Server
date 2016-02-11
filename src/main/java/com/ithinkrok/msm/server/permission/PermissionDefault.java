package com.ithinkrok.msm.server.permission;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by paul on 11/02/16.
 */
public enum PermissionDefault {
    TRUE("true"),
    FALSE("false"),
    OP("op"),
    NOT_OP("notop");

    private final String[] names;
    private static final Map<String, PermissionDefault> lookup = new HashMap<>();

    PermissionDefault(String... names) {
        this.names = names;
    }

    public static PermissionDefault getByName(String name) {
        return lookup.get(name.toLowerCase().replaceAll("[^a-z!]", ""));
    }

    static {
        for (PermissionDefault value : values()) {
            for (String name : value.names) {
                lookup.put(name, value);
            }
        }
    }

    @Override
    public String toString() {
        return names[0];
    }
}

package com.ithinkrok.msm.server.data;

import org.apache.commons.lang.Validate;

import java.util.UUID;

/**
 * Created by paul on 06/03/16.
 */
public class PlayerIdentifier {

    private final String serverType;
    private final UUID uuid;

    public PlayerIdentifier(String serverType, UUID uuid) {
        Validate.notNull(serverType, "serverType cannot be null");
        Validate.notNull(uuid, "uuid cannot be null");

        this.serverType = serverType;
        this.uuid = uuid;
    }

    public String getServerType() {
        return serverType;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayerIdentifier that = (PlayerIdentifier) o;

        if (!serverType.equals(that.serverType)) return false;
        return uuid.equals(that.uuid);

    }

    @Override
    public int hashCode() {
        int result = serverType.hashCode();
        result = 31 * result + uuid.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

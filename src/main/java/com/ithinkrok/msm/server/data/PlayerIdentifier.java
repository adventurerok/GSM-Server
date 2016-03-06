package com.ithinkrok.msm.server.data;

import org.apache.commons.lang.Validate;

import java.util.UUID;

/**
 * Created by paul on 06/03/16.
 */
public class PlayerIdentifier {

    private final String clientType;
    private final UUID uuid;

    public PlayerIdentifier(String clientType, UUID uuid) {
        Validate.notNull(clientType, "clientType cannot be null");
        Validate.notNull(uuid, "uuid cannot be null");

        this.clientType = clientType;
        this.uuid = uuid;
    }

    public String getClientType() {
        return clientType;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayerIdentifier that = (PlayerIdentifier) o;

        if (!clientType.equals(that.clientType)) return false;
        return uuid.equals(that.uuid);

    }

    @Override
    public int hashCode() {
        int result = clientType.hashCode();
        result = 31 * result + uuid.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

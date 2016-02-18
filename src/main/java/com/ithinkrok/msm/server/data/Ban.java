package com.ithinkrok.msm.server.data;

import com.ithinkrok.util.config.Config;
import com.ithinkrok.util.config.ConfigSerializable;
import com.ithinkrok.util.config.MemoryConfig;

import java.time.Instant;
import java.util.UUID;

/**
 * Created by paul on 18/02/16.
 */
public class Ban implements ConfigSerializable {

    private final UUID playerUUID;
    private final String playerName;

    private final String bannerName;

    private final String reason;

    private final Instant until;

    public Ban(UUID playerUUID, String playerName, String bannerName, String reason, Instant until) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.bannerName = bannerName;
        this.reason = reason;
        this.until = until;
    }

    public Ban(Config config) {
        this.playerUUID = UUID.fromString(config.getString("player"));
        this.playerName = config.getString("player_name", null);

        this.bannerName = config.getString("banner_name");
        this.reason = config.getString("reason");

        this.until = Instant.ofEpochMilli(config.getLong("until"));
    }

    public String getPlayerName() {
        return playerName;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getBannerName() {
        return bannerName;
    }

    public String getReason() {
        return reason;
    }

    public Instant getUntil() {
        return until;
    }

    @Override
    public Config toConfig() {
        Config result = new MemoryConfig();

        result.set("player", playerUUID.toString());
        result.set("player_name", playerName);
        result.set("banner_name", bannerName);
        result.set("reason", reason);
        result.set("until", until.toEpochMilli());

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ban ban = (Ban) o;

        if (!playerUUID.equals(ban.playerUUID)) return false;
        if (playerName != null ? !playerName.equals(ban.playerName) : ban.playerName != null) return false;
        if (bannerName != null ? !bannerName.equals(ban.bannerName) : ban.bannerName != null) return false;
        if (!reason.equals(ban.reason)) return false;
        return until.equals(ban.until);

    }

    @Override
    public int hashCode() {
        int result = playerUUID.hashCode();
        result = 31 * result + (playerName != null ? playerName.hashCode() : 0);
        result = 31 * result + (bannerName != null ? bannerName.hashCode() : 0);
        result = 31 * result + reason.hashCode();
        result = 31 * result + until.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "Ban{" +
                "playerUUID=" + playerUUID +
                ", playerName='" + playerName + '\'' +
                ", bannerName='" + bannerName + '\'' +
                ", reason='" + reason + '\'' +
                ", until=" + until +
                '}';
    }
}

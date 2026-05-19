package com.pvpindex.factions.data.model;

import com.github.ezframework.jaloquent.model.Model;
import java.util.Map;

/** Persistent record of a significant power change for a player. */
public class PowerHistoryModel extends Model {

    public static final String PREFIX = "power_history";

    public static final Map<String, String> COLUMNS = Map.of(
        "id", "VARCHAR(36) NOT NULL",
        "player_uuid", "VARCHAR(36) NOT NULL",
        "delta", "DOUBLE NOT NULL DEFAULT 0.0",
        "reason", "VARCHAR(32) NOT NULL",
        "power_after", "DOUBLE NOT NULL DEFAULT 0.0",
        "created_at", "BIGINT NOT NULL DEFAULT 0"
    );

    public PowerHistoryModel(final String id) {
        super(id);
    }

    public String getPlayerUuid() { return getAs("player_uuid", String.class, ""); }
    public void setPlayerUuid(final String playerUuid) { set("player_uuid", playerUuid); }

    public double getDelta() { return getAs("delta", Double.class, 0.0); }
    public void setDelta(final double delta) { set("delta", delta); }

    public String getReason() { return getAs("reason", String.class, ""); }
    public void setReason(final String reason) { set("reason", reason); }

    public double getPowerAfter() { return getAs("power_after", Double.class, 0.0); }
    public void setPowerAfter(final double powerAfter) { set("power_after", powerAfter); }

    public long getCreatedAt() { return getAs("created_at", Long.class, 0L); }
    public void setCreatedAt(final long createdAt) { set("created_at", createdAt); }
}

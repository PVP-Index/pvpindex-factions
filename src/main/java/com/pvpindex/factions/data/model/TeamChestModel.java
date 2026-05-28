package com.pvpindex.factions.data.model;

import com.github.ezframework.jaloquent.model.Model;
import java.util.Map;

/**
 * Persistent model for a faction team chest.
 */
public class TeamChestModel extends Model {

    public static final String PREFIX = "team_chests";

    public static final Map<String, String> COLUMNS = Map.ofEntries(
        Map.entry("id", "VARCHAR(36) NOT NULL"),
        Map.entry("faction_id", "VARCHAR(36) NOT NULL"),
        Map.entry("name", "VARCHAR(64) NOT NULL"),
        Map.entry("contents", "TEXT"),
        Map.entry("created_at", "BIGINT NOT NULL DEFAULT 0")
    );

    public TeamChestModel(final String id) {
        super(id);
        setCreatedAt(0L);
    }

    public String getFactionId() {
        return getAs("faction_id", String.class, null);
    }

    public void setFactionId(final String factionId) {
        set("faction_id", factionId);
    }

    public String getName() {
        return getAs("name", String.class, "");
    }

    public void setName(final String name) {
        set("name", name);
    }

    public String getContents() {
        return getAs("contents", String.class, null);
    }

    public void setContents(final String contents) {
        set("contents", contents);
    }

    public long getCreatedAt() {
        return getAs("created_at", Long.class, 0L);
    }

    public void setCreatedAt(final long createdAt) {
        set("created_at", createdAt);
    }
}

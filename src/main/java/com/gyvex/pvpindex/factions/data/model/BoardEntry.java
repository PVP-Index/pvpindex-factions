package com.gyvex.pvpindex.factions.data.model;

import com.github.ezframework.jaloquent.model.Model;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistent model for a claimed chunk.
 *
 * <p>{@code id} = {@code "world:chunkX:chunkZ"} (the chunk's unique key).
 */
public class BoardEntry extends Model {

    public static final String PREFIX = "board";

    public static final Map<String, String> COLUMNS = Map.of(
        "id", "VARCHAR(128) NOT NULL",
        "faction_id", "VARCHAR(36) NOT NULL",
        "access_json", "TEXT"
    );

    public BoardEntry(final String id) {
        super(id);
    }

    // -------------------------------------------------------------------------
    // ID construction / parsing
    // -------------------------------------------------------------------------

    /**
     * Build the storage ID from world name and chunk coordinates.
     *
     * @param worldName the Bukkit world name
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return composite id string
     */
    public static String buildId(final String worldName, final int chunkX, final int chunkZ) {
        return worldName + ":" + chunkX + ":" + chunkZ;
    }

    /** @return world name portion of this entry's id */
    public String getWorldName() {
        return getId().split(":")[0];
    }

    /** @return chunk X coordinate parsed from this entry's id */
    public int getChunkX() {
        return Integer.parseInt(getId().split(":")[1]);
    }

    /** @return chunk Z coordinate parsed from this entry's id */
    public int getChunkZ() {
        return Integer.parseInt(getId().split(":")[2]);
    }

    // -------------------------------------------------------------------------
    // Typed accessors
    // -------------------------------------------------------------------------

    /** UUID string of the faction that owns this chunk. */
    public String getFactionId() {
        return getAs("faction_id", String.class, null);
    }

    public void setFactionId(final String factionId) {
        set("faction_id", factionId);
    }

    /** Raw JSON for per-player / per-relation access overrides. */
    public String getAccessJson() {
        return getAs("access_json", String.class, "{}");
    }

    public void setAccessJson(final String json) {
        set("access_json", json);
    }

    // -------------------------------------------------------------------------
    // Table creation helper
    // -------------------------------------------------------------------------

    /**
     * Returns a column-definition map for the CREATE TABLE statement.
     * The id column includes the PRIMARY KEY constraint.
     */
    public static Map<String, String> createTableColumns() {
        final Map<String, String> cols = new LinkedHashMap<>(COLUMNS);
        cols.put("id", "VARCHAR(128) NOT NULL PRIMARY KEY");
        return cols;
    }
}

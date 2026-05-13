package com.pvpindex.factions.data.model;

import com.github.ezframework.jaloquent.model.Model;
import java.util.Map;

/**
 * Persistent model for a pending faction notification targeted at an offline player.
 *
 * <p>{@code id} = UUID string generated at notification time.
 * Entries are deleted when the player joins and the inbox is delivered.
 */
public class FactionInboxEntry extends Model {

    public static final String PREFIX = "faction_inbox";

    public static final Map<String, String> COLUMNS = Map.of(
        "id", "VARCHAR(36) NOT NULL",
        "player_id", "VARCHAR(36) NOT NULL",
        "message", "TEXT NOT NULL",
        "created_at", "BIGINT NOT NULL DEFAULT 0"
    );

    public FactionInboxEntry(final String id) {
        super(id);
    }

    // -------------------------------------------------------------------------
    // Typed accessors
    // -------------------------------------------------------------------------

    /** UUID string of the player who should receive this notification. */
    public String getPlayerId() {
        return getAs("player_id", String.class, null);
    }

    public void setPlayerId(final String playerId) {
        set("player_id", playerId);
    }

    /** MiniMessage-formatted notification text. */
    public String getMessage() {
        return getAs("message", String.class, "");
    }

    public void setMessage(final String message) {
        set("message", message);
    }

    public long getCreatedAt() {
        return getAs("created_at", Long.class, 0L);
    }

    public void setCreatedAt(final long createdAt) {
        set("created_at", createdAt);
    }
}

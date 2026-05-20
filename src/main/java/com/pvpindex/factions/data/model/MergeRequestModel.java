package com.pvpindex.factions.data.model;

import com.github.ezframework.jaloquent.model.Model;
import java.util.Map;

/**
 * Persistent model for a pending faction merge request.
 *
 * <p>{@code sender_faction_id} is the faction that wants to dissolve and have its
 * members absorbed into {@code target_faction_id}. The request must be explicitly
 * accepted or cancelled — there is no automatic TTL expiry.</p>
 */
public class MergeRequestModel extends Model {

    public static final String PREFIX = "merge_requests";

    public static final Map<String, String> COLUMNS = Map.of(
        "id", "VARCHAR(36) NOT NULL",
        "sender_faction_id", "VARCHAR(36) NOT NULL",
        "target_faction_id", "VARCHAR(36) NOT NULL",
        "actor_id", "VARCHAR(36) NOT NULL",
        "created_at", "BIGINT NOT NULL DEFAULT 0"
    );

    public MergeRequestModel(final String id) {
        super(id);
        setCreatedAt(0L);
    }

    // -------------------------------------------------------------------------
    // Typed accessors
    // -------------------------------------------------------------------------

    /** UUID string of the faction that is requesting to merge (will be disbanded). */
    public String getSenderFactionId() {
        return getAs("sender_faction_id", String.class, null);
    }

    public void setSenderFactionId(final String senderFactionId) {
        set("sender_faction_id", senderFactionId);
    }

    /** UUID string of the faction that will absorb the sender's members. */
    public String getTargetFactionId() {
        return getAs("target_faction_id", String.class, null);
    }

    public void setTargetFactionId(final String targetFactionId) {
        set("target_faction_id", targetFactionId);
    }

    /** UUID string of the officer who sent the merge request. */
    public String getActorId() {
        return getAs("actor_id", String.class, null);
    }

    public void setActorId(final String actorId) {
        set("actor_id", actorId);
    }

    public long getCreatedAt() {
        return getAs("created_at", Long.class, 0L);
    }

    public void setCreatedAt(final long createdAt) {
        set("created_at", createdAt);
    }
}

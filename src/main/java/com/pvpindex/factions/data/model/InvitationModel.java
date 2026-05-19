package com.pvpindex.factions.data.model;

import com.github.ezframework.jaloquent.model.Model;
import java.util.Map;

/**
 * Persistent model for a pending faction join invitation.
 *
 * <p>{@code id} = UUID string generated at invite time.
 * Invites expire after a configurable TTL; the engine deletes them on accept/decline/expiry.
 */
public class InvitationModel extends Model {

    public static final String PREFIX = "invitations";

    public static final Map<String, String> COLUMNS = Map.of(
        "id", "VARCHAR(36) NOT NULL",
        "faction_id", "VARCHAR(36) NOT NULL",
        "invitee_id", "VARCHAR(36) NOT NULL",
        "inviter_id", "VARCHAR(36) NOT NULL",
        "created_at", "BIGINT NOT NULL DEFAULT 0"
    );

    public InvitationModel(final String id) {
        super(id);
        // Ensure NOT NULL database columns always have explicit values on first save.
        setCreatedAt(0L);
    }

    // -------------------------------------------------------------------------
    // Typed accessors
    // -------------------------------------------------------------------------

    public String getFactionId() {
        return getAs("faction_id", String.class, null);
    }

    public void setFactionId(final String factionId) {
        set("faction_id", factionId);
    }

    /** UUID string of the player being invited. */
    public String getInviteeId() {
        return getAs("invitee_id", String.class, null);
    }

    public void setInviteeId(final String inviteeId) {
        set("invitee_id", inviteeId);
    }

    /** UUID string of the player who sent the invitation. */
    public String getInviterId() {
        return getAs("inviter_id", String.class, null);
    }

    public void setInviterId(final String inviterId) {
        set("inviter_id", inviterId);
    }

    public long getCreatedAt() {
        return getAs("created_at", Long.class, 0L);
    }

    public void setCreatedAt(final long createdAt) {
        set("created_at", createdAt);
    }
}

package com.pvpindex.factions.data.model;

import com.github.ezframework.jaloquent.model.Model;
import java.util.Map;

/** Persisted record of a significant faction action for audit and moderation purposes. */
public class AuditLogModel extends Model {

    public static final String PREFIX = "audit_logs";

    public static final Map<String, String> COLUMNS = Map.of(
        "id", "VARCHAR(36) NOT NULL",
        "faction_id", "VARCHAR(36) NOT NULL",
        "actor_uuid", "VARCHAR(36)",
        "action", "VARCHAR(64) NOT NULL",
        "detail", "TEXT",
        "created_at", "BIGINT NOT NULL DEFAULT 0"
    );

    public AuditLogModel(final String id) {
        super(id);
        setCreatedAt(0L);
    }

    public String getFactionId() { return getAs("faction_id", String.class, ""); }
    public void setFactionId(final String factionId) { set("faction_id", factionId); }

    public String getActorUuid() { return getAs("actor_uuid", String.class, null); }
    public void setActorUuid(final String actorUuid) { set("actor_uuid", actorUuid); }

    public String getAction() { return getAs("action", String.class, ""); }
    public void setAction(final String action) { set("action", action); }

    public String getDetail() { return getAs("detail", String.class, ""); }
    public void setDetail(final String detail) { set("detail", detail); }

    public long getCreatedAt() { return getAs("created_at", Long.class, 0L); }
    public void setCreatedAt(final long createdAt) { set("created_at", createdAt); }
}

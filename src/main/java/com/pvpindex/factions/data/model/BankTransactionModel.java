package com.pvpindex.factions.data.model;

import com.github.ezframework.jaloquent.model.Model;
import java.util.Map;

/** Persistent faction bank transaction record. */
public class BankTransactionModel extends Model {

    public static final String PREFIX = "bank_transactions";

    public static final Map<String, String> COLUMNS = Map.of(
        "id", "VARCHAR(36) NOT NULL",
        "faction_id", "VARCHAR(36) NOT NULL",
        "actor_uuid", "VARCHAR(36)",
        "type", "VARCHAR(32) NOT NULL",
        "amount", "DOUBLE NOT NULL DEFAULT 0.0",
        "counterparty_faction_id", "VARCHAR(36)",
        "created_at", "BIGINT NOT NULL DEFAULT 0",
        "note", "VARCHAR(255)"
    );

    public BankTransactionModel(final String id) {
        super(id);
        // Ensure NOT NULL database columns always have explicit values on first save.
        setAmount(0.0);
        setCreatedAt(0L);
    }

    public String getFactionId() { return getAs("faction_id", String.class, ""); }
    public void setFactionId(final String factionId) { set("faction_id", factionId); }

    public String getActorUuid() { return getAs("actor_uuid", String.class, null); }
    public void setActorUuid(final String actorUuid) { set("actor_uuid", actorUuid); }

    public String getType() { return getAs("type", String.class, ""); }
    public void setType(final String type) { set("type", type); }

    public double getAmount() { return getAs("amount", Double.class, 0.0); }
    public void setAmount(final double amount) { set("amount", amount); }

    public String getCounterpartyFactionId() { return getAs("counterparty_faction_id", String.class, null); }
    public void setCounterpartyFactionId(final String counterpartyFactionId) {
        set("counterparty_faction_id", counterpartyFactionId);
    }

    public long getCreatedAt() { return getAs("created_at", Long.class, 0L); }
    public void setCreatedAt(final long createdAt) { set("created_at", createdAt); }

    public String getNote() { return getAs("note", String.class, ""); }
    public void setNote(final String note) { set("note", note); }
}


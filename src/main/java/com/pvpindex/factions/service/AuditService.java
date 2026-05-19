package com.pvpindex.factions.service;

import com.pvpindex.factions.FactionAuditAction;
import java.util.UUID;

/** Records significant faction actions for staff and faction-leader review. */
public interface AuditService {

    /** No-operation instance used when audit recording is not needed. */
    AuditService NOOP = (factionId, actorUUID, action, detail) -> { };

    /**
     * Persist an audit log entry.
     *
     * @param factionId the faction the action was performed against or by
     * @param actorUUID the player who performed the action, or {@code null} for system actions
     * @param action    the type of action
     * @param detail    human-readable context (e.g. chunk coords, player name, amount)
     */
    void record(String factionId, UUID actorUUID, FactionAuditAction action, String detail);
}

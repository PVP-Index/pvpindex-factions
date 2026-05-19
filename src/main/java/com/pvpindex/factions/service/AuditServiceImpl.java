package com.pvpindex.factions.service;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.FactionAuditAction;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.AuditLogModel;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Persists audit log entries via {@link com.pvpindex.factions.data.repository.AuditLogRepository}. */
public final class AuditServiceImpl implements AuditService {

    private final Repositories repos;
    private final Logger logger;

    public AuditServiceImpl(final Repositories repos, final Logger logger) {
        this.repos = repos;
        this.logger = logger;
    }

    @Override
    public void record(
            final String factionId,
            final UUID actorUUID,
            final FactionAuditAction action,
            final String detail) {
        try {
            final AuditLogModel entry = new AuditLogModel(UUID.randomUUID().toString());
            entry.setFactionId(factionId);
            entry.setActorUuid(actorUUID != null ? actorUUID.toString() : null);
            entry.setAction(action.getId());
            entry.setDetail(detail != null ? detail : "");
            entry.setCreatedAt(System.currentTimeMillis());
            repos.auditLogs().save(entry);
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to record audit entry for faction " + factionId, e);
        }
    }
}

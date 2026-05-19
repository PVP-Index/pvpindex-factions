package com.pvpindex.factions.data.repository;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.model.TableRegistry;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.github.ezframework.javaquerybuilder.query.builder.QueryBuilder;
import com.pvpindex.factions.data.model.AuditLogModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Persists and queries faction audit log entries. */
public class AuditLogRepository extends ModelRepository<AuditLogModel> {

    public AuditLogRepository(final DataSourceJdbcStore store) {
        super(store, AuditLogModel.PREFIX, (id, data) -> new AuditLogModel(id));
        TableRegistry.register(AuditLogModel.PREFIX, "audit_logs", AuditLogModel.COLUMNS);
    }

    /**
     * Return up to {@code limit} entries for {@code factionId}, sorted newest-first,
     * skipping the first {@code offset} entries.
     */
    public List<AuditLogModel> findByFaction(
            final String factionId, final int limit, final int offset) throws StorageException {
        final List<AuditLogModel> rows = new ArrayList<>(
            query(new QueryBuilder().whereEquals("faction_id", factionId).build()));
        rows.sort(Comparator.comparingLong(AuditLogModel::getCreatedAt).reversed());
        if (offset >= rows.size()) {
            return List.of();
        }
        return rows.subList(offset, Math.min(rows.size(), offset + limit));
    }

    /**
     * Like {@link #findByFaction} but filtered to entries matching {@code action}.
     */
    public List<AuditLogModel> findByFactionAndAction(
            final String factionId, final String action,
            final int limit, final int offset) throws StorageException {
        final List<AuditLogModel> rows = new ArrayList<>(
            query(new QueryBuilder().whereEquals("faction_id", factionId).build()));
        rows.removeIf(r -> !action.equalsIgnoreCase(r.getAction()));
        rows.sort(Comparator.comparingLong(AuditLogModel::getCreatedAt).reversed());
        if (offset >= rows.size()) {
            return List.of();
        }
        return rows.subList(offset, Math.min(rows.size(), offset + limit));
    }
}

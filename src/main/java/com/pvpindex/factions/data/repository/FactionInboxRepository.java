package com.pvpindex.factions.data.repository;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.model.TableRegistry;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.github.ezframework.javaquerybuilder.query.builder.QueryBuilder;
import com.pvpindex.factions.data.model.FactionInboxEntry;
import java.util.List;

/**
 * Repository for {@link FactionInboxEntry} instances.
 */
public class FactionInboxRepository extends ModelRepository<FactionInboxEntry> {

    public FactionInboxRepository(final DataSourceJdbcStore store) {
        super(store, FactionInboxEntry.PREFIX, (id, data) -> new FactionInboxEntry(id));
        TableRegistry.register(FactionInboxEntry.PREFIX, "faction_inbox", FactionInboxEntry.COLUMNS);
    }

    /**
     * Find all pending inbox entries for the given player.
     *
     * @param playerId player UUID string
     * @return list of entries, ordered by insertion order
     * @throws StorageException on database error
     */
    public List<FactionInboxEntry> findByPlayerId(final String playerId) throws StorageException {
        return query(new QueryBuilder().whereEquals("player_id", playerId).build());
    }

    /**
     * Delete all inbox entries for the given player.
     *
     * @param playerId player UUID string
     * @throws StorageException on database error
     */
    public void deleteByPlayerId(final String playerId) throws StorageException {
        deleteWhere("player_id", playerId);
    }
}

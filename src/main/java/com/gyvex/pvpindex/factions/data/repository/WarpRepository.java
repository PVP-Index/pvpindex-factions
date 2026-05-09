package com.gyvex.pvpindex.factions.data.repository;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.model.TableRegistry;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.github.ezframework.javaquerybuilder.query.builder.QueryBuilder;
import com.gyvex.pvpindex.factions.data.model.WarpModel;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link WarpModel} instances.
 */
public class WarpRepository extends ModelRepository<WarpModel> {

    public WarpRepository(final DataSourceJdbcStore store) {
        super(store, WarpModel.PREFIX, (id, data) -> new WarpModel(id));
        TableRegistry.register(WarpModel.PREFIX, "warps", WarpModel.COLUMNS);
    }

    /**
     * Find all warps for a faction.
     *
     * @param factionId faction UUID string
     * @return list of warps
     * @throws StorageException on database error
     */
    public List<WarpModel> findByFactionId(final String factionId) throws StorageException {
        return query(new QueryBuilder().whereEquals("faction_id", factionId).build());
    }

    /**
     * Find a warp by faction and name (case-sensitive).
     *
     * @param factionId faction UUID string
     * @param name warp name
     * @return matching warp or empty
     * @throws StorageException on database error
     */
    public Optional<WarpModel> findByFactionIdAndName(
            final String factionId, final String name) throws StorageException {
        final List<WarpModel> candidates = findByFactionId(factionId);
        return candidates.stream()
            .filter(w -> w.getName().equalsIgnoreCase(name))
            .findFirst();
    }

    /**
     * Delete all warps belonging to a faction.
     *
     * @param factionId faction UUID string
     * @throws StorageException on database error
     */
    public void deleteByFactionId(final String factionId) throws StorageException {
        deleteWhere("faction_id", factionId);
    }
}

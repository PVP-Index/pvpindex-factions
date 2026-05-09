package com.gyvex.pvpindex.factions.data.repository;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.model.TableRegistry;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.github.ezframework.javaquerybuilder.query.builder.QueryBuilder;
import com.gyvex.pvpindex.factions.data.model.RankModel;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link RankModel} instances.
 */
public class RankRepository extends ModelRepository<RankModel> {

    public RankRepository(final DataSourceJdbcStore store) {
        super(store, RankModel.PREFIX, (id, data) -> new RankModel(id));
        TableRegistry.register(RankModel.PREFIX, "ranks", RankModel.COLUMNS);
    }

    /**
     * Find all ranks for a given faction, ordered by priority descending.
     *
     * @param factionId faction UUID string
     * @return list of ranks (may be empty)
     * @throws StorageException on database error
     */
    public List<RankModel> findByFactionId(final String factionId) throws StorageException {
        final List<RankModel> ranks = query(
            new QueryBuilder().whereEquals("faction_id", factionId).build()
        );
        ranks.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        return ranks;
    }

    /**
     * Find the default member rank for a faction (lowest priority built-in rank).
     *
     * @param factionId faction UUID string
     * @return the default rank, or empty if none found
     * @throws StorageException on database error
     */
    public Optional<RankModel> findDefaultRank(final String factionId) throws StorageException {
        return findByFactionId(factionId).stream()
            .filter(r -> RankModel.RANK_MEMBER.equals(r.getName()))
            .findFirst();
    }

    /**
     * Find the owner rank for a faction.
     *
     * @param factionId faction UUID string
     * @return the owner rank, or empty
     * @throws StorageException on database error
     */
    public Optional<RankModel> findOwnerRank(final String factionId) throws StorageException {
        return findByFactionId(factionId).stream()
            .filter(RankModel::isOwner)
            .findFirst();
    }

    /**
     * Delete all ranks belonging to a faction.
     *
     * @param factionId faction UUID string
     * @throws StorageException on database error
     */
    public void deleteByFactionId(final String factionId) throws StorageException {
        deleteWhere("faction_id", factionId);
    }
}

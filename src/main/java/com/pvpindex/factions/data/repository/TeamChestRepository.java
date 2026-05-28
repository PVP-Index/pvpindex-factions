package com.pvpindex.factions.data.repository;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.model.TableRegistry;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.github.ezframework.javaquerybuilder.query.builder.QueryBuilder;
import com.pvpindex.factions.data.model.TeamChestModel;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link TeamChestModel} instances.
 */
public class TeamChestRepository extends ModelRepository<TeamChestModel> {

    public TeamChestRepository(final DataSourceJdbcStore store) {
        super(store, TeamChestModel.PREFIX, (id, data) -> new TeamChestModel(id));
        TableRegistry.register(TeamChestModel.PREFIX, "team_chests", TeamChestModel.COLUMNS);
    }

    public List<TeamChestModel> findByFactionId(final String factionId) throws StorageException {
        return query(new QueryBuilder().whereEquals("faction_id", factionId).build());
    }

    public Optional<TeamChestModel> findByFactionIdAndName(
            final String factionId, final String name) throws StorageException {
        return findByFactionId(factionId).stream()
            .filter(chest -> chest.getName().equalsIgnoreCase(name))
            .findFirst();
    }

    public void deleteByFactionId(final String factionId) throws StorageException {
        deleteWhere("faction_id", factionId);
    }
}

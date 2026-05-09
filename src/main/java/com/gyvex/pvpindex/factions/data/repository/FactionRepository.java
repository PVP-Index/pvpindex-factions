package com.gyvex.pvpindex.factions.data.repository;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.model.TableRegistry;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.github.ezframework.javaquerybuilder.query.builder.QueryBuilder;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link FactionModel} instances.
 */
public class FactionRepository extends ModelRepository<FactionModel> {

    public FactionRepository(final DataSourceJdbcStore store) {
        super(store, FactionModel.PREFIX, (id, data) -> new FactionModel(id));
        TableRegistry.register(FactionModel.PREFIX, "factions", FactionModel.COLUMNS);
    }

    /**
     * Find all factions.
     *
     * @return list of all factions (never null, may be empty)
     * @throws StorageException on database error
     */
    public List<FactionModel> findAll() throws StorageException {
        return query(new QueryBuilder().build());
    }

    /**
     * Find a faction by its case-insensitive name.
     *
     * @param name faction name to search
     * @return first matching faction or empty
     * @throws StorageException on database error
     */
    public Optional<FactionModel> findByName(final String name) throws StorageException {
        final List<FactionModel> results = query(
            new QueryBuilder().whereEquals("name", name).build()
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Count the total number of factions in the database.
     *
     * @return faction count
     * @throws StorageException on database error
     */
    public int countAll() throws StorageException {
        return findAll().size();
    }
}

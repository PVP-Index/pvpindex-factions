package com.pvpindex.factions.data.repository;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.model.TableRegistry;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.github.ezframework.javaquerybuilder.query.builder.QueryBuilder;
import com.pvpindex.factions.data.model.MergeRequestModel;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link MergeRequestModel} instances.
 */
public class MergeRequestRepository extends ModelRepository<MergeRequestModel> {

    public MergeRequestRepository(final DataSourceJdbcStore store) {
        super(store, MergeRequestModel.PREFIX, (id, data) -> new MergeRequestModel(id));
        TableRegistry.register(MergeRequestModel.PREFIX, "merge_requests", MergeRequestModel.COLUMNS);
    }

    /**
     * Find all pending merge requests sent by a faction.
     *
     * @param senderFactionId faction UUID string
     * @return list of merge requests
     * @throws StorageException on database error
     */
    public List<MergeRequestModel> findBySenderFactionId(final String senderFactionId)
            throws StorageException {
        return query(new QueryBuilder().whereEquals("sender_faction_id", senderFactionId).build());
    }

    /**
     * Find all pending merge requests targeting a faction.
     *
     * @param targetFactionId faction UUID string
     * @return list of merge requests
     * @throws StorageException on database error
     */
    public List<MergeRequestModel> findByTargetFactionId(final String targetFactionId)
            throws StorageException {
        return query(new QueryBuilder().whereEquals("target_faction_id", targetFactionId).build());
    }

    /**
     * Find a specific merge request by sender and target faction.
     *
     * @param senderFactionId sender faction UUID string
     * @param targetFactionId target faction UUID string
     * @return matching request, or empty if none
     * @throws StorageException on database error
     */
    public Optional<MergeRequestModel> findBySenderAndTarget(
            final String senderFactionId, final String targetFactionId) throws StorageException {
        return findBySenderFactionId(senderFactionId).stream()
            .filter(r -> targetFactionId.equals(r.getTargetFactionId()))
            .findFirst();
    }

    /**
     * Delete all merge requests where the given faction is the sender.
     *
     * @param senderFactionId faction UUID string
     * @throws StorageException on database error
     */
    public void deleteBySenderFactionId(final String senderFactionId) throws StorageException {
        deleteWhere("sender_faction_id", senderFactionId);
    }

    /**
     * Delete all merge requests targeting the given faction.
     *
     * @param targetFactionId faction UUID string
     * @throws StorageException on database error
     */
    public void deleteByTargetFactionId(final String targetFactionId) throws StorageException {
        deleteWhere("target_faction_id", targetFactionId);
    }
}

package com.pvpindex.factions.data.repository;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.model.TableRegistry;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.github.ezframework.javaquerybuilder.query.builder.QueryBuilder;
import com.pvpindex.factions.data.model.InvitationModel;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link InvitationModel} instances.
 */
public class InvitationRepository extends ModelRepository<InvitationModel> {

    public InvitationRepository(final DataSourceJdbcStore store) {
        super(store, InvitationModel.PREFIX, (id, data) -> new InvitationModel(id));
        TableRegistry.register(InvitationModel.PREFIX, "invitations", InvitationModel.COLUMNS);
    }

    /**
     * Find all pending invitations for a given faction.
     *
     * @param factionId faction UUID string
     * @return list of invitations
     * @throws StorageException on database error
     */
    public List<InvitationModel> findByFactionId(final String factionId) throws StorageException {
        return query(new QueryBuilder().whereEquals("faction_id", factionId).build());
    }

    /**
     * Find all pending invitations for a given invitee (player being invited).
     *
     * @param inviteeId player UUID string
     * @return list of invitations
     * @throws StorageException on database error
     */
    public List<InvitationModel> findByInviteeId(final String inviteeId) throws StorageException {
        return query(new QueryBuilder().whereEquals("invitee_id", inviteeId).build());
    }

    /**
     * Find the specific pending invitation for a player to join a faction.
     *
     * @param factionId faction UUID string
     * @param inviteeId player UUID string
     * @return the invitation, or empty if none
     * @throws StorageException on database error
     */
    public Optional<InvitationModel> findByFactionAndInvitee(
            final String factionId, final String inviteeId) throws StorageException {
        return findByInviteeId(inviteeId).stream()
            .filter(i -> factionId.equals(i.getFactionId()))
            .findFirst();
    }

    /**
     * Delete all invitations for a faction (e.g. when disbanded).
     *
     * @param factionId faction UUID string
     * @throws StorageException on database error
     */
    public void deleteByFactionId(final String factionId) throws StorageException {
        deleteWhere("faction_id", factionId);
    }

    /**
     * Delete all invitations addressed to a player (e.g. when they join a faction).
     *
     * @param inviteeId player UUID string
     * @throws StorageException on database error
     */
    public void deleteByInviteeId(final String inviteeId) throws StorageException {
        deleteWhere("invitee_id", inviteeId);
    }
}

package com.pvpindex.factions.service;

import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.InvitationModel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal invite service interface — no TeamsAPI dependency.
 */
public interface InviteService {

    /**
     * Record an invite for {@code inviteeUUID} to the faction identified by
     * {@code factionId}, sent by {@code inviterUUID}.
     *
     * @return {@code true} if the invite was stored successfully.
     */
    boolean sendInvite(String factionId, UUID inviterUUID, UUID inviteeUUID);

    /**
     * Accept a pending invite for {@code playerUUID} to {@code factionId}.
     *
     * @return the faction the player just joined, or empty if no invite
     *         exists or an error occurred.
     */
    Optional<FactionModel> acceptInvite(String factionId, UUID playerUUID);

    /**
     * List pending invites addressed to a player.
     */
    List<InvitationModel> listInvitesForPlayer(UUID playerUUID);

    /**
     * List non-expired invites for a player, pruning expired rows first.
     */
    List<InvitationModel> listActiveInvitesForPlayer(UUID playerUUID);

    /**
     * List pending invites created by a faction.
     */
    List<InvitationModel> listInvitesForFaction(String factionId);

    /**
     * Revoke a faction->player invite.
     */
    boolean revokeInvite(String factionId, UUID inviteeUUID);

    /**
     * Decline a faction->player invite as the invitee.
     */
    boolean declineInvite(String factionId, UUID inviteeUUID);

    /**
     * Decline all invites addressed to a player.
     *
     * @return number of invites removed
     */
    int declineAllInvites(UUID inviteeUUID);

    /**
     * Remove expired invites for a specific player.
     *
     * @return number of rows removed
     */
    int pruneExpiredInvitesForPlayer(UUID playerUUID);

    /**
     * Remove expired invites globally.
     *
     * @return number of rows removed
     */
    int pruneAllExpiredInvites();
}

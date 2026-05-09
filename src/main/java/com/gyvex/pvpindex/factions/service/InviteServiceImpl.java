package com.gyvex.pvpindex.factions.service;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.gyvex.pvpindex.factions.config.FactionsConfig;
import com.gyvex.pvpindex.factions.data.Repositories;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.data.model.InvitationModel;
import com.gyvex.pvpindex.factions.data.model.RankModel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core invite business logic — no TeamsAPI dependency.
 *
 * <p>Accepts or declines invites and delegates member addition to
 * {@link FactionServiceImpl#joinFaction}.
 */
public class InviteServiceImpl implements InviteService {

    private final FactionServiceImpl factionService;
    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;

    public InviteServiceImpl(
            final FactionServiceImpl factionService,
            final Repositories repos,
            final FactionsConfig config,
            final Logger logger) {
        this.factionService = factionService;
        this.repos = repos;
        this.config = config;
        this.logger = logger;
    }

    public FactionServiceImpl getFactionService() {
        return factionService;
    }

    // -------------------------------------------------------------------------
    // InviteService implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean sendInvite(
            final String factionId, final UUID inviterUUID, final UUID inviteeUUID) {
        try {
            pruneExpiredInvitesForPlayer(inviteeUUID);
            final Optional<FactionModel> faction = repos.factions().find(factionId);
            if (faction.isEmpty()) {
                return false;
            }

            if (repos.invitations().findByFactionAndInvitee(
                    factionId, inviteeUUID.toString()).isPresent()) {
                return false;
            }

            final InvitationModel invite = new InvitationModel(UUID.randomUUID().toString());
            invite.setFactionId(factionId);
            invite.setInviterId(inviterUUID.toString());
            invite.setInviteeId(inviteeUUID.toString());
            invite.setCreatedAt(System.currentTimeMillis());
            repos.invitations().save(invite);
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE,
                "Failed to invite " + inviteeUUID + " to faction " + factionId, e);
            return false;
        }
    }

    @Override
    public Optional<FactionModel> acceptInvite(
            final String factionId, final UUID playerUUID) {
        try {
            pruneExpiredInvitesForPlayer(playerUUID);
            final Optional<InvitationModel> invite = repos.invitations()
                .findByFactionAndInvitee(factionId, playerUUID.toString());
            if (invite.isEmpty()) {
                return Optional.empty();
            }

            final Optional<FactionModel> faction = repos.factions().find(factionId);
            if (faction.isEmpty()) {
                repos.invitations().delete(invite.get().getId());
                return Optional.empty();
            }

            final Optional<RankModel> defaultRank = repos.ranks().findDefaultRank(factionId);
            if (defaultRank.isEmpty()) {
                return Optional.empty();
            }

            // Delete invite, then let joinFaction handle its own transaction
            repos.invitations().delete(invite.get().getId());
            final boolean joined = factionService.joinFaction(factionId, playerUUID);
            if (!joined) {
                return Optional.empty();
            }

            return repos.factions().find(factionId);
        } catch (StorageException e) {
            logger.log(Level.SEVERE,
                "Failed to accept invite for " + playerUUID + " to faction " + factionId, e);
            return Optional.empty();
        }
    }

    @Override
    public List<InvitationModel> listInvitesForPlayer(final UUID playerUUID) {
        try {
            return repos.invitations().findByInviteeId(playerUUID.toString());
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to list invites for player " + playerUUID, e);
            return List.of();
        }
    }

    @Override
    public List<InvitationModel> listActiveInvitesForPlayer(final UUID playerUUID) {
        try {
            pruneExpiredInvitesForPlayer(playerUUID);
            return repos.invitations().findByInviteeId(playerUUID.toString()).stream()
                .filter(this::isActive)
                .collect(Collectors.toList());
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to list active invites for player " + playerUUID, e);
            return List.of();
        }
    }

    @Override
    public List<InvitationModel> listInvitesForFaction(final String factionId) {
        try {
            return repos.invitations().findByFactionId(factionId).stream()
                .filter(this::isActive)
                .collect(Collectors.toList());
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to list invites for faction " + factionId, e);
            return List.of();
        }
    }

    @Override
    public boolean revokeInvite(final String factionId, final UUID inviteeUUID) {
        try {
            pruneExpiredInvitesForPlayer(inviteeUUID);
            final Optional<InvitationModel> invite = repos.invitations()
                .findByFactionAndInvitee(factionId, inviteeUUID.toString());
            if (invite.isEmpty()) {
                return false;
            }
            repos.invitations().delete(invite.get().getId());
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to revoke invite for faction " + factionId, e);
            return false;
        }
    }

    @Override
    public boolean declineInvite(final String factionId, final UUID inviteeUUID) {
        return revokeInvite(factionId, inviteeUUID);
    }

    @Override
    public int declineAllInvites(final UUID inviteeUUID) {
        try {
            pruneExpiredInvitesForPlayer(inviteeUUID);
            final List<InvitationModel> invites = repos.invitations().findByInviteeId(inviteeUUID.toString());
            repos.invitations().deleteByInviteeId(inviteeUUID.toString());
            return invites.size();
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to decline all invites for " + inviteeUUID, e);
            return 0;
        }
    }

    @Override
    public int pruneExpiredInvitesForPlayer(final UUID playerUUID) {
        try {
            final List<InvitationModel> invites = repos.invitations().findByInviteeId(playerUUID.toString());
            int removed = 0;
            for (final InvitationModel invite : invites) {
                if (!isActive(invite)) {
                    repos.invitations().delete(invite.getId());
                    removed++;
                }
            }
            return removed;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to prune expired invites for player " + playerUUID, e);
            return 0;
        }
    }

    @Override
    public int pruneAllExpiredInvites() {
        try {
            final List<FactionModel> factions = repos.factions().findAll();
            int removed = 0;
            for (final FactionModel faction : factions) {
                final List<InvitationModel> invites = repos.invitations().findByFactionId(faction.getId());
                for (final InvitationModel invite : invites) {
                    if (!isActive(invite)) {
                        repos.invitations().delete(invite.getId());
                        removed++;
                    }
                }
            }
            return removed;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to prune expired invites globally", e);
            return 0;
        }
    }

    private boolean isActive(final InvitationModel invite) {
        final long ttlHours = Math.max(1, config.getInviteTtlHours());
        final long expiryMs = invite.getCreatedAt() + (ttlHours * 60L * 60L * 1000L);
        return System.currentTimeMillis() <= expiryMs;
    }
}

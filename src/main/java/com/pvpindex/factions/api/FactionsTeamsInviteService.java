package com.pvpindex.factions.api;

import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionServiceImpl;
import com.pvpindex.factions.service.InviteServiceImpl;
import com.skyblockexp.teamsapi.event.TeamInviteEvent;
import com.skyblockexp.teamsapi.event.TeamJoinEvent;
import com.skyblockexp.teamsapi.model.Team;
import com.skyblockexp.teamsapi.model.TeamRole;
import com.skyblockexp.teamsapi.api.TeamsInviteService;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;

/**
 * Adapts the internal {@link InviteServiceImpl} to the TeamsAPI
 * {@link TeamsInviteService} interface.
 *
 * <p>This class is only instantiated when TeamsAPI is present on the server.
 */
public class FactionsTeamsInviteService implements TeamsInviteService {

    private final InviteServiceImpl impl;
    private final FactionServiceImpl factionImpl;

    public FactionsTeamsInviteService(final InviteServiceImpl impl) {
        this.impl = impl;
        this.factionImpl = impl.getFactionService();
    }

    @Override
    public boolean invitePlayer(
            final UUID teamId, final UUID inviterUUID, final UUID inviteeUUID) {
        // Check faction exists before firing the event
        final Optional<FactionModel> faction = factionImpl.getFactionById(teamId.toString());
        if (faction.isEmpty()) {
            return false;
        }
        final FactionTeam team = wrap(faction.get());
        final TeamInviteEvent event = new TeamInviteEvent(team, inviterUUID, inviteeUUID);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        return impl.sendInvite(teamId.toString(), inviterUUID, inviteeUUID);
    }

    @Override
    public Optional<Team> acceptInvite(final UUID teamId, final UUID playerUUID) {
        // impl fires FactionJoinEvent
        final Optional<FactionModel> joined = impl.acceptInvite(teamId.toString(), playerUUID);
        if (joined.isEmpty()) {
            return Optional.empty();
        }
        final FactionTeam team = wrap(joined.get());
        Bukkit.getPluginManager().callEvent(new TeamJoinEvent(team, playerUUID, TeamRole.MEMBER));
        return Optional.of(team);
    }

    @Override
    public boolean declineInvite(final UUID teamId, final UUID playerUUID) {
        // InviteServiceImpl does not expose declineInvite; access repos via factionImpl
        try {
            final Optional<com.pvpindex.factions.data.model.InvitationModel> invite =
                factionImpl.getRepos().invitations()
                    .findByFactionAndInvitee(teamId.toString(), playerUUID.toString());
            if (invite.isEmpty()) {
                return false;
            }
            factionImpl.getRepos().invitations().delete(invite.get().getId());
            return true;
        } catch (com.github.ezframework.jaloquent.exception.StorageException e) {
            factionImpl.getLogger().log(
                java.util.logging.Level.SEVERE,
                "Failed to decline invite for " + playerUUID, e);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private FactionTeam wrap(final FactionModel faction) {
        return new FactionTeam(
            faction,
            factionImpl.getRepos(),
            factionImpl.getConfig(),
            factionImpl.getLogger());
    }
}

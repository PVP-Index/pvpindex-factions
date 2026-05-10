package com.pvpindex.factions.api;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.service.FactionServiceImpl;
import com.skyblockexp.teamsapi.api.TeamsClaimService;
import com.skyblockexp.teamsapi.event.TeamClaimEvent;
import com.skyblockexp.teamsapi.event.TeamUnclaimEvent;
import com.skyblockexp.teamsapi.model.TeamClaim;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;

/**
 * Adapts the internal factions board storage to the TeamsAPI
 * {@link TeamsClaimService} interface.
 *
 * <p>Max-claims computation mirrors the logic in {@code EngineChunkChange}:
 * {@code totalPower / landPerPower}, capped at the configured max-land ceiling.
 * If {@code landPerPower} is zero or negative, {@code getTeamMaxClaims} returns
 * the configured {@code maxLand} value directly.</p>
 *
 * <p>This class is only instantiated when TeamsAPI is present on the server.</p>
 */
public class FactionsTeamsClaimService implements TeamsClaimService {

    /** Repositories for data access. */
    private final Repositories repos;

    /** Reference to the faction service for team lookups and wrapping. */
    private final FactionServiceImpl factionImpl;

    /** Server configuration providing land and power limits. */
    private final FactionsConfig config;

    /** Logger for error reporting. */
    private final Logger logger;

    /**
     * Creates a new {@link FactionsTeamsClaimService}.
     *
     * @param factionImpl the internal faction service; must not be {@code null}
     */
    public FactionsTeamsClaimService(final FactionServiceImpl factionImpl) {
        this.factionImpl = factionImpl;
        this.repos = factionImpl.getRepos();
        this.config = factionImpl.getConfig();
        this.logger = factionImpl.getLogger();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Fires {@link TeamClaimEvent} before persisting. Returns {@code false} if
     * the chunk is already claimed or the event is cancelled.</p>
     */
    @Override
    public boolean claimChunk(
            final UUID teamId,
            final UUID playerUUID,
            final String worldName,
            final int chunkX,
            final int chunkZ) {
        try {
            if (isClaimed(worldName, chunkX, chunkZ)) {
                return false;
            }
            final Optional<FactionModel> faction = factionImpl.getFactionById(teamId.toString());
            if (faction.isEmpty()) {
                return false;
            }
            final FactionTeam team = wrap(faction.get());
            final TeamClaimEvent event = new TeamClaimEvent(team, playerUUID, worldName, chunkX, chunkZ);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            repos.board().claimChunk(worldName, chunkX, chunkZ, teamId.toString());
            return true;
        }
        catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to claim chunk for team " + teamId, e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Fires {@link TeamUnclaimEvent} before removing. Returns {@code false} if
     * no claim exists for the given team or the event is cancelled.</p>
     */
    @Override
    public boolean unclaimChunk(
            final UUID teamId,
            final UUID playerUUID,
            final String worldName,
            final int chunkX,
            final int chunkZ) {
        try {
            if (!isClaimedBy(teamId, worldName, chunkX, chunkZ)) {
                return false;
            }
            final Optional<FactionModel> faction = factionImpl.getFactionById(teamId.toString());
            if (faction.isEmpty()) {
                return false;
            }
            final FactionTeam team = wrap(faction.get());
            final TeamUnclaimEvent event = new TeamUnclaimEvent(team, playerUUID, worldName, chunkX, chunkZ);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            repos.board().unclaimChunk(worldName, chunkX, chunkZ);
            return true;
        }
        catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to unclaim chunk for team " + teamId, e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Removes all claims for the team in a single batch operation without firing
     * per-chunk unclaim events.</p>
     */
    @Override
    public boolean unclaimAll(final UUID teamId) {
        try {
            final int count = repos.board().countByFactionId(teamId.toString());
            if (count == 0) {
                return false;
            }
            repos.board().deleteByFactionId(teamId.toString());
            return true;
        }
        catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to unclaim all chunks for team " + teamId, e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<TeamClaim> getClaimAt(
            final String worldName, final int chunkX, final int chunkZ) {
        try {
            return repos.board().findByChunk(worldName, chunkX, chunkZ)
                .map(FactionTeamClaim::new);
        }
        catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to look up claim at " + worldName + ":" + chunkX + ":" + chunkZ, e);
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<TeamClaim> getTeamClaims(final UUID teamId) {
        try {
            final List<BoardEntry> entries = repos.board().findByFactionId(teamId.toString());
            final List<TeamClaim> result = new ArrayList<>(entries.size());
            for (final BoardEntry entry : entries) {
                result.add(new FactionTeamClaim(entry));
            }
            return result;
        }
        catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to fetch claims for team " + teamId, e);
            return new ArrayList<>();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getClaimCount(final UUID teamId) {
        try {
            return repos.board().countByFactionId(teamId.toString());
        }
        catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to count claims for team " + teamId, e);
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClaimed(final String worldName, final int chunkX, final int chunkZ) {
        try {
            return repos.board().findByChunk(worldName, chunkX, chunkZ).isPresent();
        }
        catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to check claim at " + worldName + ":" + chunkX + ":" + chunkZ, e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClaimedBy(
            final UUID teamId, final String worldName, final int chunkX, final int chunkZ) {
        try {
            final Optional<BoardEntry> entry = repos.board().findByChunk(worldName, chunkX, chunkZ);
            return entry.isPresent() && teamId.toString().equals(entry.get().getFactionId());
        }
        catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to check ownership of " + worldName + ":" + chunkX + ":" + chunkZ, e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Max claims = {@code floor(totalPower / landPerPower)}, capped at
     * {@code config.getMaxLand()}. If {@code landPerPower} is zero or negative,
     * returns {@code config.getMaxLand()} directly. Returns {@code 0} if the team
     * is unknown.</p>
     */
    @Override
    public int getTeamMaxClaims(final UUID teamId) {
        try {
            final Optional<FactionModel> faction = factionImpl.getFactionById(teamId.toString());
            if (faction.isEmpty()) {
                return 0;
            }
            double totalPower = faction.get().getPowerBoost();
            for (final PlayerModel pm : repos.players().findByFactionId(teamId.toString())) {
                totalPower += pm.getPower();
            }
            final double landPerPower = config.getLandPerPower();
            if (landPerPower <= 0) {
                return config.getMaxLand();
            }
            return Math.min(config.getMaxLand(), (int) (totalPower / landPerPower));
        }
        catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to compute max claims for team " + teamId, e);
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private FactionTeam wrap(final FactionModel faction) {
        return new FactionTeam(faction, repos, config, logger);
    }
}

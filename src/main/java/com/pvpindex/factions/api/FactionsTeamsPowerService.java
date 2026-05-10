package com.pvpindex.factions.api;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.service.FactionServiceImpl;
import com.skyblockexp.teamsapi.api.TeamsPowerService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapts the internal factions power system to the TeamsAPI
 * {@link TeamsPowerService} interface.
 *
 * <p>Team power is the sum of all member power values plus the faction-level power
 * boost, mirroring {@code EnginePower#computeTotalPower}. Team max power is
 * {@code config.getMaxPower() * memberCount}.</p>
 *
 * <p>This class is only instantiated when TeamsAPI is present on the server.</p>
 */
public class FactionsTeamsPowerService implements TeamsPowerService {

    /** Repositories for data access. */
    private final Repositories repos;

    /** Reference to the faction service for team lookups. */
    private final FactionServiceImpl factionImpl;

    /** Server configuration providing power limits. */
    private final FactionsConfig config;

    /** Logger for error reporting. */
    private final Logger logger;

    /**
     * Creates a new {@link FactionsTeamsPowerService}.
     *
     * @param factionImpl the internal faction service; must not be {@code null}
     */
    public FactionsTeamsPowerService(final FactionServiceImpl factionImpl) {
        this.factionImpl = factionImpl;
        this.repos = factionImpl.getRepos();
        this.config = factionImpl.getConfig();
        this.logger = factionImpl.getLogger();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getPlayerPower(final UUID playerUUID) {
        try {
            final Optional<PlayerModel> pm = repos.players().find(playerUUID.toString());
            return pm.map(PlayerModel::getPower).orElse(0.0);
        }
        catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to get power for player " + playerUUID, e);
            return 0.0;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the server-wide max power limit from configuration.</p>
     */
    @Override
    public double getPlayerMaxPower(final UUID playerUUID) {
        try {
            final Optional<PlayerModel> pm = repos.players().find(playerUUID.toString());
            if (pm.isEmpty()) {
                return 0.0;
            }
            return config.getMaxPower();
        }
        catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to get max power for player " + playerUUID, e);
            return 0.0;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The stored value is clamped to {@code [0, config.getMaxPower()]} before
     * being persisted.</p>
     */
    @Override
    public boolean setPlayerPower(final UUID playerUUID, final double power) {
        try {
            final Optional<PlayerModel> pm = repos.players().find(playerUUID.toString());
            if (pm.isEmpty()) {
                return false;
            }
            final PlayerModel model = pm.get();
            final double clamped = Math.max(0.0, Math.min(config.getMaxPower(), power));
            model.setPower(clamped);
            repos.players().save(model);
            return true;
        }
        catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to set power for player " + playerUUID, e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Total team power = faction power boost + sum of all member power values,
     * matching the formula used by {@code EnginePower#computeTotalPower}.</p>
     */
    @Override
    public double getTeamPower(final UUID teamId) {
        try {
            final Optional<FactionModel> faction = factionImpl.getFactionById(teamId.toString());
            if (faction.isEmpty()) {
                return 0.0;
            }
            double total = faction.get().getPowerBoost();
            for (final PlayerModel pm : repos.players().findByFactionId(teamId.toString())) {
                total += pm.getPower();
            }
            return total;
        }
        catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to compute team power for " + teamId, e);
            return 0.0;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code config.getMaxPower() * memberCount}. Returns {@code 0.0}
     * if the team does not exist.</p>
     */
    @Override
    public double getTeamMaxPower(final UUID teamId) {
        try {
            final List<PlayerModel> members = repos.players().findByFactionId(teamId.toString());
            if (members.isEmpty()) {
                final Optional<FactionModel> faction = factionImpl.getFactionById(teamId.toString());
                if (faction.isEmpty()) {
                    return 0.0;
                }
            }
            return config.getMaxPower() * members.size();
        }
        catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to compute max team power for " + teamId, e);
            return 0.0;
        }
    }
}

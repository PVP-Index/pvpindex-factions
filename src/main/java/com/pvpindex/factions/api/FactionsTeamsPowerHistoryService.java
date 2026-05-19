package com.pvpindex.factions.api;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.PowerHistoryModel;
import com.pvpindex.factions.service.FactionServiceImpl;
import com.skyblockexp.teamsapi.api.TeamsPowerHistoryService;
import com.skyblockexp.teamsapi.model.TeamPowerHistoryEntry;
import com.skyblockexp.teamsapi.model.TeamPowerHistoryType;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapts the internal power-history repository to the TeamsAPI
 * {@link TeamsPowerHistoryService} interface.
 *
 * <p>Only player-scoped queries are backed by real data; team-scoped queries
 * return all member history combined. Mutation methods honour the interface
 * contract on a best-effort basis: {@code updatePowerHistoryEntry} is not
 * supported and always returns {@code false}.</p>
 *
 * <p>This class is only instantiated when TeamsAPI 1.8+ is present on the
 * server.</p>
 */
public class FactionsTeamsPowerHistoryService implements TeamsPowerHistoryService {

    /** Data access. */
    private final Repositories repos;

    /** Used to resolve team membership for team-scoped history. */
    private final FactionServiceImpl factionImpl;

    /** Logger for error reporting. */
    private final Logger logger;

    /**
     * Creates a new {@link FactionsTeamsPowerHistoryService}.
     *
     * @param factionImpl the internal faction service; must not be {@code null}
     */
    public FactionsTeamsPowerHistoryService(final FactionServiceImpl factionImpl) {
        this.factionImpl = factionImpl;
        this.repos = factionImpl.getRepos();
        this.logger = factionImpl.getLogger();
    }

    // -------------------------------------------------------------------------
    // Read — player
    // -------------------------------------------------------------------------

    @Override
    public Collection<TeamPowerHistoryEntry> getPlayerPowerHistory(
            final UUID playerUUID, final int limit) {
        try {
            final int effective = limit <= 0 ? 50 : limit;
            final List<PowerHistoryModel> rows =
                    repos.powerHistory().findRecentByPlayerUuid(
                            playerUUID.toString(), effective, 0);
            return toEntries(rows);
        } catch (StorageException e) {
            logger.log(Level.SEVERE,
                    "Failed to retrieve power history for player " + playerUUID, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Collection<TeamPowerHistoryEntry> getPlayerPowerHistory(
            final UUID playerUUID,
            final Instant fromInclusive,
            final Instant toExclusive,
            final int limit) {
        try {
            final int effective = limit <= 0 ? 50 : limit;
            final List<PowerHistoryModel> all =
                    repos.powerHistory().findAllByPlayerUuid(playerUUID.toString());
            return all.stream()
                    .filter(m -> {
                        final Instant ts = Instant.ofEpochMilli(m.getCreatedAt());
                        return !ts.isBefore(fromInclusive) && ts.isBefore(toExclusive);
                    })
                    .limit(effective)
                    .map(FactionsTeamsPowerHistoryService::toEntry)
                    .collect(java.util.stream.Collectors.toUnmodifiableList());
        } catch (StorageException e) {
            logger.log(Level.SEVERE,
                    "Failed to retrieve power history for player " + playerUUID, e);
            return Collections.emptyList();
        }
    }

    // -------------------------------------------------------------------------
    // Read — team
    // -------------------------------------------------------------------------

    @Override
    public Collection<TeamPowerHistoryEntry> getTeamPowerHistory(
            final UUID teamId, final int limit) {
        try {
            final int effective = limit <= 0 ? 50 : limit;
            return repos.players().findByFactionId(teamId.toString()).stream()
                    .flatMap(member -> {
                        try {
                            return repos.powerHistory()
                                    .findAllByPlayerUuid(member.getId()).stream();
                        } catch (StorageException e) {
                            logger.log(Level.WARNING,
                                    "Failed to load history for member " + member.getId(), e);
                            return java.util.stream.Stream.empty();
                        }
                    })
                    .sorted(java.util.Comparator
                            .comparingLong(PowerHistoryModel::getCreatedAt).reversed())
                    .limit(effective)
                    .map(FactionsTeamsPowerHistoryService::toEntry)
                    .collect(java.util.stream.Collectors.toUnmodifiableList());
        } catch (StorageException e) {
            logger.log(Level.SEVERE,
                    "Failed to retrieve power history for team " + teamId, e);
            return Collections.emptyList();
        }
    }

    // -------------------------------------------------------------------------
    // Mutate
    // -------------------------------------------------------------------------

    @Override
    public boolean addPowerHistoryEntry(
            final UUID entryId,
            final UUID playerUUID,
            final UUID teamId,
            final double delta,
            final TeamPowerHistoryType type,
            final String reason,
            final UUID actorUUID,
            final Instant occurredAt,
            final String details) {
        try {
            return repos.powerHistory().insert(
                    entryId,
                    playerUUID.toString(),
                    delta,
                    reason.toUpperCase(Locale.ROOT),
                    occurredAt.toEpochMilli());
        } catch (StorageException e) {
            logger.log(Level.SEVERE,
                    "Failed to add power history entry " + entryId, e);
            return false;
        }
    }

    @Override
    public boolean updatePowerHistoryEntry(
            final UUID entryId,
            final double delta,
            final TeamPowerHistoryType type,
            final String reason,
            final UUID actorUUID,
            final Instant occurredAt,
            final String details) {
        // In-place updates are not supported by the current storage schema.
        return false;
    }

    @Override
    public boolean removePowerHistoryEntry(final UUID entryId) {
        try {
            return repos.powerHistory().deleteById(entryId.toString());
        } catch (StorageException e) {
            logger.log(Level.SEVERE,
                    "Failed to remove power history entry " + entryId, e);
            return false;
        }
    }

    @Override
    public int clearPlayerPowerHistory(final UUID playerUUID) {
        try {
            final List<PowerHistoryModel> all =
                    repos.powerHistory().findAllByPlayerUuid(playerUUID.toString());
            for (final PowerHistoryModel m : all) {
                repos.powerHistory().deleteById(m.getId());
            }
            return all.size();
        } catch (StorageException e) {
            logger.log(Level.SEVERE,
                    "Failed to clear power history for player " + playerUUID, e);
            return 0;
        }
    }

    @Override
    public int clearTeamPowerHistory(final UUID teamId) {
        try {
            int removed = 0;
            for (final var member : repos.players().findByFactionId(teamId.toString())) {
                removed += clearPlayerPowerHistory(UUID.fromString(member.getId()));
            }
            return removed;
        } catch (StorageException e) {
            logger.log(Level.SEVERE,
                    "Failed to clear power history for team " + teamId, e);
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static List<TeamPowerHistoryEntry> toEntries(final List<PowerHistoryModel> rows) {
        return rows.stream()
                .map(FactionsTeamsPowerHistoryService::toEntry)
                .collect(java.util.stream.Collectors.toUnmodifiableList());
    }

    private static TeamPowerHistoryEntry toEntry(final PowerHistoryModel m) {
        return new TeamPowerHistoryEntry() {
            @Override
            public UUID getEntryId() {
                return UUID.fromString(m.getId());
            }

            @Override
            public UUID getPlayerUUID() {
                return UUID.fromString(m.getPlayerUuid());
            }

            @Override
            public Optional<UUID> getTeamId() {
                return Optional.empty();
            }

            @Override
            public double getDelta() {
                return m.getDelta();
            }

            @Override
            public TeamPowerHistoryType getType() {
                return m.getDelta() >= 0
                        ? TeamPowerHistoryType.GAIN
                        : TeamPowerHistoryType.LOSS;
            }

            @Override
            public String getReason() {
                return m.getReason().toLowerCase(Locale.ROOT);
            }

            @Override
            public Optional<UUID> getActorUUID() {
                return Optional.empty();
            }

            @Override
            public Instant getOccurredAt() {
                return Instant.ofEpochMilli(m.getCreatedAt());
            }

            @Override
            public Optional<String> getDetails() {
                return Optional.empty();
            }
        };
    }
}

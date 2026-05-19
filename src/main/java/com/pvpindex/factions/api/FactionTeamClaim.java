package com.pvpindex.factions.api;

import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.skyblockexp.teamsapi.model.ClaimTerritoryType;
import com.skyblockexp.teamsapi.model.TeamClaim;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapts a {@link BoardEntry} to the TeamsAPI {@link TeamClaim} interface.
 *
 * <p>The factions storage layer does not record a claim timestamp or the UUID of
 * the claiming player, so {@link #getClaimedAt()} returns {@link Instant#EPOCH}
 * as documented in {@link TeamClaim#getClaimedAt()}.</p>
 */
public final class FactionTeamClaim implements TeamClaim {

    /** The underlying storage entry for the claimed chunk. */
    private final BoardEntry entry;

    /**
     * Creates a new {@link FactionTeamClaim} wrapping the given {@link BoardEntry}.
     *
     * @param entry the storage entry; must not be {@code null}
     */
    public FactionTeamClaim(final BoardEntry entry) {
        this.entry = entry;
    }

    /**
     * {@inheritDoc}
     *
     * <p>For SafeZone and WarZone entries the factions storage uses the sentinel
     * strings {@code "SAFEZONE"} / {@code "WARZONE"} rather than a UUID.
     * This method converts those to deterministic name-based UUIDs for API
     * compatibility. Callers that need to distinguish special territories should
     * use {@link #getOwningTeamId()} instead.</p>
     */
    @Override
    public UUID getTeamId() {
        final String factionId = entry.getFactionId();
        if (FactionModel.SAFEZONE_ID.equals(factionId)
                || FactionModel.WARZONE_ID.equals(factionId)) {
            return UUID.nameUUIDFromBytes(factionId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return UUID.fromString(factionId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Maps the internal sentinel IDs {@link FactionModel#SAFEZONE_ID} and
     * {@link FactionModel#WARZONE_ID} to the corresponding
     * {@link ClaimTerritoryType} constants. All other entries are classified as
     * {@link ClaimTerritoryType#TEAM}.</p>
     */
    @Override
    public ClaimTerritoryType getTerritoryType() {
        final String factionId = entry.getFactionId();
        if (FactionModel.SAFEZONE_ID.equals(factionId)) {
            return ClaimTerritoryType.SAFE_ZONE;
        }
        if (FactionModel.WARZONE_ID.equals(factionId)) {
            return ClaimTerritoryType.WAR_ZONE;
        }
        return ClaimTerritoryType.TEAM;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link Optional#empty()} for SafeZone and WarZone chunks because
     * special territories are server-admin owned, not player-team owned.</p>
     */
    @Override
    public Optional<UUID> getOwningTeamId() {
        final ClaimTerritoryType type = getTerritoryType();
        if (type == ClaimTerritoryType.TEAM) {
            return Optional.of(getTeamId());
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWorldName() {
        return entry.getWorldName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getChunkX() {
        return entry.getChunkX();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getChunkZ() {
        return entry.getChunkZ();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Always returns {@link Instant#EPOCH} because the factions storage layer
     * does not persist a claim timestamp.</p>
     */
    @Override
    public Instant getClaimedAt() {
        return Instant.EPOCH;
    }
}

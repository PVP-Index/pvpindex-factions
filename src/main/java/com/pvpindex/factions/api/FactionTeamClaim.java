package com.pvpindex.factions.api;

import com.pvpindex.factions.data.model.BoardEntry;
import com.skyblockexp.teamsapi.model.TeamClaim;
import java.time.Instant;
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
     */
    @Override
    public UUID getTeamId() {
        return UUID.fromString(entry.getFactionId());
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

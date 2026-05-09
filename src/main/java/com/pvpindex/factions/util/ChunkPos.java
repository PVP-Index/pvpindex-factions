package com.pvpindex.factions.util;

import java.util.Objects;

/**
 * Value object representing a chunk coordinate pair (world + chunkX + chunkZ).
 */
public final class ChunkPos {

    private final String world;
    private final int x;
    private final int z;

    public ChunkPos(final String world, final int x, final int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    /** Convert to the board-entry ID string used by {@link com.pvpindex.factions.data.model.BoardEntry}. */
    public String toEntryId() {
        return world + ":" + x + ":" + z;
    }

    /** Parse a board-entry ID back into a {@link ChunkPos}. */
    public static ChunkPos fromEntryId(final String id) {
        final String[] parts = id.split(":", 3);
        return new ChunkPos(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ChunkPos other)) return false;
        return x == other.x && z == other.z && Objects.equals(world, other.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, z);
    }

    @Override
    public String toString() {
        return toEntryId();
    }
}

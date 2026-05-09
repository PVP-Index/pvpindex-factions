package com.pvpindex.factions.data.model;

import com.github.ezframework.jaloquent.model.Model;
import java.util.Map;

/**
 * Persistent model for a faction named warp point.
 *
 * <p>{@code id} = UUID string generated at warp creation time.
 */
public class WarpModel extends Model {

    public static final String PREFIX = "warps";

    public static final Map<String, String> COLUMNS = Map.ofEntries(
        Map.entry("id", "VARCHAR(36) NOT NULL"),
        Map.entry("faction_id", "VARCHAR(36) NOT NULL"),
        Map.entry("name", "VARCHAR(64) NOT NULL"),
        Map.entry("world", "VARCHAR(64) NOT NULL"),
        Map.entry("x", "DOUBLE NOT NULL DEFAULT 0.0"),
        Map.entry("y", "DOUBLE NOT NULL DEFAULT 64.0"),
        Map.entry("z", "DOUBLE NOT NULL DEFAULT 0.0"),
        Map.entry("yaw", "FLOAT NOT NULL DEFAULT 0.0"),
        Map.entry("pitch", "FLOAT NOT NULL DEFAULT 0.0"),
        Map.entry("creator_id", "VARCHAR(36)"),
        Map.entry("created_at", "BIGINT NOT NULL DEFAULT 0")
    );

    public WarpModel(final String id) {
        super(id);
    }

    // -------------------------------------------------------------------------
    // Typed accessors
    // -------------------------------------------------------------------------

    public String getFactionId() {
        return getAs("faction_id", String.class, null);
    }

    public void setFactionId(final String factionId) {
        set("faction_id", factionId);
    }

    public String getName() {
        return getAs("name", String.class, "");
    }

    public void setName(final String name) {
        set("name", name);
    }

    public String getWorld() {
        return getAs("world", String.class, null);
    }

    public void setWorld(final String world) {
        set("world", world);
    }

    public double getX() {
        return getAs("x", Double.class, 0.0);
    }

    public void setX(final double x) {
        set("x", x);
    }

    public double getY() {
        return getAs("y", Double.class, 64.0);
    }

    public void setY(final double y) {
        set("y", y);
    }

    public double getZ() {
        return getAs("z", Double.class, 0.0);
    }

    public void setZ(final double z) {
        set("z", z);
    }

    public float getYaw() {
        return getAs("yaw", Float.class, 0.0f);
    }

    public void setYaw(final float yaw) {
        set("yaw", yaw);
    }

    public float getPitch() {
        return getAs("pitch", Float.class, 0.0f);
    }

    public void setPitch(final float pitch) {
        set("pitch", pitch);
    }

    /** UUID string of the player who created this warp. */
    public String getCreatorId() {
        return getAs("creator_id", String.class, null);
    }

    public void setCreatorId(final String creatorId) {
        set("creator_id", creatorId);
    }

    public long getCreatedAt() {
        return getAs("created_at", Long.class, 0L);
    }

    public void setCreatedAt(final long createdAt) {
        set("created_at", createdAt);
    }

    /**
     * Reconstruct a Bukkit {@link org.bukkit.Location} from the stored world name and
     * coordinates. Returns {@code null} if the world is not loaded.
     */
    public org.bukkit.Location toLocation() {
        final String worldName = getWorld();
        if (worldName == null) {
            return null;
        }
        final org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new org.bukkit.Location(world, getX(), getY(), getZ(), getYaw(), getPitch());
    }
}

package com.gyvex.pvpindex.factions.data.model;

import com.github.ezframework.jaloquent.model.Model;
import java.util.Map;

/**
 * Persistent model for a Faction.
 *
 * <p>The {@code id} is a UUID string. All persistence is delegated to
 * Jaloquent's {@link Model} base class via the {@code set}/{@code getAs}
 * attribute API; no fields are stored on the subclass itself.
 */
public class FactionModel extends Model {

    /** Repository prefix — used as the {@link com.github.ezframework.jaloquent.model.TableRegistry} key. */
    public static final String PREFIX = "factions";

    /**
     * Column definitions for {@link com.github.ezframework.jaloquent.model.TableRegistry}.
     * The types use H2/MySQL compatible syntax.
     */
    public static final Map<String, String> COLUMNS = Map.ofEntries(
        Map.entry("id", "VARCHAR(36) NOT NULL"),
        Map.entry("name", "VARCHAR(64) NOT NULL"),
        Map.entry("owner_id", "VARCHAR(36)"),
        Map.entry("description", "TEXT"),
        Map.entry("motd", "TEXT"),
        Map.entry("created_at", "BIGINT NOT NULL DEFAULT 0"),
        Map.entry("power_boost", "DOUBLE NOT NULL DEFAULT 0.0"),
        Map.entry("money", "DOUBLE NOT NULL DEFAULT 0.0"),
        Map.entry("flags_json", "TEXT"),
        Map.entry("perms_json", "TEXT"),
        Map.entry("relations_json", "TEXT"),
        Map.entry("home_world", "VARCHAR(64)"),
        Map.entry("home_x", "DOUBLE"),
        Map.entry("home_y", "DOUBLE"),
        Map.entry("home_z", "DOUBLE"),
        Map.entry("home_yaw", "FLOAT"),
        Map.entry("home_pitch", "FLOAT")
    );

    /** Special sentinel ID for the "wilderness" (no faction). */
    public static final String WILDERNESS_ID = "WILDERNESS";

    /** Special sentinel ID for the safe zone pseudo-faction. */
    public static final String SAFEZONE_ID = "SAFEZONE";

    /** Special sentinel ID for the war zone pseudo-faction. */
    public static final String WARZONE_ID = "WARZONE";

    public FactionModel(final String id) {
        super(id);
    }

    // -------------------------------------------------------------------------
    // Typed accessors
    // -------------------------------------------------------------------------

    public String getName() {
        return getAs("name", String.class, "");
    }

    public void setName(final String name) {
        set("name", name);
    }

    /** UUID string of the faction owner (the player with the highest rank). */
    public String getOwnerId() {
        return getAs("owner_id", String.class, null);
    }

    public void setOwnerId(final String ownerId) {
        set("owner_id", ownerId);
    }

    /** @return {@code true} if {@code playerUUID} is the owner of this faction. */
    public boolean isOwner(final java.util.UUID playerUUID) {
        return playerUUID != null && playerUUID.toString().equals(getOwnerId());
    }

    public String getDescription() {
        return getAs("description", String.class, "");
    }

    public void setDescription(final String description) {
        set("description", description);
    }

    public String getMotd() {
        return getAs("motd", String.class, "");
    }

    public void setMotd(final String motd) {
        set("motd", motd);
    }

    public long getCreatedAt() {
        return getAs("created_at", Long.class, 0L);
    }

    public void setCreatedAt(final long createdAt) {
        set("created_at", createdAt);
    }

    public double getPowerBoost() {
        return getAs("power_boost", Double.class, 0.0);
    }

    public void setPowerBoost(final double powerBoost) {
        set("power_boost", powerBoost);
    }

    public double getMoney() {
        return getAs("money", Double.class, 0.0);
    }

    public void setMoney(final double money) {
        set("money", money);
    }

    /** Alias for {@link #getMoney()} — faction bank balance. */
    public double getBank() {
        return getMoney();
    }

    /** Alias for {@link #setMoney(double)} — faction bank balance. */
    public void setBank(final double bank) {
        setMoney(bank);
    }

    /** Raw JSON string for the flags map. Parsed by the service layer. */
    public String getFlagsJson() {
        return getAs("flags_json", String.class, "{}");
    }

    public void setFlagsJson(final String json) {
        set("flags_json", json);
    }

    /** Raw JSON string for the permissions map. Parsed by the service layer. */
    public String getPermsJson() {
        return getAs("perms_json", String.class, "{}");
    }

    public void setPermsJson(final String json) {
        set("perms_json", json);
    }

    /** Raw JSON string for the relations map ({@code factionId → Relation}). */
    public String getRelationsJson() {
        return getAs("relations_json", String.class, "{}");
    }

    public void setRelationsJson(final String json) {
        set("relations_json", json);
    }

    // Home location

    public String getHomeWorld() {
        return getAs("home_world", String.class, null);
    }

    public void setHomeWorld(final String world) {
        set("home_world", world);
    }

    public double getHomeX() {
        return getAs("home_x", Double.class, 0.0);
    }

    public void setHomeX(final double x) {
        set("home_x", x);
    }

    public double getHomeY() {
        return getAs("home_y", Double.class, 64.0);
    }

    public void setHomeY(final double y) {
        set("home_y", y);
    }

    public double getHomeZ() {
        return getAs("home_z", Double.class, 0.0);
    }

    public void setHomeZ(final double z) {
        set("home_z", z);
    }

    public float getHomeYaw() {
        return getAs("home_yaw", Float.class, 0.0f);
    }

    public void setHomeYaw(final float yaw) {
        set("home_yaw", yaw);
    }

    public float getHomePitch() {
        return getAs("home_pitch", Float.class, 0.0f);
    }

    public void setHomePitch(final float pitch) {
        set("home_pitch", pitch);
    }

    public boolean hasHome() {
        return getHomeWorld() != null;
    }

    // -------------------------------------------------------------------------
    // Identity helpers
    // -------------------------------------------------------------------------

    public boolean isWilderness() {
        return WILDERNESS_ID.equals(getId());
    }

    public boolean isSafeZone() {
        return SAFEZONE_ID.equals(getId());
    }

    public boolean isWarZone() {
        return WARZONE_ID.equals(getId());
    }

    public boolean isNormal() {
        return !isWilderness() && !isSafeZone() && !isWarZone();
    }
}

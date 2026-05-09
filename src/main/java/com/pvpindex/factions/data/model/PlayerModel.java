package com.pvpindex.factions.data.model;

import com.github.ezframework.jaloquent.model.Model;
import java.util.Map;

/**
 * Persistent model for a player's faction membership.
 *
 * <p>{@code id} = player UUID string.
 */
public class PlayerModel extends Model {

    public static final String PREFIX = "players";

    public static final Map<String, String> COLUMNS = Map.ofEntries(
        Map.entry("id", "VARCHAR(36) NOT NULL"),
        Map.entry("faction_id", "VARCHAR(36)"),
        Map.entry("rank_id", "VARCHAR(36)"),
        Map.entry("title", "VARCHAR(64)"),
        Map.entry("power_boost", "DOUBLE NOT NULL DEFAULT 0.0"),
        Map.entry("power", "DOUBLE NOT NULL DEFAULT 0.0"),
        Map.entry("joined_at", "BIGINT NOT NULL DEFAULT 0"),
        Map.entry("last_activity", "BIGINT NOT NULL DEFAULT 0"),
        Map.entry("overriding", "TINYINT NOT NULL DEFAULT 0"),
        Map.entry("territory_titles", "TINYINT NOT NULL DEFAULT 1"),
        Map.entry("auto_territory_mode", "TINYINT NOT NULL DEFAULT 0"),
        Map.entry("notify_invites", "TINYINT NOT NULL DEFAULT 1"),
        Map.entry("notify_bank_tax", "TINYINT NOT NULL DEFAULT 1")
    );

    public PlayerModel(final String id) {
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

    public boolean isInFaction() {
        final String factionId = getFactionId();
        return factionId != null && !factionId.isEmpty();
    }

    /** UUID string of the rank assigned to this player within their faction. */
    public String getRankId() {
        return getAs("rank_id", String.class, null);
    }

    public void setRankId(final String rankId) {
        set("rank_id", rankId);
    }

    public String getTitle() {
        return getAs("title", String.class, "");
    }

    public void setTitle(final String title) {
        set("title", title);
    }

    /** Extra power added or subtracted by an admin (may be negative). */
    public double getPowerBoost() {
        return getAs("power_boost", Double.class, 0.0);
    }

    public void setPowerBoost(final double powerBoost) {
        set("power_boost", powerBoost);
    }

    /** Current accumulated power for this player. */
    public double getPower() {
        return getAs("power", Double.class, 0.0);
    }

    public void setPower(final double power) {
        set("power", power);
    }

    /** Epoch-millis timestamp when this player joined their current faction. */
    public long getJoinedAt() {
        return getAs("joined_at", Long.class, 0L);
    }

    public void setJoinedAt(final long joinedAt) {
        set("joined_at", joinedAt);
    }

    /** Epoch-millis timestamp of the last time this player was seen online. */
    public long getLastActivity() {
        return getAs("last_activity", Long.class, 0L);
    }

    public void setLastActivity(final long lastActivity) {
        set("last_activity", lastActivity);
    }

    /** When {@code true} the player is in admin-override mode. */
    public boolean isOverriding() {
        return getAs("overriding", Integer.class, 0) == 1;
    }

    public void setOverriding(final boolean overriding) {
        set("overriding", overriding ? 1 : 0);
    }

    /** When {@code true} the player sees territory titles when crossing chunk borders. */
    public boolean hasTerritoryTitles() {
        return getAs("territory_titles", Integer.class, 1) == 1;
    }

    public void setTerritoryTitles(final boolean enabled) {
        set("territory_titles", enabled ? 1 : 0);
    }

    public boolean hasInviteNotifications() {
        return getAs("notify_invites", Integer.class, 1) == 1;
    }

    public void setInviteNotifications(final boolean enabled) {
        set("notify_invites", enabled ? 1 : 0);
    }

    public boolean hasBankTaxNotifications() {
        return getAs("notify_bank_tax", Integer.class, 1) == 1;
    }

    public void setBankTaxNotifications(final boolean enabled) {
        set("notify_bank_tax", enabled ? 1 : 0);
    }

    public AutoTerritoryMode getAutoTerritoryMode() {
        return AutoTerritoryMode.fromDbValue(getAs("auto_territory_mode", Integer.class, 0));
    }

    public void setAutoTerritoryMode(final AutoTerritoryMode mode) {
        final AutoTerritoryMode effective = mode == null ? AutoTerritoryMode.OFF : mode;
        set("auto_territory_mode", effective.getDbValue());
    }
}

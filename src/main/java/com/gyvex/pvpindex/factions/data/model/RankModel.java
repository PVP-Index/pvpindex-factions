package com.gyvex.pvpindex.factions.data.model;

import com.github.ezframework.jaloquent.model.Model;
import java.util.Map;

/**
 * Persistent model for a faction rank (role).
 *
 * <p>{@code id} = UUID string. Each faction has at minimum three built-in
 * ranks: OWNER (priority 100), OFFICER (priority 50), and MEMBER (priority 10).
 * Admins may create additional custom ranks between these tiers.
 */
public class RankModel extends Model {

    public static final String PREFIX = "ranks";

    public static final Map<String, String> COLUMNS = Map.of(
        "id", "VARCHAR(36) NOT NULL",
        "faction_id", "VARCHAR(36) NOT NULL",
        "name", "VARCHAR(64) NOT NULL",
        "prefix", "VARCHAR(32)",
        "priority", "INT NOT NULL DEFAULT 10"
    );

    /** Built-in rank names — always present for every faction. */
    public static final String RANK_OWNER = "Owner";
    public static final String RANK_OFFICER = "Officer";
    public static final String RANK_MEMBER = "Member";

    /** Priority thresholds used to map ranks to {@link com.skyblockexp.teamsapi.model.TeamRole}. */
    public static final int PRIORITY_OWNER = 100;
    public static final int PRIORITY_OFFICER = 50;
    public static final int PRIORITY_MEMBER = 10;

    public RankModel(final String id) {
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
        return getAs("name", String.class, "Member");
    }

    public void setName(final String name) {
        set("name", name);
    }

    /** Chat prefix shown before the player's name (MiniMessage string, may be null). */
    public String getPrefix() {
        return getAs("prefix", String.class, null);
    }

    public void setPrefix(final String prefix) {
        set("prefix", prefix);
    }

    /**
     * Priority of this rank. Higher value = more authority.
     * Built-in: Owner=100, Officer=50, Member=10.
     */
    public int getPriority() {
        return getAs("priority", Integer.class, PRIORITY_MEMBER);
    }

    public void setPriority(final int priority) {
        set("priority", priority);
    }

    // -------------------------------------------------------------------------
    // Convenience
    // -------------------------------------------------------------------------

    public boolean isOwner() {
        return getPriority() >= PRIORITY_OWNER;
    }

    public boolean isOfficerOrAbove() {
        return getPriority() >= PRIORITY_OFFICER;
    }

    /** Returns {@code true} when this rank can manage (promote/kick) {@code other}. */
    public boolean canManage(final RankModel other) {
        return getPriority() > other.getPriority();
    }
}

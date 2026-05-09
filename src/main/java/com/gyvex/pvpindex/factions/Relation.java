package com.gyvex.pvpindex.factions;

/**
 * Represents the relationship between two factions, or between a player and a faction.
 *
 * <p>Ordinal ordering (lowest → highest hostility):
 * MEMBER &lt; ALLY &lt; TRUCE &lt; NEUTRAL &lt; ENEMY
 */
public enum Relation {

    /** The player/faction is a full member of the faction. */
    MEMBER,

    /** The two factions are formal allies — mutual benefits. */
    ALLY,

    /** The two factions have agreed to a temporary truce — no hostility. */
    TRUCE,

    /** No formal relation; neither friendly nor hostile. */
    NEUTRAL,

    /** The two factions are actively hostile. */
    ENEMY;

    // -------------------------------------------------------------------------
    // Convenience helpers
    // -------------------------------------------------------------------------

    public boolean isAtLeast(final Relation other) {
        return this.ordinal() <= other.ordinal();
    }

    public boolean isAtMost(final Relation other) {
        return this.ordinal() >= other.ordinal();
    }

    public boolean isFriendly() {
        return this == MEMBER || this == ALLY;
    }

    public boolean isNeutralOrBetter() {
        return this != ENEMY;
    }

    public boolean isHostile() {
        return this == ENEMY;
    }

    /** Display name suitable for chat messages. */
    public String displayName() {
        return switch (this) {
            case MEMBER -> "Member";
            case ALLY -> "Ally";
            case TRUCE -> "Truce";
            case NEUTRAL -> "Neutral";
            case ENEMY -> "Enemy";
        };
    }

    /** MiniMessage color tag for this relation level. */
    public String colorTag() {
        return switch (this) {
            case MEMBER -> "<green>";
            case ALLY -> "<aqua>";
            case TRUCE -> "<yellow>";
            case NEUTRAL -> "<gray>";
            case ENEMY -> "<red>";
        };
    }
}

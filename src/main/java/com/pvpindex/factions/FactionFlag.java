package com.pvpindex.factions;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Enumeration of all toggleable per-faction boolean flags.
 *
 * <p>Each flag has a config/JSON {@link #getId() id} (e.g. {@code "friendly-fire"}),
 * a human-readable {@link #getDisplayName() displayName}, a short
 * {@link #getDescription() description}, and a hard-coded
 * {@link #getDefaultValue() defaultValue} that may be overridden by
 * {@link com.pvpindex.factions.config.FactionsConfig}.
 */
public enum FactionFlag {

    /** Allow PvP inside this faction's claimed territory. */
    PVP("pvp", "PvP", "Allow PvP in this faction's territory.", true),

    /** Allow faction members to damage each other anywhere. */
    FRIENDLY_FIRE("friendly-fire", "Friendly Fire",
            "Allow faction members to harm each other.", false),

    /** Allow explosions to destroy terrain in this faction's territory. */
    EXPLOSIONS("explosions", "Explosions",
            "Allow explosions to destroy terrain in this faction's territory.", false),

    /** Allow fire to spread inside this faction's territory. */
    FIRE_SPREAD("fire-spread", "Fire Spread",
            "Allow fire to spread in this faction's territory.", false),

    /** Allow any player to join this faction without a pending invite. */
    OPEN("open", "Open", "Allow anyone to join this faction without an invite.", false);

    // -------------------------------------------------------------------------

    private final String id;
    private final String displayName;
    private final String description;
    private final boolean defaultValue;

    FactionFlag(
            final String id,
            final String displayName,
            final String description,
            final boolean defaultValue) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    /** The config / JSON key for this flag (e.g. {@code "friendly-fire"}). */
    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Hard-coded default value. May be overridden per-server by
     * {@link com.pvpindex.factions.config.FactionsConfig#getFlagDefault(FactionFlag)}.
     */
    public boolean getDefaultValue() {
        return defaultValue;
    }

    /**
     * Finds a flag by its id, case-insensitively.
     *
     * @param id the flag id string (e.g. {@code "pvp"} or {@code "fire-spread"})
     * @return the matching flag, or empty if unknown
     */
    public static Optional<FactionFlag> byId(final String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (final FactionFlag flag : values()) {
            if (flag.id.equalsIgnoreCase(id)) {
                return Optional.of(flag);
            }
        }
        return Optional.empty();
    }

    /** Returns a list of all flag id strings, suitable for tab-completion. */
    public static List<String> ids() {
        return Arrays.stream(values()).map(FactionFlag::getId).toList();
    }
}

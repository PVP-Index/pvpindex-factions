package com.pvpindex.factions.config;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed accessor for {@code notifications.yml}.
 */
public class NotificationsConfig {

    private final FileConfiguration cfg;

    public NotificationsConfig(final FileConfiguration cfg) {
        this.cfg = cfg;
    }

    // -------------------------------------------------------------------------
    // Inbox
    // -------------------------------------------------------------------------

    public boolean isInboxEnabled() {
        return cfg.getBoolean("inbox.enabled", true);
    }

    public int getInboxMaxPerLogin() {
        return cfg.getInt("inbox.max-per-login", 20);
    }

    // -------------------------------------------------------------------------
    // Member events
    // -------------------------------------------------------------------------

    public boolean isMemberNotifyPlayerJoined() {
        return cfg.getBoolean("member.notify-player-joined", true);
    }

    // -------------------------------------------------------------------------
    // Economy / Tax
    // -------------------------------------------------------------------------

    public boolean isEconomyTaxNotifyMembers() {
        return cfg.getBoolean("economy.tax.notify-members", true);
    }

    // -------------------------------------------------------------------------
    // EzCountdown
    // -------------------------------------------------------------------------

    public boolean isEzCountdownEnabled() {
        return cfg.getBoolean("ezcountdown.enabled", true);
    }

    public long getEzCountdownDurationSeconds() {
        return cfg.getLong("ezcountdown.announcement-duration-seconds", 8L);
    }

    public List<String> getEzCountdownDisplayTypes() {
        final List<String> types = cfg.getStringList("ezcountdown.display-types");
        return types.isEmpty() ? List.of("ACTION_BAR") : types;
    }
}

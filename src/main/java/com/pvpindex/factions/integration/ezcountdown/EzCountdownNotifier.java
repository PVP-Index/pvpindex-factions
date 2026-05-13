package com.pvpindex.factions.integration.ezcountdown;

import com.skyblockexp.ezcountdown.api.EzCountdownApi;
import com.skyblockexp.ezcountdown.api.model.Notification;
import com.skyblockexp.ezcountdown.display.DisplayType;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Soft-dependency wrapper around the EzCountdown notification API.
 *
 * <p>All methods are no-ops when EzCountdown is absent. Call {@link #setup()} at
 * startup; use {@link #isEnabled()} before calling {@link #sendAnnouncement} if you
 * want to branch on availability.
 */
public class EzCountdownNotifier {

    private final Logger logger;
    private boolean pluginPresent = true;

    public EzCountdownNotifier(final Logger logger) {
        this.logger = logger;
    }

    /**
     * Check whether the EzCountdown plugin jar is loaded.
     *
     * @return {@code true} if EzCountdown is present on the server
     */
    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("EzCountdown") == null) {
            pluginPresent = false;
            return false;
        }
        return true;
    }

    /** @return {@code true} if EzCountdown is present and its API service is registered. */
    public boolean isEnabled() {
        return api() != null;
    }

    /**
     * Fire a server-wide ephemeral display notification via EzCountdown.
     *
     * <p>The {@code message} is shown in each configured display (e.g. ACTION_BAR, TITLE)
     * for {@code durationSeconds} seconds. No countdown YAML entry is created.
     *
     * @param message         MiniMessage-formatted text to display
     * @param durationSeconds how long the notification runs (must be &gt; 0)
     * @param displayTypes    raw display-type names from config (e.g. {@code ["ACTION_BAR"]})
     */
    public void sendAnnouncement(
            final String message,
            final long durationSeconds,
            final List<String> displayTypes) {
        final EzCountdownApi api = this.api();
        if (api == null) {
            return;
        }
        final EnumSet<DisplayType> displays = parseDisplayTypes(displayTypes);
        try {
            final Notification notif = Notification.builder()
                .duration(durationSeconds)
                .displays(displays)
                .message(message)
                .build();
            api.sendNotification(notif);
        } catch (Exception e) {
            logger.warning("EzCountdown sendNotification failed: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private EzCountdownApi api() {
        if (!pluginPresent) {
            return null;
        }
        final RegisteredServiceProvider<EzCountdownApi> rsp =
            Bukkit.getServicesManager().getRegistration(EzCountdownApi.class);
        return rsp != null ? rsp.getProvider() : null;
    }

    private EnumSet<DisplayType> parseDisplayTypes(final List<String> names) {
        if (names == null || names.isEmpty()) {
            return EnumSet.of(DisplayType.ACTION_BAR);
        }
        final EnumSet<DisplayType> result = EnumSet.noneOf(DisplayType.class);
        for (final String name : names) {
            try {
                result.add(DisplayType.valueOf(name.toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                logger.warning("EzCountdown: unknown display type '" + name + "' — skipping.");
            }
        }
        return result.isEmpty() ? EnumSet.of(DisplayType.ACTION_BAR) : result;
    }
}

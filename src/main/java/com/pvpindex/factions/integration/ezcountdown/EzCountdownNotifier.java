package com.pvpindex.factions.integration.ezcountdown;

import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;

/**
 * Soft-dependency wrapper around the EzCountdown notification API.
 *
 * <p><strong>No EzCountdown imports.</strong> This class is safe to load unconditionally.
 * When EzCountdown is present, {@link #setup()} loads {@code EzCountdownNotifierImpl} via
 * reflection and delegates all calls to it. When EzCountdown is absent every method is a
 * no-op.
 *
 * <p>Call {@link #setup()} at startup; use {@link #isEnabled()} before calling
 * {@link #sendAnnouncement} if you want to branch on availability.
 */
public class EzCountdownNotifier {

    static final String IMPL_CLASS =
        "com.pvpindex.factions.integration.ezcountdown.EzCountdownNotifierImpl";

    private final Logger logger;
    private EzCountdownSender delegate;

    public EzCountdownNotifier(final Logger logger) {
        this.logger = logger;
    }

    /**
     * Check whether the EzCountdown plugin jar is loaded and wire up the implementation.
     *
     * @return {@code true} if EzCountdown is present and the integration loaded successfully
     */
    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("EzCountdown") == null) {
            return false;
        }
        try {
            delegate = (EzCountdownSender) Class.forName(IMPL_CLASS)
                .getDeclaredConstructor(Logger.class)
                .newInstance(logger);
            return true;
        } catch (Exception e) {
            logger.warning("Failed to load EzCountdown integration: " + e.getMessage());
            return false;
        }
    }

    /** @return {@code true} if EzCountdown is present and its API service is registered. */
    public boolean isEnabled() {
        return delegate != null && delegate.isEnabled();
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
        if (delegate != null) {
            delegate.sendAnnouncement(message, durationSeconds, displayTypes);
        }
    }
}

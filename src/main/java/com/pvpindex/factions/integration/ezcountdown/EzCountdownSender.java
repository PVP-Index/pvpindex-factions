package com.pvpindex.factions.integration.ezcountdown;

import java.util.List;

/**
 * Isolation interface for the EzCountdown notification contract.
 *
 * <p><strong>No EzCountdown imports.</strong> Safe to reference from any code that is
 * loaded unconditionally. The concrete implementation ({@code EzCountdownNotifierImpl})
 * carries all EzCountdown type references and is loaded via reflection only after
 * EzCountdown has been confirmed present on the server.
 */
public interface EzCountdownSender {

    /** @return {@code true} if EzCountdown is present and its API service is registered. */
    boolean isEnabled();

    /**
     * Fire a server-wide ephemeral display notification via EzCountdown.
     *
     * @param message         MiniMessage-formatted text to display
     * @param durationSeconds how long the notification runs (must be &gt; 0)
     * @param displayTypes    raw display-type names from config (e.g. {@code ["ACTION_BAR"]})
     */
    void sendAnnouncement(String message, long durationSeconds, List<String> displayTypes);
}

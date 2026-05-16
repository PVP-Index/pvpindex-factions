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
 * Concrete EzCountdown notification sender.
 *
 * <p><strong>Never reference this class directly from bootstrap or any other code
 * that is loaded unconditionally.</strong> It must only be instantiated via
 * {@code Class.forName("com.pvpindex.factions.integration.ezcountdown.EzCountdownNotifierImpl")}
 * after EzCountdown has been confirmed present on the server. Loading this class
 * when EzCountdown is absent will throw {@link NoClassDefFoundError}.
 */
public final class EzCountdownNotifierImpl implements EzCountdownSender {

    private final Logger logger;

    public EzCountdownNotifierImpl(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public boolean isEnabled() {
        return api() != null;
    }

    @Override
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

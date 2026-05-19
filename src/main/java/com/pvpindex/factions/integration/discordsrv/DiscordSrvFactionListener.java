package com.pvpindex.factions.integration.discordsrv;

import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.event.FactionCreateEvent;
import com.pvpindex.factions.event.FactionDisbandEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Bukkit event listener that forwards faction lifecycle events to Discord
 * via {@link DiscordSrvNotifier}.
 *
 * <p>Only registered when DiscordSRV is present and
 * {@code integrations.discordsrv.enabled} is {@code true} in config.
 */
public final class DiscordSrvFactionListener implements Listener {

    private final DiscordSrvNotifier notifier;
    private final FactionsConfig config;

    public DiscordSrvFactionListener(final DiscordSrvNotifier notifier, final FactionsConfig config) {
        this.notifier = notifier;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFactionCreate(final FactionCreateEvent event) {
        if (!config.isDiscordSrvFactionCreatedEnabled()) {
            return;
        }
        notifier.sendMessage(
            config.getDiscordSrvFactionCreatedMessage()
                .replace("{faction}", event.getFaction().getName()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFactionDisband(final FactionDisbandEvent event) {
        if (!config.isDiscordSrvFactionDisbandedEnabled()) {
            return;
        }
        notifier.sendMessage(
            config.getDiscordSrvFactionDisbandedMessage()
                .replace("{faction}", event.getFaction().getName()));
    }
}

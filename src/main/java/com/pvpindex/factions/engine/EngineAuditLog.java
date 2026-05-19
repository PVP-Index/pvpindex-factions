package com.pvpindex.factions.engine;

import com.pvpindex.factions.FactionAuditAction;
import com.pvpindex.factions.event.FactionBankTransactionEvent;
import com.pvpindex.factions.event.FactionChunkClaimEvent;
import com.pvpindex.factions.event.FactionChunkUnclaimEvent;
import com.pvpindex.factions.service.AuditService;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Listens for Bukkit faction events and forwards them to {@link AuditService}.
 *
 * <p>Handles claim/unclaim and bank-transaction events at MONITOR priority so
 * all earlier handlers (including cancellation) have already run. Kick,
 * promotion, demotion, and relation-change actions are recorded directly by
 * {@link com.pvpindex.factions.service.FactionServiceImpl}.
 */
public final class EngineAuditLog implements Listener {

    private final AuditService auditService;

    public EngineAuditLog(final AuditService auditService) {
        this.auditService = auditService;
    }

    /** Register this engine as a Bukkit event listener. */
    public void register(final Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkClaim(final FactionChunkClaimEvent event) {
        final String detail = event.getWorldName() + " " + event.getChunkX() + "," + event.getChunkZ();
        auditService.record(event.getFaction().getId(), event.getPlayerUUID(), FactionAuditAction.CLAIM, detail);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnclaim(final FactionChunkUnclaimEvent event) {
        final String detail = event.getWorldName() + " " + event.getChunkX() + "," + event.getChunkZ();
        auditService.record(event.getFaction().getId(), event.getPlayerUUID(), FactionAuditAction.UNCLAIM, detail);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBankTransaction(final FactionBankTransactionEvent event) {
        final FactionAuditAction action = switch (event.getType()) {
            case DEPOSIT -> FactionAuditAction.BANK_DEPOSIT;
            case WITHDRAW -> FactionAuditAction.BANK_WITHDRAW;
            default -> FactionAuditAction.BANK_TRANSFER;
        };
        final String detail = String.format(Locale.ROOT, "%.2f", event.getAmount());
        auditService.record(event.getFaction().getId(), event.getPlayerUUID(), action, detail);
    }
}

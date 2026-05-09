package com.pvpindex.factions.event;

import com.pvpindex.factions.data.model.FactionModel;
import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when money is deposited to or withdrawn from a faction bank. Cancellable. */
public final class FactionBankTransactionEvent extends Event implements Cancellable {

    public enum Type { DEPOSIT, WITHDRAW, TRANSFER }

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    private final FactionModel faction;
    private final UUID playerUUID;
    private final Type type;
    private double amount;

    public FactionBankTransactionEvent(
            final FactionModel faction,
            final UUID playerUUID,
            final Type type,
            final double amount) {
        this.faction = faction;
        this.playerUUID = playerUUID;
        this.type = type;
        this.amount = amount;
    }

    public FactionModel getFaction() {
        return faction;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Type getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(final double amount) {
        this.amount = amount;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(final boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

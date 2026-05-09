package com.pvpindex.factions.event;

import com.pvpindex.factions.data.model.FactionModel;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a faction is disbanded. Cancellable. */
public final class FactionDisbandEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;
    private final FactionModel faction;

    public FactionDisbandEvent(final FactionModel faction) {
        this.faction = faction;
    }

    public FactionModel getFaction() {
        return faction;
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

package com.gyvex.pvpindex.factions.event;

import com.gyvex.pvpindex.factions.data.model.FactionModel;
import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a player joins a faction. Cancellable. */
public final class FactionJoinEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;
    private final FactionModel faction;
    private final UUID playerUUID;

    public FactionJoinEvent(final FactionModel faction, final UUID playerUUID) {
        this.faction = faction;
        this.playerUUID = playerUUID;
    }

    public FactionModel getFaction() {
        return faction;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
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

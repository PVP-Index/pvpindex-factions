package com.pvpindex.factions.event;

import com.pvpindex.factions.data.model.FactionModel;
import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a new faction is successfully created. Cancellable. */
public final class FactionCreateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    private final FactionModel faction;
    private final UUID creatorUUID;

    public FactionCreateEvent(final FactionModel faction, final UUID creatorUUID) {
        this.faction = faction;
        this.creatorUUID = creatorUUID;
    }

    public FactionModel getFaction() {
        return faction;
    }

    public UUID getCreatorUUID() {
        return creatorUUID;
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

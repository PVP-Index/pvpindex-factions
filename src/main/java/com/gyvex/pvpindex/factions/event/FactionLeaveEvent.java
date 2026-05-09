package com.gyvex.pvpindex.factions.event;

import com.gyvex.pvpindex.factions.data.model.FactionModel;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a player leaves (or is kicked from) a faction. Not cancellable. */
public final class FactionLeaveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final FactionModel faction;
    private final UUID playerUUID;

    public FactionLeaveEvent(final FactionModel faction, final UUID playerUUID) {
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
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

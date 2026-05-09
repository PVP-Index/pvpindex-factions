package com.pvpindex.factions.event;

import com.pvpindex.factions.data.model.FactionModel;
import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a faction unclaims a chunk. Cancellable. */
public final class FactionChunkUnclaimEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    private final FactionModel faction;
    private final UUID playerUUID;
    private final String worldName;
    private final int chunkX;
    private final int chunkZ;

    public FactionChunkUnclaimEvent(
            final FactionModel faction, final UUID playerUUID,
            final String worldName, final int chunkX, final int chunkZ) {
        this.faction = faction;
        this.playerUUID = playerUUID;
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public FactionModel getFaction() {
        return faction;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
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

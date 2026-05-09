package com.gyvex.pvpindex.factions.registry;

import com.gyvex.pvpindex.factions.engine.EngineChunkChange;
import com.gyvex.pvpindex.factions.engine.EngineEconomy;
import com.gyvex.pvpindex.factions.engine.AutoTerritoryModeCache;
import com.gyvex.pvpindex.factions.gui.FactionsGuiManager;

/**
 * Holds engine instances that are needed across multiple subsystems.
 *
 * <p>Engines that are only used internally (EngineProtection, EnginePlayerMove,
 * EngineChat, EnginePower) are not stored here — they are registered as
 * Bukkit Listeners by {@link com.gyvex.pvpindex.factions.Bootstrap} and
 * require no further reference.
 */
public class EngineRegistry {

    private EngineChunkChange chunkChange;
    private EngineEconomy economy;
    private AutoTerritoryModeCache autoTerritoryModeCache;
    private FactionsGuiManager factionsGuiManager;

    public void setChunkChange(final EngineChunkChange engine) { this.chunkChange = engine; }
    public void setEconomy(final EngineEconomy engine) { this.economy = engine; }
    public void setAutoTerritoryModeCache(final AutoTerritoryModeCache cache) { this.autoTerritoryModeCache = cache; }
    public void setFactionsGuiManager(final FactionsGuiManager manager) { this.factionsGuiManager = manager; }

    public EngineChunkChange getChunkChange() { return chunkChange; }
    public EngineEconomy getEconomy() { return economy; }
    public AutoTerritoryModeCache getAutoTerritoryModeCache() { return autoTerritoryModeCache; }
    public FactionsGuiManager getFactionsGuiManager() { return factionsGuiManager; }
}

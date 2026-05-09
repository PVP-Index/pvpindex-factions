package com.gyvex.pvpindex.factions.bootstrap;

import com.gyvex.pvpindex.factions.PvPIndexFactions;
import com.gyvex.pvpindex.factions.api.FactionsTeamsInviteService;
import com.gyvex.pvpindex.factions.api.FactionsTeamsService;
import com.gyvex.pvpindex.factions.api.FactionsTeamsWarpService;
import com.gyvex.pvpindex.factions.registry.EngineRegistry;
import com.gyvex.pvpindex.factions.registry.InfraRegistry;
import com.gyvex.pvpindex.factions.registry.ServiceRegistry;

/**
 * Shared state available to bootstrap components.
 */
public final class BootstrapContext {

    private final PvPIndexFactions plugin;
    private final InfraRegistry infraRegistry;
    private final ServiceRegistry serviceRegistry;
    private final EngineRegistry engineRegistry;

    private FactionsTeamsService teamsAdapter;
    private FactionsTeamsInviteService inviteAdapter;
    private FactionsTeamsWarpService warpAdapter;
    private boolean teamsApiEnabled;
    private boolean vaultEnabled;
    private boolean placeholderApiEnabled;
    private boolean dynmapEnabled;
    private boolean lwcEnabled;

    public BootstrapContext(
            final PvPIndexFactions plugin,
            final InfraRegistry infraRegistry,
            final ServiceRegistry serviceRegistry,
            final EngineRegistry engineRegistry) {
        this.plugin = plugin;
        this.infraRegistry = infraRegistry;
        this.serviceRegistry = serviceRegistry;
        this.engineRegistry = engineRegistry;
    }

    public PvPIndexFactions plugin() { return plugin; }
    public InfraRegistry infra() { return infraRegistry; }
    public ServiceRegistry services() { return serviceRegistry; }
    public EngineRegistry engines() { return engineRegistry; }

    public FactionsTeamsService getTeamsAdapter() { return teamsAdapter; }
    public void setTeamsAdapter(final FactionsTeamsService teamsAdapter) { this.teamsAdapter = teamsAdapter; }
    public FactionsTeamsInviteService getInviteAdapter() { return inviteAdapter; }
    public void setInviteAdapter(final FactionsTeamsInviteService inviteAdapter) { this.inviteAdapter = inviteAdapter; }
    public FactionsTeamsWarpService getWarpAdapter() { return warpAdapter; }
    public void setWarpAdapter(final FactionsTeamsWarpService warpAdapter) { this.warpAdapter = warpAdapter; }
    public boolean isTeamsApiEnabled() { return teamsApiEnabled; }
    public void setTeamsApiEnabled(final boolean teamsApiEnabled) { this.teamsApiEnabled = teamsApiEnabled; }
    public boolean isVaultEnabled() { return vaultEnabled; }
    public void setVaultEnabled(final boolean vaultEnabled) { this.vaultEnabled = vaultEnabled; }
    public boolean isPlaceholderApiEnabled() { return placeholderApiEnabled; }
    public void setPlaceholderApiEnabled(final boolean placeholderApiEnabled) {
        this.placeholderApiEnabled = placeholderApiEnabled;
    }
    public boolean isDynmapEnabled() { return dynmapEnabled; }
    public void setDynmapEnabled(final boolean dynmapEnabled) { this.dynmapEnabled = dynmapEnabled; }
    public boolean isLwcEnabled() { return lwcEnabled; }
    public void setLwcEnabled(final boolean lwcEnabled) { this.lwcEnabled = lwcEnabled; }
}

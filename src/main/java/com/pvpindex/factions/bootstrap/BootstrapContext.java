package com.pvpindex.factions.bootstrap;

import com.pvpindex.factions.PvPIndexFactions;
import com.pvpindex.factions.registry.EngineRegistry;
import com.pvpindex.factions.registry.InfraRegistry;
import com.pvpindex.factions.registry.ServiceRegistry;

/**
 * Shared state available to bootstrap components.
 */
public final class BootstrapContext {

    private final PvPIndexFactions plugin;
    private final InfraRegistry infraRegistry;
    private final ServiceRegistry serviceRegistry;
    private final EngineRegistry engineRegistry;

    private Object teamsAdapter;
    private Object inviteAdapter;
    private Object warpAdapter;
    private Object claimAdapter;
    private Object powerAdapter;
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

    public Object getTeamsAdapter() { return teamsAdapter; }
    public void setTeamsAdapter(final Object teamsAdapter) { this.teamsAdapter = teamsAdapter; }
    public Object getInviteAdapter() { return inviteAdapter; }
    public void setInviteAdapter(final Object inviteAdapter) { this.inviteAdapter = inviteAdapter; }
    public Object getWarpAdapter() { return warpAdapter; }
    public void setWarpAdapter(final Object warpAdapter) { this.warpAdapter = warpAdapter; }
    public Object getClaimAdapter() { return claimAdapter; }
    public void setClaimAdapter(final Object claimAdapter) { this.claimAdapter = claimAdapter; }
    public Object getPowerAdapter() { return powerAdapter; }
    public void setPowerAdapter(final Object powerAdapter) { this.powerAdapter = powerAdapter; }
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

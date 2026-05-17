package com.pvpindex.factions.bootstrap;

import com.pvpindex.factions.api.TeamsApiRegistrar;
import com.pvpindex.factions.registry.EngineRegistry;
import com.pvpindex.factions.registry.InfraRegistry;
import com.pvpindex.factions.registry.ServiceRegistry;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Shared state available to bootstrap components.
 *
 * <p>{@link #plugin()} returns the {@link Plugin} interface, which is safely
 * mockable in unit tests. {@link #javaPlugin()} casts to {@link JavaPlugin}
 * for bootstrap components that need JavaPlugin-specific methods
 * ({@code saveDefaultConfig}, {@code saveResource}, {@code getCommand}, etc.).
 */
public final class BootstrapContext {

    private final Plugin plugin;
    private final InfraRegistry infraRegistry;
    private final ServiceRegistry serviceRegistry;
    private final EngineRegistry engineRegistry;

    private TeamsApiRegistrar teamsRegistrar;
    private boolean teamsApiEnabled;
    private boolean vaultEnabled;
    private boolean placeholderApiEnabled;
    private boolean dynmapEnabled;
    private boolean lwcEnabled;
    private boolean ezCountdownEnabled;

    public BootstrapContext(
            final Plugin plugin,
            final InfraRegistry infraRegistry,
            final ServiceRegistry serviceRegistry,
            final EngineRegistry engineRegistry) {
        this.plugin = plugin;
        this.infraRegistry = infraRegistry;
        this.serviceRegistry = serviceRegistry;
        this.engineRegistry = engineRegistry;
    }

    /** Returns the plugin as the {@link Plugin} interface — safe to mock in tests. */
    public Plugin plugin() { return plugin; }

    /** Returns the plugin cast to {@link JavaPlugin} for components that need it. */
    public JavaPlugin javaPlugin() { return (JavaPlugin) plugin; }

    public InfraRegistry infra() { return infraRegistry; }
    public ServiceRegistry services() { return serviceRegistry; }
    public EngineRegistry engines() { return engineRegistry; }

    public TeamsApiRegistrar getTeamsRegistrar() { return teamsRegistrar; }
    public void setTeamsRegistrar(final TeamsApiRegistrar teamsRegistrar) { this.teamsRegistrar = teamsRegistrar; }
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
    public boolean isEzCountdownEnabled() { return ezCountdownEnabled; }
    public void setEzCountdownEnabled(final boolean ezCountdownEnabled) { this.ezCountdownEnabled = ezCountdownEnabled; }
}

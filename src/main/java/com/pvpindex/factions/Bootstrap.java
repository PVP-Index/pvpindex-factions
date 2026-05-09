package com.pvpindex.factions;

import com.pvpindex.factions.bootstrap.BootstrapComponent;
import com.pvpindex.factions.bootstrap.BootstrapContext;
import com.pvpindex.factions.bootstrap.CommandsBootstrapComponent;
import com.pvpindex.factions.bootstrap.EnginesBootstrapComponent;
import com.pvpindex.factions.bootstrap.InfrastructureBootstrapComponent;
import com.pvpindex.factions.bootstrap.OptionalHooksBootstrapComponent;
import com.pvpindex.factions.bootstrap.ServicesBootstrapComponent;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.registry.EngineRegistry;
import com.pvpindex.factions.registry.InfraRegistry;
import com.pvpindex.factions.registry.ServiceRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns all startup and shutdown logic for PvPIndex Factions.
 *
 * <p>Startup order:
 * <ol>
 *   <li>Load config</li>
 *   <li>Initialise database + repositories</li>
 *   <li>Hook Vault (soft-depend)</li>
 *   <li>Initialise internal services (no TeamsAPI required)</li>
 *   <li>Register TeamsAPI provider if TeamsAPI is present (optional)</li>
 *   <li>Register engines (Listeners + scheduler tasks)</li>
 *   <li>Register commands</li>
 *   <li>Hook PlaceholderAPI (soft-depend)</li>
 * </ol>
 *
 * <p>Call {@link #start()} from {@code onEnable} and {@link #stop()} from
 * {@code onDisable}. After a successful {@link #start()}, the populated
 * {@link InfraRegistry}, {@link ServiceRegistry}, and {@link EngineRegistry}
 * are accessible via their respective getters.
 */
public class Bootstrap {

    private final PvPIndexFactions plugin;

    private final InfraRegistry infraRegistry = new InfraRegistry();
    private final ServiceRegistry serviceRegistry = new ServiceRegistry();
    private final EngineRegistry engineRegistry = new EngineRegistry();
    private final BootstrapContext context;
    private final List<BootstrapComponent> components;
    private final List<BootstrapComponent> startedComponents = new ArrayList<>();

    public Bootstrap(final PvPIndexFactions plugin) {
        this.plugin = plugin;
        this.context = new BootstrapContext(plugin, infraRegistry, serviceRegistry, engineRegistry);
        this.components = List.of(
            new InfrastructureBootstrapComponent(),
            new ServicesBootstrapComponent(),
            new EnginesBootstrapComponent(),
            new CommandsBootstrapComponent(),
            new OptionalHooksBootstrapComponent()
        );
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Execute startup sequence.
     *
     * @return {@code true} if startup completed successfully, {@code false} if
     *         a fatal error occurred (plugin should be disabled by the caller).
     */
    public boolean start() {
        plugin.getLogger().info("--------------------------------------------------");
        plugin.getLogger().info("PvPIndex Factions is rallying the clans...");
        plugin.getLogger().info("Preparing claims, power, relations, and war economy.");
        plugin.getLogger().info("--------------------------------------------------");

        for (final BootstrapComponent component : components) {
            final boolean ok = component.start(context);
            if (!ok) {
                plugin.getLogger().severe("Bootstrap phase failed: " + component.name());
                stopStartedComponents();
                return false;
            }
            startedComponents.add(component);
        }

        logStartupSummary();
        return true;
    }

    /**
     * Execute shutdown sequence — always safe to call even if {@link #start()}
     * was never called or returned {@code false}.
     */
    public void stop() {
        stopStartedComponents();
        plugin.getLogger().info("PvPIndex Factions stood down. Territory state saved and services stopped.");
    }

    private void stopStartedComponents() {
        for (int i = startedComponents.size() - 1; i >= 0; i--) {
            final BootstrapComponent component = startedComponents.get(i);
            try {
                component.stop(context);
            } catch (Exception e) {
                plugin.getLogger().warning("Shutdown phase failed: " + component.name() + " (" + e.getMessage() + ")");
            }
        }
        startedComponents.clear();
    }

    // -------------------------------------------------------------------------
    // Registry accessors
    // -------------------------------------------------------------------------

    public InfraRegistry getInfraRegistry() { return infraRegistry; }
    public ServiceRegistry getServiceRegistry() { return serviceRegistry; }
    public EngineRegistry getEngineRegistry() { return engineRegistry; }

    private void logStartupSummary() {
        final FactionsConfig cfg = infraRegistry.getConfig();
        plugin.getLogger().info("--------------------------------------------------");
        plugin.getLogger().info("PvPIndex Factions is now online.");
        plugin.getLogger().info("Command roots: /f and /fa");
        plugin.getLogger().info("Faction limits: members=" + cfg.getMaxMembers()
            + ", warps=" + cfg.getMaxWarps()
            + ", allies=" + cfg.getMaxAllies()
            + ", truces=" + cfg.getMaxTruces());
        plugin.getLogger().info("Core systems: land, power, relations, warps, bank, invites");
        plugin.getLogger().info("Integrations: TeamsAPI=" + state(context.isTeamsApiEnabled())
            + ", Vault=" + state(context.isVaultEnabled())
            + ", PlaceholderAPI=" + state(context.isPlaceholderApiEnabled())
            + ", Dynmap=" + state(context.isDynmapEnabled())
            + ", LWC=" + state(context.isLwcEnabled()));
        plugin.getLogger().info("Factions are ready. Raise your banner and claim your land.");
        plugin.getLogger().info("--------------------------------------------------");
    }

    private String state(final boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }
}

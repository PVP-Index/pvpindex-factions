package com.gyvex.pvpindex.factions;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin entry-point.
 *
 * <p>All startup and shutdown logic lives in {@link Bootstrap}.
 * This class intentionally contains nothing beyond lifecycle delegation.
 */
public final class PvPIndexFactions extends JavaPlugin {

    private Bootstrap bootstrap;

    @Override
    public void onEnable() {
        bootstrap = new Bootstrap(this);
        if (!bootstrap.start()) {
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (bootstrap != null) {
            bootstrap.stop();
        }
    }

    // -------------------------------------------------------------------------
    // Accessors — delegates to registries for external callers
    // -------------------------------------------------------------------------

    public Bootstrap getBootstrap() { return bootstrap; }
}


package com.gyvex.pvpindex.factions.integration.lwc;

import org.bukkit.plugin.Plugin;

/**
 * Optional bridge for LWC/LWCX protection behavior.
 */
public interface LwcInterop {

    /**
     * Register listeners/hooks if available.
     *
     * @param plugin owning plugin
     */
    void register(Plugin plugin);

    /**
     * Unregister listeners/hooks.
     */
    void unregister();
}

package com.pvpindex.factions.integration.lwc;

import org.bukkit.plugin.Plugin;

/**
 * No-op interop when LWC integration is disabled or unavailable.
 */
public final class NoopLwcInterop implements LwcInterop {

    @Override
    public void register(final Plugin plugin) {
        // Intentionally no-op.
    }

    @Override
    public void unregister() {
        // Intentionally no-op.
    }
}

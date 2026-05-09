package com.gyvex.pvpindex.factions.integration.lwc;

import com.gyvex.pvpindex.factions.config.FactionsConfig;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

/**
 * Resolves the active LWC interop provider.
 */
public final class LwcInteropFactory {

    private LwcInteropFactory() {
    }

    public static LwcInterop create(
            final Plugin plugin,
            final FactionsConfig config,
            final com.gyvex.pvpindex.factions.data.Repositories repos,
            final Logger logger) {
        if (!config.isLwcEnabled()) {
            logger.info("LWC integration disabled in config.");
            return new NoopLwcInterop();
        }

        final PluginManager pm = plugin.getServer().getPluginManager();
        final Plugin lwc = pm.getPlugin("LWC");
        final Plugin lwcx = pm.getPlugin("LWCX");
        if (lwc == null && lwcx == null) {
            logger.info("LWC/LWCX not found - integration disabled.");
            return new NoopLwcInterop();
        }

        logger.info("LWC/LWCX detected - integration enabled.");
        return new LwcxInterop(repos, config, logger);
    }
}

package com.pvpindex.factions.integration.essentials;

import com.earth2me.essentials.IEssentials;
import com.pvpindex.factions.config.FactionsConfig;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

/** Resolves the active Essentials interop provider. */
public final class EssentialsInteropFactory {

    private EssentialsInteropFactory() {
    }

    public static EssentialsInterop create(
            final Plugin ownerPlugin,
            final FactionsConfig config,
            final Logger logger) {
        if (!config.isEssentialsXEnabled()) {
            logger.info("EssentialsX interop disabled in config.");
            return new NoopEssentialsInterop();
        }
        final PluginManager pm = ownerPlugin.getServer().getPluginManager();
        final Plugin essentials = pm.getPlugin("Essentials");
        if (essentials == null) {
            logger.info("EssentialsX not found — interop disabled.");
            return new NoopEssentialsInterop();
        }
        if (!(essentials instanceof IEssentials)) {
            logger.warning("Found plugin named 'Essentials' but it does not implement IEssentials"
                + " — interop disabled.");
            return new NoopEssentialsInterop();
        }
        logger.info("EssentialsX " + essentials.getDescription().getVersion()
            + " detected — home/warp teleport interop enabled.");
        return new EssentialsXInterop((IEssentials) essentials, logger);
    }
}


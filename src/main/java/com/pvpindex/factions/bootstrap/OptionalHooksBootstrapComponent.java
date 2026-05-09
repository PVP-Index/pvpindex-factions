package com.pvpindex.factions.bootstrap;

import com.pvpindex.factions.integration.dynmap.DynmapLayer;
import com.pvpindex.factions.integration.placeholderapi.FactionsPlaceholders;

/**
 * Initializes optional plugin hooks that should not block startup.
 */
public final class OptionalHooksBootstrapComponent extends AbstractBootstrapComponent {

    @Override
    public String name() {
        return "optional-hooks";
    }

    @Override
    public boolean start(final BootstrapContext context) {
        initPlaceholderApi(context);
        initDynmap(context);
        return true;
    }

    private void initPlaceholderApi(final BootstrapContext context) {
        if (context.plugin().getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            context.setPlaceholderApiEnabled(false);
            return;
        }

        new FactionsPlaceholders(context.infra().getRepositories(), logger(context)).register();
        logger(context).info("PlaceholderAPI hooked.");
        context.setPlaceholderApiEnabled(true);
    }

    private void initDynmap(final BootstrapContext context) {
        if (context.plugin().getServer().getPluginManager().getPlugin("dynmap") == null) {
            context.setDynmapEnabled(false);
            return;
        }

        try {
            final DynmapLayer layer = new DynmapLayer(context.infra().getRepositories(), logger(context));
            if (layer.start(context.plugin())) {
                logger(context).info("dynmap hooked - faction territory layer enabled.");
                context.setDynmapEnabled(true);
            } else {
                context.setDynmapEnabled(false);
            }
        } catch (Exception e) {
            logger(context).warning("Failed to hook dynmap: " + e.getMessage());
            context.setDynmapEnabled(false);
        }
    }
}

package com.pvpindex.factions.bootstrap;

import com.pvpindex.factions.integration.dynmap.DynmapLayer;
import com.pvpindex.factions.integration.ezcountdown.EzCountdownNotifier;
import com.pvpindex.factions.integration.placeholderapi.FactionsPlaceholders;
import com.pvpindex.factions.metrics.BStatsMetricsManager;

/**
 * Initializes optional plugin hooks that should not block startup.
 */
public final class OptionalHooksBootstrapComponent extends AbstractBootstrapComponent {

    private BStatsMetricsManager metricsManager;

    @Override
    public String name() {
        return "optional-hooks";
    }

    @Override
    public void stop(final BootstrapContext context) {
        if (metricsManager != null) {
            metricsManager.stop();
        }
    }

    @Override
    public boolean start(final BootstrapContext context) {
        initBstats(context);
        initPlaceholderApi(context);
        initDynmap(context);
        initEzCountdown(context);
        return true;
    }

    private void initBstats(final BootstrapContext context) {
        final int pluginId = context.infra().getConfig().getBstatsPluginId();
        if (!context.infra().getConfig().isBstatsEnabled()) {
            logger(context).info("bStats disabled in config.");
            return;
        }
        if (pluginId <= 0) {
            logger(context).warning("bStats enabled but plugin-id is invalid (<= 0). Skipping metrics bootstrap.");
            return;
        }

        try {
            metricsManager = new BStatsMetricsManager(
                context.plugin(),
                context.infra().getRepositories(),
                context.infra().getDatabaseConfig(),
                context.infra().getTaskScheduler(),
                logger(context)
            );
            metricsManager.start(pluginId);
            logger(context).info("bStats metrics hooked.");
        } catch (Exception e) {
            logger(context).warning("Failed to hook bStats: " + e.getMessage());
        }
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
            final DynmapLayer layer = new DynmapLayer(
                context.infra().getRepositories(),
                context.infra().getTaskScheduler(),
                logger(context));
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

    private void initEzCountdown(final BootstrapContext context) {
        final EzCountdownNotifier notifier = new EzCountdownNotifier(logger(context));
        if (!context.infra().getNotificationsConfig().isEzCountdownEnabled() || !notifier.setup()) {
            logger(context).info("EzCountdown not found or disabled — faction announcements will use chat only.");
            context.infra().setEzCountdownNotifier(notifier);
            context.setEzCountdownEnabled(false);
            return;
        }
        context.infra().setEzCountdownNotifier(notifier);
        context.setEzCountdownEnabled(true);
        logger(context).info("EzCountdown hooked — faction relation announcements enabled.");
    }
}

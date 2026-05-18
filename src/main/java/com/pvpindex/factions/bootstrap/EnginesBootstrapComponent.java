package com.pvpindex.factions.bootstrap;

import com.github.ezplugins.updater.ChainedUpdateChecker;
import com.github.ezplugins.updater.ModrinthUpdateChecker;
import com.github.ezplugins.updater.UpdateChecker;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.config.NotificationsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.engine.EngineChat;
import com.pvpindex.factions.engine.EngineAutoTerritory;
import com.pvpindex.factions.engine.AutoTerritoryModeCache;
import com.pvpindex.factions.engine.EngineChunkChange;
import com.pvpindex.factions.engine.EngineEconomy;
import com.pvpindex.factions.engine.EngineNotifications;
import com.pvpindex.factions.engine.EnginePlayerMove;
import com.pvpindex.factions.engine.EnginePower;
import com.pvpindex.factions.engine.EngineProtection;
import com.pvpindex.factions.engine.EngineUpdateNotifier;
import com.pvpindex.factions.gui.FactionsGuiManager;
import com.pvpindex.factions.integration.vault.VaultEconomy;
import com.pvpindex.factions.scheduler.TaskScheduler;
import com.pvpindex.factions.update.UpdateNotificationManager;
import java.util.List;

/**
 * Registers listeners and long-running gameplay engines.
 */
public final class EnginesBootstrapComponent extends AbstractBootstrapComponent {

    private EnginePower powerEngine;

    @Override
    public String name() {
        return "engines";
    }

    @Override
    public boolean start(final BootstrapContext context) {
        final Repositories repos = context.infra().getRepositories();
        final FactionsConfig cfg = context.infra().getConfig();
        final NotificationsConfig notifCfg = context.infra().getNotificationsConfig();
        final VaultEconomy vault = context.infra().getVaultEconomy();
        final TaskScheduler scheduler = context.infra().getTaskScheduler();

        final EngineChunkChange chunkChange = new EngineChunkChange(repos, cfg, logger(context));
        final EngineEconomy economy = new EngineEconomy(
            context.plugin(), repos, cfg, notifCfg, vault, scheduler, logger(context));
        final AutoTerritoryModeCache autoTerritoryModeCache = new AutoTerritoryModeCache(repos, logger(context));
        context.engines().setChunkChange(chunkChange);
        context.engines().setEconomy(economy);
        context.engines().setAutoTerritoryModeCache(autoTerritoryModeCache);
        economy.startTaxScheduler(scheduler);

        final EngineProtection protection = new EngineProtection(repos, cfg, logger(context));
        protection.register(context.plugin());

        final EnginePlayerMove playerMove = new EnginePlayerMove(repos, cfg, logger(context));
        playerMove.register(context.plugin());

        final EngineChat chat = new EngineChat(repos, cfg, logger(context));
        chat.register(context.plugin());

        powerEngine = new EnginePower(repos, cfg, logger(context), scheduler);
        powerEngine.start(context.plugin());

        final EngineNotifications notifications = new EngineNotifications(
            context.services().getInviteService(),
            context.services().getFactionService(),
            repos,
            logger(context),
            notifCfg);
        notifications.register(context.plugin(), scheduler);

        final EngineAutoTerritory autoTerritory =
            new EngineAutoTerritory(chunkChange, context.infra().getTerritoryGuard(), autoTerritoryModeCache);
        autoTerritory.register(context.plugin());

        if (cfg.isUpdateCheckEnabled()) {
            final String version = context.plugin().getDescription().getVersion();
            final ChainedUpdateChecker checker = ChainedUpdateChecker.builder()
                .primary(ModrinthUpdateChecker.builder(cfg.getUpdateModrinthSlug(), version)
                    .loaders(List.of("paper"))
                    .build())
                .backup(UpdateChecker.builder(cfg.getUpdateGithubOwner(), cfg.getUpdateGithubRepo(), version).build())
                .build();
            final UpdateNotificationManager updateManager = new UpdateNotificationManager(checker, logger(context));
            updateManager.checkAsync();
            final EngineUpdateNotifier updateNotifier = new EngineUpdateNotifier(cfg, updateManager);
            updateNotifier.register(context.plugin());
        }

        final FactionsGuiManager guiManager = new FactionsGuiManager(
            context.plugin(),
            context.infra().getGuiConfig(),
            repos,
            context.services().getFactionService(),
            cfg,
            logger(context));
        guiManager.register();
        context.engines().setFactionsGuiManager(guiManager);

        return true;
    }

    @Override
    public void stop(final BootstrapContext context) {
        if (powerEngine != null) {
            powerEngine.stop();
        }
        final EngineEconomy economy = context.engines().getEconomy();
        if (economy != null) {
            economy.stopTaxScheduler();
        }
    }
}

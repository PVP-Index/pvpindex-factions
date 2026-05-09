package com.gyvex.pvpindex.factions.bootstrap;

import com.gyvex.pvpindex.factions.config.FactionsConfig;
import com.gyvex.pvpindex.factions.data.Repositories;
import com.gyvex.pvpindex.factions.engine.EngineChat;
import com.gyvex.pvpindex.factions.engine.EngineAutoTerritory;
import com.gyvex.pvpindex.factions.engine.AutoTerritoryModeCache;
import com.gyvex.pvpindex.factions.engine.EngineChunkChange;
import com.gyvex.pvpindex.factions.engine.EngineEconomy;
import com.gyvex.pvpindex.factions.engine.EngineNotifications;
import com.gyvex.pvpindex.factions.engine.EnginePlayerMove;
import com.gyvex.pvpindex.factions.engine.EnginePower;
import com.gyvex.pvpindex.factions.engine.EngineProtection;
import com.gyvex.pvpindex.factions.gui.FactionsGuiManager;
import com.gyvex.pvpindex.factions.integration.vault.VaultEconomy;

/**
 * Registers listeners and long-running gameplay engines.
 */
public final class EnginesBootstrapComponent extends AbstractBootstrapComponent {

    @Override
    public String name() {
        return "engines";
    }

    @Override
    public boolean start(final BootstrapContext context) {
        final Repositories repos = context.infra().getRepositories();
        final FactionsConfig cfg = context.infra().getConfig();
        final VaultEconomy vault = context.infra().getVaultEconomy();

        final EngineChunkChange chunkChange = new EngineChunkChange(repos, cfg, logger(context));
        final EngineEconomy economy = new EngineEconomy(context.plugin(), repos, cfg, vault, logger(context));
        final AutoTerritoryModeCache autoTerritoryModeCache = new AutoTerritoryModeCache(repos, logger(context));
        context.engines().setChunkChange(chunkChange);
        context.engines().setEconomy(economy);
        context.engines().setAutoTerritoryModeCache(autoTerritoryModeCache);
        economy.startTaxScheduler(context.plugin());

        final EngineProtection protection = new EngineProtection(repos, cfg, logger(context));
        protection.register(context.plugin());

        final EnginePlayerMove playerMove = new EnginePlayerMove(repos, cfg, logger(context));
        playerMove.register(context.plugin());

        final EngineChat chat = new EngineChat(repos, cfg, logger(context));
        chat.register(context.plugin());

        final EnginePower power = new EnginePower(repos, cfg, logger(context));
        power.start(context.plugin());

        final EngineNotifications notifications = new EngineNotifications(
            context.services().getInviteService(),
            context.services().getFactionService(),
            repos,
            logger(context));
        notifications.register(context.plugin());

        final EngineAutoTerritory autoTerritory =
            new EngineAutoTerritory(chunkChange, context.infra().getTerritoryGuard(), autoTerritoryModeCache);
        autoTerritory.register(context.plugin());

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
        final EngineEconomy economy = context.engines().getEconomy();
        if (economy != null) {
            economy.stopTaxScheduler();
        }
    }
}

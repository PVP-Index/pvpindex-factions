package com.pvpindex.factions.bootstrap;

import com.pvpindex.factions.config.DatabaseConfig;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.config.GuiConfig;
import com.pvpindex.factions.config.MessagesConfig;
import com.pvpindex.factions.config.NotificationsConfig;
import com.pvpindex.factions.data.DatabaseManager;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.integration.essentials.EssentialsInteropFactory;
import com.pvpindex.factions.integration.lwc.LwcInterop;
import com.pvpindex.factions.integration.lwc.LwcInteropFactory;
import com.pvpindex.factions.integration.lwc.NoopLwcInterop;
import com.pvpindex.factions.integration.vault.VaultEconomy;
import com.pvpindex.factions.integration.worldguard.TerritoryGuardFactory;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.scheduler.BukkitTaskScheduler;
import com.pvpindex.factions.scheduler.FoliaTaskScheduler;
import com.pvpindex.factions.scheduler.PlatformDetector;
import com.pvpindex.factions.scheduler.TaskScheduler;
import com.pvpindex.factions.util.MsgUtil;
import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Initializes config, database and foundational integrations.
 */
public final class InfrastructureBootstrapComponent extends AbstractBootstrapComponent {

    @Override
    public String name() {
        return "infrastructure";
    }

    @Override
    public boolean start(final BootstrapContext context) {
        initScheduler(context);
        if (!initConfig(context)) {
            return false;
        }
        if (!initDatabase(context)) {
            return false;
        }
        initVault(context);
        initEssentialsInterop(context);
        initTerritoryGuard(context);
        initLwcInterop(context);
        return true;
    }

    @Override
    public void stop(final BootstrapContext context) {
        context.infra().getLwcInterop().unregister();
        final DatabaseManager db = context.infra().getDatabaseManager();
        if (db != null) {
            db.close();
        }
    }

    private boolean initConfig(final BootstrapContext context) {
        context.javaPlugin().saveDefaultConfig();
        context.infra().setConfig(new FactionsConfig(context.plugin().getConfig()));
        final File guiFile = new File(context.plugin().getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            context.javaPlugin().saveResource("gui.yml", false);
        }
        context.infra().setGuiConfig(new GuiConfig(YamlConfiguration.loadConfiguration(guiFile)));
        final File dbFile = new File(context.plugin().getDataFolder(), "database.yml");
        if (!dbFile.exists()) {
            context.javaPlugin().saveResource("database.yml", false);
        }
        final File messagesFile = new File(context.plugin().getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            context.javaPlugin().saveResource("messages.yml", false);
        }
        final FileConfiguration msgCfgRaw = YamlConfiguration.loadConfiguration(messagesFile);
        final MessagesConfig messagesConfig = new MessagesConfig(msgCfgRaw);
        context.infra().setMessagesConfig(messagesConfig);
        MsgUtil.setMessagesConfig(messagesConfig);
        final FileConfiguration dbCfgRaw = YamlConfiguration.loadConfiguration(dbFile);
        context.infra().setDatabaseConfig(new DatabaseConfig(dbCfgRaw));
        final PredefinedConfigManager predefined = new PredefinedConfigManager(context.plugin().getDataFolder(), logger(context));
        predefined.initialize();
        PredefinedConfigManager.setInstance(predefined);
        context.infra().setPredefinedConfigManager(predefined);
        final File notifFile = new File(context.plugin().getDataFolder(), "notifications.yml");
        if (!notifFile.exists()) {
            context.javaPlugin().saveResource("notifications.yml", false);
        }
        context.infra().setNotificationsConfig(
            new NotificationsConfig(YamlConfiguration.loadConfiguration(notifFile)));
        return true;
    }

    private boolean initDatabase(final BootstrapContext context) {
        final DatabaseConfig dbCfg = context.infra().getDatabaseConfig();
        final DatabaseManager db = new DatabaseManager();
        db.initialize(dbCfg, context.plugin().getDataFolder(), logger(context));
        if (!db.isInitialized()) {
            logger(context).severe("Failed to initialise the database. Disabling plugin.");
            return false;
        }
        context.infra().setDatabaseManager(db);
        context.infra().setRepositories(new Repositories(db.getStore()));
        return true;
    }

    private void initVault(final BootstrapContext context) {
        final VaultEconomy vault = new VaultEconomy(logger(context));
        final boolean hooked = vault.setup();
        if (!hooked) {
            logger(context).warning("Vault not found — economy features will be disabled.");
            context.setVaultEnabled(false);
        } else {
            logger(context).info("Vault found — economy provider will be resolved on first use.");
            context.setVaultEnabled(true);
        }
        context.infra().setVaultEconomy(vault);
    }

    private void initEssentialsInterop(final BootstrapContext context) {
        final FactionsConfig cfg = context.infra().getConfig();
        context.infra().setEssentialsInterop(
            EssentialsInteropFactory.create(context.plugin(), cfg, logger(context))
        );
    }

    private void initTerritoryGuard(final BootstrapContext context) {
        final FactionsConfig cfg = context.infra().getConfig();
        context.infra().setTerritoryGuard(
            TerritoryGuardFactory.create(context.plugin(), cfg, logger(context))
        );
    }

    private void initLwcInterop(final BootstrapContext context) {
        final FactionsConfig cfg = context.infra().getConfig();
        final Repositories repos = context.infra().getRepositories();
        final TaskScheduler scheduler = context.infra().getTaskScheduler();
        final LwcInterop lwcInterop = LwcInteropFactory.create(
            context.plugin(), cfg, repos, scheduler, logger(context));
        context.infra().setLwcInterop(lwcInterop);
        lwcInterop.register(context.plugin());
        context.setLwcEnabled(!(lwcInterop instanceof NoopLwcInterop));
    }

    private void initScheduler(final BootstrapContext context) {
        final TaskScheduler scheduler = PlatformDetector.isFolia()
            ? new FoliaTaskScheduler(context.plugin())
            : new BukkitTaskScheduler(context.plugin());
        context.infra().setTaskScheduler(scheduler);
        logger(context).info("Platform scheduler: "
            + (PlatformDetector.isFolia() ? "Folia" : "Bukkit"));
    }
}

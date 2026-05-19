package com.pvpindex.factions.registry;

import com.pvpindex.factions.config.DatabaseConfig;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.config.GuiConfig;
import com.pvpindex.factions.config.MessagesConfig;
import com.pvpindex.factions.config.NotificationsConfig;
import com.pvpindex.factions.data.DatabaseManager;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.integration.discordsrv.DiscordSrvNotifier;
import com.pvpindex.factions.integration.essentials.EssentialsInterop;
import com.pvpindex.factions.integration.essentials.NoopEssentialsInterop;
import com.pvpindex.factions.integration.ezcountdown.EzCountdownNotifier;
import com.pvpindex.factions.integration.lwc.LwcInterop;
import com.pvpindex.factions.integration.lwc.NoopLwcInterop;
import com.pvpindex.factions.integration.vault.VaultEconomy;
import com.pvpindex.factions.integration.worldguard.NoopTerritoryGuard;
import com.pvpindex.factions.integration.worldguard.TerritoryGuard;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.scheduler.TaskScheduler;

/**
 * Holds core infrastructure instances: config, database, repositories, and Vault.
 *
 * <p>Populated during the first phase of
 * {@link com.pvpindex.factions.Bootstrap#start()}.
 */
public class InfraRegistry {

    private FactionsConfig config;
    private GuiConfig guiConfig;
    private MessagesConfig messagesConfig;
    private DatabaseConfig databaseConfig;
    private NotificationsConfig notificationsConfig;
    private DatabaseManager databaseManager;
    private Repositories repositories;
    private VaultEconomy vaultEconomy;
    private EzCountdownNotifier ezCountdownNotifier;
    private EssentialsInterop essentialsInterop = new NoopEssentialsInterop();
    private DiscordSrvNotifier discordSrvNotifier;
    private TerritoryGuard territoryGuard = new NoopTerritoryGuard();
    private LwcInterop lwcInterop = new NoopLwcInterop();
    private PredefinedConfigManager predefinedConfigManager;
    private TaskScheduler taskScheduler;

    public void setConfig(final FactionsConfig config) { this.config = config; }
    public void setGuiConfig(final GuiConfig guiConfig) { this.guiConfig = guiConfig; }
    public void setMessagesConfig(final MessagesConfig messagesConfig) { this.messagesConfig = messagesConfig; }
    public void setDatabaseConfig(final DatabaseConfig databaseConfig) { this.databaseConfig = databaseConfig; }
    public void setNotificationsConfig(final NotificationsConfig cfg) { this.notificationsConfig = cfg; }
    public void setDatabaseManager(final DatabaseManager manager) { this.databaseManager = manager; }
    public void setRepositories(final Repositories repositories) { this.repositories = repositories; }
    public void setVaultEconomy(final VaultEconomy economy) { this.vaultEconomy = economy; }
    public void setEzCountdownNotifier(final EzCountdownNotifier notifier) { this.ezCountdownNotifier = notifier; }
    public void setEssentialsInterop(final EssentialsInterop interop) { this.essentialsInterop = interop; }
    public void setDiscordSrvNotifier(final DiscordSrvNotifier notifier) { this.discordSrvNotifier = notifier; }
    public void setTerritoryGuard(final TerritoryGuard territoryGuard) { this.territoryGuard = territoryGuard; }
    public void setLwcInterop(final LwcInterop lwcInterop) { this.lwcInterop = lwcInterop; }
    public void setPredefinedConfigManager(final PredefinedConfigManager predefinedConfigManager) {
        this.predefinedConfigManager = predefinedConfigManager;
    }

    public void setTaskScheduler(final TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public FactionsConfig getConfig() { return config; }
    public GuiConfig getGuiConfig() { return guiConfig; }
    public MessagesConfig getMessagesConfig() { return messagesConfig; }
    public DatabaseConfig getDatabaseConfig() { return databaseConfig; }
    public NotificationsConfig getNotificationsConfig() { return notificationsConfig; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public Repositories getRepositories() { return repositories; }
    public VaultEconomy getVaultEconomy() { return vaultEconomy; }
    public EzCountdownNotifier getEzCountdownNotifier() { return ezCountdownNotifier; }
    public EssentialsInterop getEssentialsInterop() { return essentialsInterop; }
    public DiscordSrvNotifier getDiscordSrvNotifier() { return discordSrvNotifier; }
    public TerritoryGuard getTerritoryGuard() { return territoryGuard; }
    public LwcInterop getLwcInterop() { return lwcInterop; }
    public PredefinedConfigManager getPredefinedConfigManager() { return predefinedConfigManager; }

    public TaskScheduler getTaskScheduler() { return taskScheduler; }
}

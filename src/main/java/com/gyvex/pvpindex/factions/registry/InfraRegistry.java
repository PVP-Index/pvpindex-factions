package com.gyvex.pvpindex.factions.registry;

import com.gyvex.pvpindex.factions.config.DatabaseConfig;
import com.gyvex.pvpindex.factions.config.FactionsConfig;
import com.gyvex.pvpindex.factions.config.GuiConfig;
import com.gyvex.pvpindex.factions.config.MessagesConfig;
import com.gyvex.pvpindex.factions.data.DatabaseManager;
import com.gyvex.pvpindex.factions.data.Repositories;
import com.gyvex.pvpindex.factions.integration.essentials.EssentialsInterop;
import com.gyvex.pvpindex.factions.integration.essentials.NoopEssentialsInterop;
import com.gyvex.pvpindex.factions.integration.lwc.LwcInterop;
import com.gyvex.pvpindex.factions.integration.lwc.NoopLwcInterop;
import com.gyvex.pvpindex.factions.integration.vault.VaultEconomy;
import com.gyvex.pvpindex.factions.integration.worldguard.NoopTerritoryGuard;
import com.gyvex.pvpindex.factions.integration.worldguard.TerritoryGuard;

/**
 * Holds core infrastructure instances: config, database, repositories, and Vault.
 *
 * <p>Populated during the first phase of
 * {@link com.gyvex.pvpindex.factions.Bootstrap#start()}.
 */
public class InfraRegistry {

    private FactionsConfig config;
    private GuiConfig guiConfig;
    private MessagesConfig messagesConfig;
    private DatabaseConfig databaseConfig;
    private DatabaseManager databaseManager;
    private Repositories repositories;
    private VaultEconomy vaultEconomy;
    private EssentialsInterop essentialsInterop = new NoopEssentialsInterop();
    private TerritoryGuard territoryGuard = new NoopTerritoryGuard();
    private LwcInterop lwcInterop = new NoopLwcInterop();

    public void setConfig(final FactionsConfig config) { this.config = config; }
    public void setGuiConfig(final GuiConfig guiConfig) { this.guiConfig = guiConfig; }
    public void setMessagesConfig(final MessagesConfig messagesConfig) { this.messagesConfig = messagesConfig; }
    public void setDatabaseConfig(final DatabaseConfig databaseConfig) { this.databaseConfig = databaseConfig; }
    public void setDatabaseManager(final DatabaseManager manager) { this.databaseManager = manager; }
    public void setRepositories(final Repositories repositories) { this.repositories = repositories; }
    public void setVaultEconomy(final VaultEconomy economy) { this.vaultEconomy = economy; }
    public void setEssentialsInterop(final EssentialsInterop interop) { this.essentialsInterop = interop; }
    public void setTerritoryGuard(final TerritoryGuard territoryGuard) { this.territoryGuard = territoryGuard; }
    public void setLwcInterop(final LwcInterop lwcInterop) { this.lwcInterop = lwcInterop; }

    public FactionsConfig getConfig() { return config; }
    public GuiConfig getGuiConfig() { return guiConfig; }
    public MessagesConfig getMessagesConfig() { return messagesConfig; }
    public DatabaseConfig getDatabaseConfig() { return databaseConfig; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public Repositories getRepositories() { return repositories; }
    public VaultEconomy getVaultEconomy() { return vaultEconomy; }
    public EssentialsInterop getEssentialsInterop() { return essentialsInterop; }
    public TerritoryGuard getTerritoryGuard() { return territoryGuard; }
    public LwcInterop getLwcInterop() { return lwcInterop; }
}

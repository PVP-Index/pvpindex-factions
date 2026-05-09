package com.pvpindex.factions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.repository.FactionRepository;
import com.pvpindex.factions.data.repository.PlayerRepository;
import com.pvpindex.factions.engine.EngineEconomy;
import com.pvpindex.factions.integration.vault.VaultEconomy;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BankTest {

    @Mock private Repositories repos;
    @Mock private FactionRepository factionRepo;
    @Mock private FactionsConfig config;
    @Mock private VaultEconomy vaultEconomy;
    @Mock private Plugin plugin;
    @Mock private Player player;
    @Mock private Server mockServer;
    @Mock private PluginManager mockPluginManager;
    @Mock private PlayerRepository playerRepo;

    private EngineEconomy engine;
    private FactionModel faction;
    private final String factionId = UUID.randomUUID().toString();

    private static ArgumentMatcher<Component> componentContains(final String text) {
        return comp -> PlainTextComponentSerializer.plainText().serialize(comp).contains(text);
    }

    @BeforeEach
    void setUp() throws Exception {
        setBukkitServer(mockServer);
        lenient().when(mockServer.getPluginManager()).thenReturn(mockPluginManager);

        engine = new EngineEconomy(plugin, repos, config, vaultEconomy, Logger.getLogger("test"));
        faction = new FactionModel(factionId);
        faction.setBank(500.0);
        // lenient: not all tests reach the repo lookup path
        lenient().when(repos.factions()).thenReturn(factionRepo);
        lenient().when(factionRepo.find(factionId)).thenReturn(Optional.of(faction));
        lenient().when(config.isBankEnabled()).thenReturn(true);
        lenient().when(vaultEconomy.isEnabled()).thenReturn(true);
        lenient().when(config.isTaxNotifyMembersEnabled()).thenReturn(false);
        lenient().when(repos.players()).thenReturn(playerRepo);
    }

    @AfterEach
    void tearDown() throws Exception {
        setBukkitServer(null);
    }

    private static void setBukkitServer(final Server server) throws Exception {
        final Field f = Bukkit.class.getDeclaredField("server");
        f.setAccessible(true);
        f.set(null, server);
    }

    @Test
    void depositSuccess() throws Exception {
        when(vaultEconomy.getBalance(player)).thenReturn(200.0);
        when(vaultEconomy.withdraw(player, 100.0)).thenReturn(true);

        final boolean result = engine.deposit(player, factionId, 100.0);

        assertTrue(result);
        assertEquals(600.0, faction.getBank());
        verify(factionRepo).save(faction);
        verify(player).sendMessage(argThat(componentContains("Deposited")));
    }

    @Test
    void depositRejectsNegativeAmount() {
        final boolean result = engine.deposit(player, factionId, -50.0);
        assertFalse(result);
        verify(player).sendMessage(argThat(componentContains("positive")));
    }

    @Test
    void depositFailsWhenInsufficientFunds() throws Exception {
        when(vaultEconomy.getBalance(player)).thenReturn(50.0);
        final boolean result = engine.deposit(player, factionId, 100.0);
        assertFalse(result);
        verify(factionRepo, never()).save(any());
    }

    @Test
    void withdrawSuccess() throws Exception {
        when(vaultEconomy.deposit(player, 200.0)).thenReturn(true);
        final boolean result = engine.withdraw(player, factionId, 200.0);
        assertTrue(result);
        assertEquals(300.0, faction.getBank());
        verify(player).sendMessage(argThat(componentContains("Withdrew")));
    }

    @Test
    void withdrawFailsWhenBankInsufficientFunds() throws Exception {
        final boolean result = engine.withdraw(player, factionId, 1000.0);
        assertFalse(result);
        verify(player).sendMessage(argThat(componentContains("enough")));
    }

    @Test
    void withdrawRejectsNegativeAmount() {
        final boolean result = engine.withdraw(player, factionId, 0.0);
        assertFalse(result);
        verify(player).sendMessage(argThat(componentContains("positive")));
    }

    @Test
    void depositFailsWhenVaultDisabled() throws Exception {
        when(vaultEconomy.isEnabled()).thenReturn(false);
        final boolean result = engine.deposit(player, factionId, 100.0);
        assertFalse(result);
        verify(factionRepo, never()).save(any());
    }

    @Test
    void applyFactionTaxesNowChargesConfiguredRate() throws Exception {
        when(config.isTaxEnabled()).thenReturn(true);
        when(config.getTaxRate()).thenReturn(0.10D);
        when(config.getTaxMinBankBalance()).thenReturn(0.0D);
        when(config.getTaxMinChargeAmount()).thenReturn(0.01D);
        when(factionRepo.findAll()).thenReturn(java.util.List.of(faction));

        final int taxed = engine.applyFactionTaxesNow();

        assertEquals(1, taxed);
        assertEquals(450.0D, faction.getBank());
        verify(factionRepo).save(faction);
    }
}

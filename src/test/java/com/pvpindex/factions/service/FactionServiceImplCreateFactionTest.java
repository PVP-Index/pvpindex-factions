package com.pvpindex.factions.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import com.pvpindex.factions.config.DatabaseConfig;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.DatabaseManager;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Integration test for {@link FactionServiceImpl#createFaction} against a real H2 database.
 *
 * <p>Regression test for the NOT NULL constraint violation (H2 error 23502) that occurred
 * because {@link FactionModel}'s constructor did not initialise {@code is_raidable} and
 * {@code shield_duration_hours}. Jaloquent serialises all registered columns in the INSERT
 * statement, so any unset column is passed as NULL regardless of the column DEFAULT.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FactionServiceImpl createFaction — integration (real H2)")
class FactionServiceImplCreateFactionTest {

    @TempDir
    File tempDir;

    @Mock
    private FileConfiguration fileCfg;
    @Mock
    private Plugin plugin;
    @Mock
    private FactionsConfig config;
    @Mock
    private Server mockServer;
    @Mock
    private PluginManager mockPluginManager;

    private DatabaseManager dbManager;
    private Repositories repos;
    private FactionServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(fileCfg.getString("type", "h2")).thenReturn("h2");
        lenient().when(fileCfg.getString("h2.file", "data/factions")).thenReturn("factions");
        final DatabaseConfig dbConfig = new DatabaseConfig(fileCfg);

        dbManager = new DatabaseManager();
        dbManager.initialize(dbConfig, tempDir, Logger.getLogger("test"));
        repos = new Repositories(dbManager.getStore());

        setBukkitServer(mockServer);
        lenient().when(mockServer.getPluginManager()).thenReturn(mockPluginManager);

        service = new FactionServiceImpl(plugin, repos, config, Logger.getLogger("test"));
        PredefinedConfigManager.setInstance(null);
    }

    @AfterEach
    void tearDown() throws Exception {
        PredefinedConfigManager.setInstance(null);
        setBukkitServer(null);
        if (dbManager.isInitialized()) {
            dbManager.close();
        }
    }

    private static void setBukkitServer(final Server server) throws Exception {
        final Field f = Bukkit.class.getDeclaredField("server");
        f.setAccessible(true);
        f.set(null, server);
    }

    @Test
    @DisplayName("createFaction — persists without NOT NULL violation (regression: H2 error 23502)")
    void createFactionPersistsWithoutNotNullViolation() {
        final UUID ownerUUID = UUID.randomUUID();

        final Optional<FactionModel> result = service.createFaction("Alpha", ownerUUID);

        assertTrue(result.isPresent(), "createFaction must return a faction");
        final FactionModel saved = result.get();
        assertEquals("Alpha", saved.getName());
        assertEquals(ownerUUID.toString(), saved.getOwnerId());
        assertFalse(saved.isRaidable(), "is_raidable must default to false");
        assertEquals(0, saved.getShieldDurationHours(), "shield_duration_hours must default to 0");
    }

    @Test
    @DisplayName("createFaction — faction is readable back from the database")
    void createFactionIsReadableFromDatabase() throws Exception {
        final UUID ownerUUID = UUID.randomUUID();

        final Optional<FactionModel> created = service.createFaction("Beta", ownerUUID);

        assertTrue(created.isPresent());
        final String factionId = created.get().getId();
        final Optional<FactionModel> loaded = repos.factions().find(factionId);
        assertTrue(loaded.isPresent(), "Faction must be readable from the database after creation");
        assertEquals("Beta", loaded.get().getName());
        assertEquals(ownerUUID.toString(), loaded.get().getOwnerId());
    }

    @Test
    @DisplayName("createFaction — duplicate name returns empty without saving")
    void createFactionDuplicateNameReturnsEmpty() {
        final UUID ownerUUID = UUID.randomUUID();
        service.createFaction("Gamma", ownerUUID);

        final Optional<FactionModel> duplicate = service.createFaction("Gamma", UUID.randomUUID());

        assertFalse(duplicate.isPresent(), "Duplicate faction name must be rejected");
    }
}

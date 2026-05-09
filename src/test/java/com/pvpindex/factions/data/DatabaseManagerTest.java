package com.pvpindex.factions.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.config.DatabaseConfig;
import java.io.File;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DatabaseManager — lifecycle and driver detection")
class DatabaseManagerTest {

    @TempDir
    File tempDir;

    @Mock
    private FileConfiguration fileCfg;

    private DatabaseConfig config;
    private final Logger logger = Logger.getLogger("test");
    private DatabaseManager manager;

    @BeforeEach
    void setUp() {
        when(fileCfg.getString("type", "h2")).thenReturn("h2");
        when(fileCfg.getString("h2.file", "data/factions")).thenReturn("test/data");
        config = new DatabaseConfig(fileCfg);
        manager = new DatabaseManager();
    }

    @AfterEach
    void tearDown() {
        if (manager.isInitialized()) {
            manager.close();
        }
    }

    @Test
    @DisplayName("not initialized by default")
    void testNotInitializedByDefault() {
        assertFalse(manager.isInitialized());
    }

    @Test
    @DisplayName("initialize with H2 — isInitialized returns true")
    void testInitializeH2() {
        manager.initialize(config, tempDir, logger);

        assertTrue(manager.isInitialized());
    }

    @Test
    @DisplayName("getStore — returns non-null store after init")
    void testGetStoreAfterInit() {
        manager.initialize(config, tempDir, logger);

        assertNotNull(manager.getStore());
    }

    @Test
    @DisplayName("getStore — throws before init")
    void testGetStoreBeforeInit() {
        assertThrows(IllegalStateException.class, () -> manager.getStore());
    }

    @Test
    @DisplayName("close — isInitialized returns false after close")
    void testCloseAfterInit() {
        manager.initialize(config, tempDir, logger);
        manager.close();

        assertFalse(manager.isInitialized());
    }

    @Test
    @DisplayName("close — safe to call when not initialized")
    void testCloseSafe() {
        // Should not throw
        manager.close();
    }

    @Test
    @DisplayName("detectH2DriverClass — returns org.h2.Driver in test context (unshaded)")
    void testDetectH2DriverClass() {
        final String driverClass = DatabaseManager.detectH2DriverClass();

        assertEquals("org.h2.Driver", driverClass);
    }

    @Test
    @DisplayName("detectMysqlDriverClass — returns mysql driver class in test context")
    void testDetectMysqlDriverClass() {
        final String driverClass = DatabaseManager.detectMysqlDriverClass();

        assertTrue(driverClass.contains("mysql"),
            "Expected mysql driver class but got: " + driverClass);
    }

    @Test
    @DisplayName("close — double close does not throw")
    void testDoubleClose() {
        manager.initialize(config, tempDir, logger);
        manager.close();
        manager.close();
    }
}

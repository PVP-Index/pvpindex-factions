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
import com.pvpindex.factions.data.model.PlayerModel;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FactionServiceImpl role management")
class FactionServiceImplRoleManagementTest {

    @TempDir
    File tempDir;

    @Mock private FileConfiguration fileCfg;
    @Mock private Plugin plugin;
    @Mock private FactionsConfig config;
    @Mock private Server server;
    @Mock private PluginManager pluginManager;
    @Mock private OfflinePlayer offlinePlayer;

    private DatabaseManager dbManager;
    private Repositories repos;
    private FactionServiceImpl service;
    private UUID ownerId;
    private UUID memberId;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(fileCfg.getString("type", "h2")).thenReturn("h2");
        lenient().when(fileCfg.getString("h2.file", "data/factions")).thenReturn("factions");
        dbManager = new DatabaseManager();
        dbManager.initialize(new DatabaseConfig(fileCfg), tempDir, Logger.getLogger("test"));
        repos = new Repositories(dbManager.getStore());
        setBukkitServer(server);
        lenient().when(server.getPluginManager()).thenReturn(pluginManager);
        lenient().when(server.getOfflinePlayer(org.mockito.ArgumentMatchers.any(UUID.class))).thenReturn(offlinePlayer);
        lenient().when(offlinePlayer.getName()).thenReturn("TestPlayer");
        lenient().when(config.isCustomRolesEnabled()).thenReturn(true);
        lenient().when(config.isRoleFactionOverridesEnabled()).thenReturn(true);
        lenient().when(config.getMinCustomRolePriority()).thenReturn(11);
        lenient().when(config.getMaxCustomRolePriority()).thenReturn(99);
        lenient().when(config.getMaxCustomRolesPerFaction()).thenReturn(0);
        lenient().when(config.isRolePrefixesEnabled()).thenReturn(true);
        lenient().when(config.getMaxRolePrefixLength()).thenReturn(32);
        service = new FactionServiceImpl(plugin, repos, config, Logger.getLogger("test"));

        ownerId = UUID.randomUUID();
        memberId = UUID.randomUUID();
        final Optional<FactionModel> created = service.createFaction("Roles", ownerId);
        assertTrue(created.isPresent());
        assertTrue(service.joinFaction(created.get().getId(), memberId));
    }

    @AfterEach
    void tearDown() throws Exception {
        setBukkitServer(null);
        if (dbManager != null && dbManager.isInitialized()) {
            dbManager.close();
        }
    }

    @Test
    @DisplayName("create+assign+delete role happy path with in-use guard")
    void createAssignDeleteRoleLifecycle() throws Exception {
        assertEquals(CreateRoleResult.SUCCESS, service.createRole(ownerId, "Scout", 30, "<gray>[S]</gray>"));
        assertTrue(service.assignRole(ownerId, memberId, "Scout"));

        final PlayerModel member = repos.players().find(memberId.toString()).orElseThrow();
        final String scoutRankId = member.getRankId();
        assertTrue(scoutRankId != null && !scoutRankId.isBlank());

        assertFalse(service.deleteRole(ownerId, "Scout"));

        final String factionId = service.getFactionByPlayer(ownerId).orElseThrow().getId();
        final String memberRankId = repos.ranks().findDefaultRank(factionId).orElseThrow().getId();
        member.setRankId(memberRankId);
        repos.players().save(member);
        assertTrue(service.deleteRole(ownerId, "Scout"));
    }

    @Test
    @DisplayName("protected core roles cannot be renamed")
    void protectedRoleRenameBlocked() {
        assertFalse(service.renameRole(ownerId, "Owner", "Boss"));
    }

    @Test
    @DisplayName("priority bounds enforced for custom roles")
    void priorityBoundsEnforced() {
        assertEquals(CreateRoleResult.PRIORITY_OUT_OF_RANGE, service.createRole(ownerId, "TooLow", 10, null));
        assertEquals(CreateRoleResult.PRIORITY_OUT_OF_RANGE, service.createRole(ownerId, "TooHigh", 100, null));
        assertEquals(CreateRoleResult.SUCCESS, service.createRole(ownerId, "Valid", 60, null));
        assertEquals(4, service.listRoles(ownerId).size());
    }

    private static void setBukkitServer(final Server newServer) throws Exception {
        final Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(null, newServer);
    }
}

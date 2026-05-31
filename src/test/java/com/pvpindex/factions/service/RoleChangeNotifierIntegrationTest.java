package com.pvpindex.factions.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import com.pvpindex.factions.api.RoleChangeNotifier;
import com.pvpindex.factions.api.RoleChangeNotifierHolder;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.RankModel;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RoleChangeNotifier — integration")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class RoleChangeNotifierIntegrationTest extends com.pvpindex.factions.command.CommandTestBase {

    private Server server;
    private OfflinePlayer offlineMember;

    @BeforeEach
    void setUp() throws Exception {
        // Provide a Bukkit Server so OfflinePlayer lookups succeed for resolveName.
        server = org.mockito.Mockito.mock(Server.class);
        final PluginManager pm = org.mockito.Mockito.mock(PluginManager.class);
        org.mockito.Mockito.lenient().when(server.getPluginManager()).thenReturn(pm);
        offlineMember = org.mockito.Mockito.mock(OfflinePlayer.class);
        org.mockito.Mockito.lenient().when(offlineMember.getName()).thenReturn("member");
        org.mockito.Mockito.lenient().when(server.getOfflinePlayer("member")).thenReturn(offlineMember);
        setBukkitServer(server);
    }

    @AfterEach
    void tearDown() throws Exception {
        RoleChangeNotifierHolder.clearNotifier();
        setBukkitServer(null);
    }

    @StorageTest
    @DisplayName("notifier invoked for create/update/rename/delete")
    void notifierInvokedOnLifecycle() throws Exception {
        // Skip the pure Mockito mock run; we want real DB-backed behavior.
        Assumptions.assumeFalse(org.mockito.Mockito.mockingDetails(repos).isMock(), "Skip mock storage");

        // Counters for notifier calls
        final AtomicInteger created = new AtomicInteger(0);
        final AtomicInteger updated = new AtomicInteger(0);
        final AtomicInteger renamed = new AtomicInteger(0);
        final AtomicInteger deleted = new AtomicInteger(0);

        RoleChangeNotifierHolder.setNotifier(new RoleChangeNotifier() {
            @Override
            public void roleCreated(final RankModel rank) { created.incrementAndGet(); }
            @Override
            public void roleUpdated(final RankModel rank) { updated.incrementAndGet(); }
            @Override
            public void roleRenamed(final RankModel rank, final String oldName) { renamed.incrementAndGet(); }
            @Override
            public void roleDeleted(final RankModel rank) { deleted.incrementAndGet(); }
        });

        // Basic config/fixtures are provided by the inherited mocks
        lenient().when(config.isCustomRolesEnabled()).thenReturn(true);
        lenient().when(config.isRoleFactionOverridesEnabled()).thenReturn(true);
        lenient().when(config.getMinCustomRolePriority()).thenReturn(11);
        lenient().when(config.getMaxCustomRolePriority()).thenReturn(99);
        lenient().when(config.getMaxCustomRolesPerFaction()).thenReturn(0);
        lenient().when(config.isRolePrefixesEnabled()).thenReturn(true);
        lenient().when(config.getMaxRolePrefixLength()).thenReturn(32);
        lenient().when(config.getDefaultMemberRolePrefix()).thenReturn("");
        lenient().when(config.getDefaultOfficerRolePrefix()).thenReturn("");
        lenient().when(config.getDefaultOwnerRolePrefix()).thenReturn("");
        lenient().when(config.getMaxMembers()).thenReturn(0);

        final FactionServiceImpl service = new FactionServiceImpl(
            org.mockito.Mockito.mock(org.bukkit.plugin.Plugin.class), repos, config, logger);

        final UUID owner = UUID.randomUUID();

        final Optional<FactionModel> factionOpt = service.createFaction("Roles", owner);
        assertTrue(factionOpt.isPresent());
        final String factionId = factionOpt.get().getId();

        // join a member so the faction has at least one player (default rank)
        final UUID member = UUID.randomUUID();
        assertTrue(service.joinFaction(factionId, member));

        // create role -> notifier.roleCreated
        assertEquals(CreateRoleResult.SUCCESS, service.createRole(owner, "Scout", 30, null));
        assertEquals(1, created.get(), "roleCreated called once");

        // update priority -> notifier.roleUpdated
        assertTrue(service.setRolePriority(owner, "Scout", 35));
        assertEquals(1, updated.get(), "roleUpdated called once");

        // rename -> notifier.roleRenamed
        assertTrue(service.renameRole(owner, "Scout", "Scout2"));
        assertEquals(1, renamed.get(), "roleRenamed called once");

        // delete -> notifier.roleDeleted
        assertTrue(service.deleteRole(owner, "Scout2"));
        assertEquals(1, deleted.get(), "roleDeleted called once");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static void setBukkitServer(final Server newServer) throws Exception {
        final Field field = Bukkit.class.getDeclaredField("server");
        field.setAccessible(true);
        field.set(null, newServer);
    }
}

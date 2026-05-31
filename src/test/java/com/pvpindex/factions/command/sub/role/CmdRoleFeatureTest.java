package com.pvpindex.factions.command.sub.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.service.CreateRoleResult;
import com.pvpindex.factions.service.FactionServiceImpl;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
@DisplayName("CmdRole — feature tests (DB)")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class CmdRoleFeatureTest extends CommandTestBase {

    private UUID ownerId;
    private UUID memberId;

    private Server server;
    private PluginManager pluginManager;
    private OfflinePlayer offlineMember;

    @BeforeEach
    void setUp() throws Exception {
        ownerId = UUID.randomUUID();
        memberId = UUID.randomUUID();

        when(player.getUniqueId()).thenReturn(ownerId);

        // Basic config required by FactionServiceImpl
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

        // Provide a Bukkit Server so OfflinePlayer lookups succeed
        server = org.mockito.Mockito.mock(Server.class);
        pluginManager = org.mockito.Mockito.mock(PluginManager.class);
        org.mockito.Mockito.lenient().when(server.getPluginManager()).thenReturn(pluginManager);
        offlineMember = org.mockito.Mockito.mock(OfflinePlayer.class);
        org.mockito.Mockito.lenient().when(offlineMember.getUniqueId()).thenReturn(memberId);
        org.mockito.Mockito.lenient().when(offlineMember.getName()).thenReturn("member");
        org.mockito.Mockito.lenient().when(server.getOfflinePlayer("member")).thenReturn(offlineMember);
        org.mockito.Mockito.lenient().when(server.getOfflinePlayer(memberId)).thenReturn(offlineMember);
        setBukkitServer(server);
    }

    @AfterEach
    void tearDown() throws Exception {
        setBukkitServer(null);
    }

    // ---------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------

    @StorageTest
    @DisplayName("create role persists rank and sends success message")
    void createRoleCreatesPersistedRankAndSendsSuccessMessage() throws Exception {
        // Only exercise real DB backends — skip the Mockito mock Repositories run.
        Assumptions.assumeFalse(org.mockito.Mockito.mockingDetails(repos).isMock(), "Skip mock storage");

        final FactionServiceImpl service = new FactionServiceImpl(plugin, repos, config, logger);
        final var cmd = new CmdRoleCreate(service);

        final Optional<FactionModel> created = service.createFaction("Roles", ownerId);
        assertTrue(created.isPresent());
        final String factionId = created.get().getId();
        assertTrue(service.joinFaction(factionId, memberId));

        cmd.execute(ctx("Scout", "30", "<gray>[S]</gray>"));

        org.mockito.Mockito.verify(player).sendMessage(argThat(componentContains("Created role")));

        final List<RankModel> ranks = repos.ranks().findByFactionId(factionId);
        assertTrue(ranks.stream().anyMatch(r -> "Scout".equals(r.getName()) && r.getPriority() == 30));
    }

    @StorageTest
    @DisplayName("setpriority updates persisted rank and sends message")
    void setPriorityUpdatesRankAndSendsMessage() throws Exception {
        Assumptions.assumeFalse(org.mockito.Mockito.mockingDetails(repos).isMock(), "Skip mock storage");

        final FactionServiceImpl service = new FactionServiceImpl(plugin, repos, config, logger);
        final var createCmd = new CmdRoleCreate(service);
        final var setPriorityCmd = new CmdRoleSetPriority(service);

        final Optional<FactionModel> created = service.createFaction("Roles", ownerId);
        assertTrue(created.isPresent());
        final String factionId = created.get().getId();
        assertTrue(service.joinFaction(factionId, memberId));

        // create a role to modify
        assertEquals(CreateRoleResult.SUCCESS, service.createRole(ownerId, "Scout", 30, null));

        setPriorityCmd.execute(ctx("Scout", "40"));

        org.mockito.Mockito.verify(player).sendMessage(argThat(componentContains("Set role")));

        final RankModel updated = repos.ranks().findByFactionId(factionId).stream()
            .filter(r -> "Scout".equals(r.getName()))
            .findFirst().orElseThrow();
        assertEquals(40, updated.getPriority());
    }

    @StorageTest
    @DisplayName("assign role updates player and sends message")
    void assignRoleAssignsAndSendsMessage() throws Exception {
        Assumptions.assumeFalse(org.mockito.Mockito.mockingDetails(repos).isMock(), "Skip mock storage");

        final FactionServiceImpl service = new FactionServiceImpl(plugin, repos, config, logger);
        final var assignCmd = new CmdRoleAssign(service);

        final Optional<FactionModel> created = service.createFaction("Roles", ownerId);
        assertTrue(created.isPresent());
        final String factionId = created.get().getId();
        assertTrue(service.joinFaction(factionId, memberId));

        assertEquals(CreateRoleResult.SUCCESS, service.createRole(ownerId, "Scout", 30, null));

        assignCmd.execute(ctx("member", "Scout"));

        org.mockito.Mockito.verify(player).sendMessage(argThat(componentContains("Assigned role")));

        final PlayerModel pm = repos.players().find(memberId.toString()).orElseThrow();
        final String rankId = pm.getRankId();
        final RankModel rank = repos.ranks().find(rankId).orElseThrow();
        assertEquals("Scout", rank.getName());
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

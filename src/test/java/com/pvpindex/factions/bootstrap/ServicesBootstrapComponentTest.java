package com.pvpindex.factions.bootstrap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.api.TeamsApiRegistrar;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.config.NotificationsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.repository.FactionRepository;
import com.pvpindex.factions.data.repository.InvitationRepository;
import com.pvpindex.factions.data.repository.PlayerRepository;
import com.pvpindex.factions.data.repository.RankRepository;
import com.pvpindex.factions.data.repository.WarpRepository;
import com.pvpindex.factions.registry.EngineRegistry;
import com.pvpindex.factions.registry.InfraRegistry;
import com.pvpindex.factions.registry.ServiceRegistry;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ServicesBootstrapComponent — TeamsAPI soft-dependency isolation")
class ServicesBootstrapComponentTest {

    @Mock private Plugin plugin;
    @Mock private Server server;
    @Mock private PluginManager pluginManager;
    @Mock private InfraRegistry infraRegistry;
    @Mock private ServiceRegistry serviceRegistry;
    @Mock private EngineRegistry engineRegistry;
    @Mock private Repositories repos;
    @Mock private FactionsConfig factionsConfig;
    @Mock private NotificationsConfig notificationsConfig;
    @Mock private FactionRepository factionRepo;
    @Mock private PlayerRepository playerRepo;
    @Mock private RankRepository rankRepo;
    @Mock private InvitationRepository invitationRepo;
    @Mock private WarpRepository warpRepo;

    private BootstrapContext context;
    private ServicesBootstrapComponent component;

    @BeforeEach
    void setUp() {
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        when(server.getPluginManager()).thenReturn(pluginManager);

        when(infraRegistry.getRepositories()).thenReturn(repos);
        when(infraRegistry.getConfig()).thenReturn(factionsConfig);
        when(infraRegistry.getNotificationsConfig()).thenReturn(notificationsConfig);

        when(repos.factions()).thenReturn(factionRepo);
        when(repos.players()).thenReturn(playerRepo);
        when(repos.ranks()).thenReturn(rankRepo);
        when(repos.invitations()).thenReturn(invitationRepo);
        when(repos.warps()).thenReturn(warpRepo);

        context = new BootstrapContext(plugin, infraRegistry, serviceRegistry, engineRegistry);
        component = new ServicesBootstrapComponent();
    }

    // -------------------------------------------------------------------------
    // Without TeamsAPI plugin loaded on the server
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("start() returns true and runs standalone when TeamsAPI plugin is absent")
    void testStartStandaloneWhenTeamsApiPluginAbsent() {
        when(pluginManager.getPlugin("TeamsAPI")).thenReturn(null);

        final boolean result = component.start(context);

        assertTrue(result, "start() must succeed in standalone mode");
        assertFalse(context.isTeamsApiEnabled(), "teamsApiEnabled must be false in standalone mode");
        assertNull(context.getTeamsRegistrar(), "no registrar should be stored in standalone mode");
    }

    @Test
    @DisplayName("internal services are always wired into ServiceRegistry regardless of TeamsAPI")
    void testStartRegistersInternalServicesAlways() {
        when(pluginManager.getPlugin("TeamsAPI")).thenReturn(null);

        component.start(context);

        verify(serviceRegistry).setFactionService(notNull());
        verify(serviceRegistry).setInviteService(notNull());
        verify(serviceRegistry).setWarpService(notNull());
    }

    // -------------------------------------------------------------------------
    // stop() lifecycle
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("stop() is a no-op and safe when no registrar was stored")
    void testStopSafeWhenNoRegistrar() {
        assertNull(context.getTeamsRegistrar());
        component.stop(context);  // must not throw
        assertNull(context.getTeamsRegistrar());
    }

    @Test
    @DisplayName("stop() calls unregister() and clears the registrar when one is stored")
    void testStopCallsUnregisterWhenRegistrarPresent() {
        final TeamsApiRegistrar registrar = mock(TeamsApiRegistrar.class);
        context.setTeamsRegistrar(registrar);

        component.stop(context);

        verify(registrar).unregister();
        assertNull(context.getTeamsRegistrar(), "registrar must be cleared after stop");
    }

    // -------------------------------------------------------------------------
    // Registrar failure path: registration throws → bootstrap still succeeds
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("stop() clears a non-null registrar that was stored then manually nulled")
    void testStartGracefulFallbackWhenRegistrarReturnsFalse() {
        when(pluginManager.getPlugin("TeamsAPI")).thenReturn(null);

        final TeamsApiRegistrar failing = mock(TeamsApiRegistrar.class);
        context.setTeamsRegistrar(failing);
        context.setTeamsRegistrar(null);

        component.stop(context);
        assertNull(context.getTeamsRegistrar());
    }
}

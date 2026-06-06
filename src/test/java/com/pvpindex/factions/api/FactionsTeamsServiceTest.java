package com.pvpindex.factions.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.repository.FactionRepository;
import com.pvpindex.factions.service.FactionServiceImpl;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
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
@DisplayName("FactionsTeamsService")
class FactionsTeamsServiceTest {

    @Mock private Plugin plugin;
    @Mock private Repositories repos;
    @Mock private FactionsConfig config;
    @Mock private FactionRepository factions;

    private FactionsTeamsService service;

    @BeforeEach
    void setUp() throws Exception {
        when(repos.factions()).thenReturn(factions);

        final FactionServiceImpl impl = new FactionServiceImpl(
                plugin, repos, config, Logger.getLogger("test"));
        service = new FactionsTeamsService(impl);

        final PluginManager pluginManager = mock(PluginManager.class);
        final Server mockServer = mock(Server.class);
        when(mockServer.getPluginManager()).thenReturn(pluginManager);
        final Field serverField = org.bukkit.Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, mockServer);
    }

    @AfterEach
    void tearDown() throws Exception {
        final Field serverField = org.bukkit.Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, null);
    }

    // -------------------------------------------------------------------------
    // getTeamIds
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getTeamIds returns all faction UUIDs")
    void getTeamIdsReturnsAllFactions() throws StorageException {
        final UUID id1 = UUID.randomUUID();
        final UUID id2 = UUID.randomUUID();
        final FactionModel f1 = new FactionModel(id1.toString());
        final FactionModel f2 = new FactionModel(id2.toString());
        when(factions.findAll()).thenReturn(List.of(f1, f2));

        final var ids = service.getTeamIds();

        assertEquals(2, ids.size());
        assertTrue(ids.contains(id1));
        assertTrue(ids.contains(id2));
    }

    @Test
    @DisplayName("getTeamIds returns empty list when no factions exist")
    void getTeamIdsReturnsEmptyWhenNoFactions() throws StorageException {
        when(factions.findAll()).thenReturn(List.of());

        final var ids = service.getTeamIds();

        assertTrue(ids.isEmpty());
    }

    @Test
    @DisplayName("getTeamIds returns empty list on StorageException")
    void getTeamIdsReturnsEmptyOnException() throws StorageException {
        when(factions.findAll()).thenThrow(new StorageException("test"));

        final var ids = service.getTeamIds();

        assertTrue(ids.isEmpty());
    }

    // -------------------------------------------------------------------------
    // getTeamCount delegation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getTeamCount delegates to repository countAll")
    void getTeamCountDelegates() throws StorageException {
        when(factions.countAll()).thenReturn(42);

        assertEquals(42, service.getTeamCount());
    }
}

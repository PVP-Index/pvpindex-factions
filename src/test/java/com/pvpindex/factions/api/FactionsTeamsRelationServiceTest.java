package com.pvpindex.factions.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.repository.FactionRepository;
import com.pvpindex.factions.service.FactionServiceImpl;
import com.skyblockexp.teamsapi.event.TeamRelationChangeEvent;
import com.skyblockexp.teamsapi.model.TeamRelation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
@DisplayName("FactionsTeamsRelationService")
class FactionsTeamsRelationServiceTest {

    @Mock private Plugin plugin;
    @Mock private Repositories repos;
    @Mock private FactionsConfig config;
    @Mock private FactionRepository factions;

    private FactionsTeamsRelationService service;
    private PluginManager pluginManager;

    private final UUID fromId = UUID.randomUUID();
    private final UUID toId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        when(repos.factions()).thenReturn(factions);

        final FactionServiceImpl impl = new FactionServiceImpl(
                plugin, repos, config, Logger.getLogger("test"));
        service = new FactionsTeamsRelationService(impl);

        pluginManager = mock(PluginManager.class);
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
    // getRelation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getRelation returns NEUTRAL when no relation is stored")
    void getRelationDefaultsToNeutral() throws StorageException {
        final FactionModel fromFaction = new FactionModel(fromId.toString());
        fromFaction.setRelationsJson("{}");
        when(factions.find(fromId.toString())).thenReturn(Optional.of(fromFaction));

        assertEquals(TeamRelation.NEUTRAL, service.getRelation(fromId, toId));
    }

    @Test
    @DisplayName("getRelation returns ALLY when stored")
    void getRelationReturnsAlly() throws StorageException {
        final FactionModel fromFaction = new FactionModel(fromId.toString());
        fromFaction.setRelationsJson("{\"" + toId + "\":\"ALLY\"}");
        when(factions.find(fromId.toString())).thenReturn(Optional.of(fromFaction));

        assertEquals(TeamRelation.ALLY, service.getRelation(fromId, toId));
    }

    @Test
    @DisplayName("getRelation returns ENEMY when stored")
    void getRelationReturnsEnemy() throws StorageException {
        final FactionModel fromFaction = new FactionModel(fromId.toString());
        fromFaction.setRelationsJson("{\"" + toId + "\":\"ENEMY\"}");
        when(factions.find(fromId.toString())).thenReturn(Optional.of(fromFaction));

        assertEquals(TeamRelation.ENEMY, service.getRelation(fromId, toId));
    }

    @Test
    @DisplayName("getRelation returns NEUTRAL when faction is not found")
    void getRelationNeutralWhenMissing() throws StorageException {
        when(factions.find(fromId.toString())).thenReturn(Optional.empty());

        assertEquals(TeamRelation.NEUTRAL, service.getRelation(fromId, toId));
    }

    // -------------------------------------------------------------------------
    // getRelations
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getRelations returns all non-neutral declared relations")
    void getRelationsReturnsNonNeutral() throws StorageException {
        final UUID enemyId = UUID.randomUUID();
        final FactionModel faction = new FactionModel(fromId.toString());
        faction.setRelationsJson(
                "{\"" + toId + "\":\"ALLY\",\"" + enemyId + "\":\"ENEMY\"}");
        when(factions.find(fromId.toString())).thenReturn(Optional.of(faction));

        final Map<UUID, TeamRelation> result = service.getRelations(fromId);

        assertEquals(2, result.size());
        assertEquals(TeamRelation.ALLY, result.get(toId));
        assertEquals(TeamRelation.ENEMY, result.get(enemyId));
    }

    @Test
    @DisplayName("getRelations returns empty map when faction has no relations")
    void getRelationsEmptyWhenNoRelations() throws StorageException {
        final FactionModel faction = new FactionModel(fromId.toString());
        faction.setRelationsJson("{}");
        when(factions.find(fromId.toString())).thenReturn(Optional.of(faction));

        assertTrue(service.getRelations(fromId).isEmpty());
    }

    // -------------------------------------------------------------------------
    // setRelation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("setRelation persists ALLY and returns true")
    void setRelationPersistsAlly() throws StorageException {
        final FactionModel fromFaction = new FactionModel(fromId.toString());
        fromFaction.setRelationsJson("{}");
        final FactionModel toFaction = new FactionModel(toId.toString());
        toFaction.setRelationsJson("{}");
        when(factions.find(fromId.toString())).thenReturn(Optional.of(fromFaction));
        when(factions.find(toId.toString())).thenReturn(Optional.of(toFaction));

        final boolean result = service.setRelation(fromId, toId, TeamRelation.ALLY, actorId);

        assertTrue(result);
        assertTrue(fromFaction.getRelationsJson().contains("\"ALLY\""),
                "Expected ALLY in relations JSON");
        verify(factions).save(fromFaction);
    }

    @Test
    @DisplayName("setRelation setting NEUTRAL removes the prior declared relation")
    void setRelationNeutralRemovesEntry() throws StorageException {
        final FactionModel fromFaction = new FactionModel(fromId.toString());
        fromFaction.setRelationsJson("{\"" + toId + "\":\"ENEMY\"}");
        final FactionModel toFaction = new FactionModel(toId.toString());
        toFaction.setRelationsJson("{}");
        when(factions.find(fromId.toString())).thenReturn(Optional.of(fromFaction));
        when(factions.find(toId.toString())).thenReturn(Optional.of(toFaction));

        final boolean result = service.setRelation(fromId, toId, TeamRelation.NEUTRAL, actorId);

        assertTrue(result);
        assertFalse(fromFaction.getRelationsJson().contains(toId.toString()),
                "Target faction ID must be removed when relation is NEUTRAL");
        verify(factions).save(fromFaction);
    }

    @Test
    @DisplayName("setRelation returns false when source faction not found")
    void setRelationReturnsFalseWhenFromMissing() throws StorageException {
        when(factions.find(fromId.toString())).thenReturn(Optional.empty());

        assertFalse(service.setRelation(fromId, toId, TeamRelation.ALLY, actorId));
        verify(factions, never()).save(any());
    }

    @Test
    @DisplayName("setRelation returns false when target faction not found")
    void setRelationReturnsFalseWhenToMissing() throws StorageException {
        final FactionModel fromFaction = new FactionModel(fromId.toString());
        fromFaction.setRelationsJson("{}");
        when(factions.find(fromId.toString())).thenReturn(Optional.of(fromFaction));
        when(factions.find(toId.toString())).thenReturn(Optional.empty());

        assertFalse(service.setRelation(fromId, toId, TeamRelation.ALLY, actorId));
        verify(factions, never()).save(any());
    }

    @Test
    @DisplayName("setRelation returns false when TeamRelationChangeEvent is cancelled")
    void setRelationReturnsFalseWhenEventCancelled() throws StorageException {
        final FactionModel fromFaction = new FactionModel(fromId.toString());
        fromFaction.setRelationsJson("{}");
        final FactionModel toFaction = new FactionModel(toId.toString());
        toFaction.setRelationsJson("{}");
        when(factions.find(fromId.toString())).thenReturn(Optional.of(fromFaction));
        when(factions.find(toId.toString())).thenReturn(Optional.of(toFaction));

        doAnswer(invocation -> {
            final TeamRelationChangeEvent event = invocation.getArgument(0);
            event.setCancelled(true);
            return null;
        }).when(pluginManager).callEvent(any(TeamRelationChangeEvent.class));

        assertFalse(service.setRelation(fromId, toId, TeamRelation.ALLY, actorId));
        verify(factions, never()).save(any());
    }

    @Test
    @DisplayName("setRelation respects overridden relation from event listener")
    void setRelationUsesOverriddenRelationFromEvent() throws StorageException {
        final FactionModel fromFaction = new FactionModel(fromId.toString());
        fromFaction.setRelationsJson("{}");
        final FactionModel toFaction = new FactionModel(toId.toString());
        toFaction.setRelationsJson("{}");
        when(factions.find(fromId.toString())).thenReturn(Optional.of(fromFaction));
        when(factions.find(toId.toString())).thenReturn(Optional.of(toFaction));

        doAnswer(invocation -> {
            final TeamRelationChangeEvent event = invocation.getArgument(0);
            event.setNewRelation(TeamRelation.TRUCE); // listener downgrades ALLY → TRUCE
            return null;
        }).when(pluginManager).callEvent(any(TeamRelationChangeEvent.class));

        assertTrue(service.setRelation(fromId, toId, TeamRelation.ALLY, actorId));
        assertTrue(fromFaction.getRelationsJson().contains("\"TRUCE\""),
                "Expected TRUCE (overridden by event listener) in relations JSON");
        assertFalse(fromFaction.getRelationsJson().contains("\"ALLY\""),
                "ALLY must not appear when overridden to TRUCE");
    }

    // -------------------------------------------------------------------------
    // clearRelations
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("clearRelations removes own outgoing relations and returns true")
    void clearRelationsRemovesOutgoing() throws StorageException {
        final FactionModel faction = new FactionModel(fromId.toString());
        faction.setRelationsJson("{\"" + toId + "\":\"ALLY\"}");
        when(factions.find(fromId.toString())).thenReturn(Optional.of(faction));
        when(factions.findAll()).thenReturn(List.of(faction));

        assertTrue(service.clearRelations(fromId));
        assertEquals("{}", faction.getRelationsJson());
        verify(factions).save(faction);
    }

    @Test
    @DisplayName("clearRelations removes incoming references from other factions")
    void clearRelationsRemovesIncoming() throws StorageException {
        final FactionModel target = new FactionModel(fromId.toString());
        target.setRelationsJson("{}");
        final FactionModel other = new FactionModel(toId.toString());
        other.setRelationsJson("{\"" + fromId + "\":\"ENEMY\"}");
        when(factions.find(fromId.toString())).thenReturn(Optional.of(target));
        when(factions.findAll()).thenReturn(List.of(target, other));

        assertTrue(service.clearRelations(fromId));
        assertFalse(other.getRelationsJson().contains(fromId.toString()),
                "Other faction must not reference the cleared faction");
        verify(factions).save(other);
    }

    @Test
    @DisplayName("clearRelations returns false when faction does not exist")
    void clearRelationsReturnsFalseWhenMissing() throws StorageException {
        when(factions.find(fromId.toString())).thenReturn(Optional.empty());

        assertFalse(service.clearRelations(fromId));
        verify(factions, never()).save(any());
    }

    @Test
    @DisplayName("clearRelations returns false when faction has no relations at all")
    void clearRelationsReturnsFalseWhenNoRelations() throws StorageException {
        final FactionModel faction = new FactionModel(fromId.toString());
        faction.setRelationsJson("{}");
        when(factions.find(fromId.toString())).thenReturn(Optional.of(faction));
        when(factions.findAll()).thenReturn(List.of(faction));

        assertFalse(service.clearRelations(fromId));
    }
}

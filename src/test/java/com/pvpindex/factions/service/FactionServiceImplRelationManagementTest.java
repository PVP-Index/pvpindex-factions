package com.pvpindex.factions.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.Relation;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.repository.FactionRepository;
import com.pvpindex.factions.data.repository.PlayerRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
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
@DisplayName("FactionServiceImpl relation management")
class FactionServiceImplRelationManagementTest {

    @Mock private Plugin plugin;
    @Mock private Repositories repos;
    @Mock private FactionsConfig config;
    @Mock private PlayerRepository players;
    @Mock private FactionRepository factions;

    private FactionServiceImpl service;
    private final UUID actor = UUID.randomUUID();
    private final String sourceId = UUID.randomUUID().toString();
    private final String targetId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() throws StorageException {
        when(repos.players()).thenReturn(players);
        when(repos.factions()).thenReturn(factions);
        when(config.getMaxAllies()).thenReturn(5);
        when(config.getMaxTruces()).thenReturn(5);
        service = new FactionServiceImpl(plugin, repos, config, Logger.getLogger("test"));

        final PlayerModel actorModel = new PlayerModel(actor.toString());
        actorModel.setFactionId(sourceId);
        when(players.find(actor.toString())).thenReturn(Optional.of(actorModel));
    }

    @Test
    @DisplayName("ally relation is reciprocated when both sides agree")
    void allyIsReciprocatedWhenMutual() throws StorageException {
        final FactionModel source = new FactionModel(sourceId);
        source.setName("Alpha");
        source.setRelationsJson("{}");
        final FactionModel target = new FactionModel(targetId);
        target.setName("Beta");
        target.setRelationsJson("{\"" + sourceId + "\":\"ALLY\"}");

        when(factions.find(sourceId)).thenReturn(Optional.of(source));
        when(factions.findByName("Beta")).thenReturn(Optional.of(target));

        final Optional<Relation> result = service.setRelation(actor, "Beta", Relation.ALLY);

        assertTrue(result.isPresent());
        assertTrue(source.getRelationsJson().contains("\"" + targetId + "\":\"ALLY\""));
        verify(factions).save(source);
        verify(factions).save(target);
    }

    @Test
    @DisplayName("enemy relation is always mirrored")
    void enemyIsAlwaysMirrored() throws StorageException {
        final FactionModel source = new FactionModel(sourceId);
        source.setName("Alpha");
        source.setRelationsJson("{}");
        final FactionModel target = new FactionModel(targetId);
        target.setName("Beta");
        target.setRelationsJson("{}");

        when(factions.find(sourceId)).thenReturn(Optional.of(source));
        when(factions.findByName("Beta")).thenReturn(Optional.of(target));

        final Optional<Relation> result = service.setRelation(actor, "Beta", Relation.ENEMY);

        assertTrue(result.isPresent());
        assertTrue(source.getRelationsJson().contains("\"" + targetId + "\":\"ENEMY\""));
        assertTrue(target.getRelationsJson().contains("\"" + sourceId + "\":\"ENEMY\""));
        verify(factions).save(source);
        verify(factions).save(target);
    }

    @Test
    @DisplayName("ally relation respects max-allies limit")
    void allyRespectsLimit() throws StorageException {
        when(config.getMaxAllies()).thenReturn(1);
        final String existingAlly = UUID.randomUUID().toString();

        final FactionModel source = new FactionModel(sourceId);
        source.setName("Alpha");
        source.setRelationsJson("{\"" + existingAlly + "\":\"ALLY\"}");
        final FactionModel target = new FactionModel(targetId);
        target.setName("Beta");
        target.setRelationsJson("{}");

        when(factions.find(sourceId)).thenReturn(Optional.of(source));
        when(factions.findByName("Beta")).thenReturn(Optional.of(target));

        final Optional<Relation> result = service.setRelation(actor, "Beta", Relation.ALLY);

        assertTrue(result.isEmpty());
        verify(factions, never()).save(any());
    }
}

package com.pvpindex.factions.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.repository.BoardRepository;
import com.pvpindex.factions.data.repository.FactionRepository;
import com.pvpindex.factions.data.repository.PlayerRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
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
@DisplayName("EngineChunkChange — overclaim validation")
class EngineChunkChangeOverclaimTest {

    private static final String ATTACKER_FACTION_ID = "attacker-faction";
    private static final String VICTIM_FACTION_ID = "victim-faction";

    @Mock private Repositories repos;
    @Mock private FactionsConfig config;
    @Mock private PlayerRepository players;
    @Mock private FactionRepository factions;
    @Mock private BoardRepository board;

    @Mock private Player player;
    @Mock private Chunk chunk;
    @Mock private World world;

    private EngineChunkChange engine;

    private final UUID playerUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() throws StorageException {
        when(repos.players()).thenReturn(players);
        when(repos.factions()).thenReturn(factions);
        when(repos.board()).thenReturn(board);

        // Player belongs to the attacker faction
        final PlayerModel pm = new PlayerModel(playerUuid.toString());
        pm.setFactionId(ATTACKER_FACTION_ID);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(players.find(playerUuid.toString())).thenReturn(Optional.of(pm));

        // Attacker faction (no relations JSON set → relationsJson is null → NEUTRAL by default)
        final FactionModel attackerFaction = new FactionModel(ATTACKER_FACTION_ID);
        attackerFaction.setName("Attackers");
        when(factions.find(ATTACKER_FACTION_ID)).thenReturn(Optional.of(attackerFaction));

        // Victim faction
        final FactionModel victimFaction = new FactionModel(VICTIM_FACTION_ID);
        victimFaction.setName("Victims");
        when(factions.find(VICTIM_FACTION_ID)).thenReturn(Optional.of(victimFaction));

        // The target chunk is owned by the victim faction
        final BoardEntry victimEntry = new BoardEntry("world:0:0");
        victimEntry.setFactionId(VICTIM_FACTION_ID);
        when(chunk.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(chunk.getX()).thenReturn(0);
        when(chunk.getZ()).thenReturn(0);
        when(board.findByChunk("world", 0, 0)).thenReturn(Optional.of(victimEntry));

        // Config defaults (power computation)
        when(config.getMaxLand()).thenReturn(100);
        when(config.getLandPerPower()).thenReturn(1.0);

        engine = new EngineChunkChange(repos, config, Logger.getLogger("test"));
    }

    @Test
    @DisplayName("overclaim blocked — feature disabled in config")
    void testOverclaimBlockedWhenDisabled() throws StorageException {
        when(config.isOverclaimingEnabled()).thenReturn(false);

        assertFalse(engine.claim(player, chunk));
    }

    @Test
    @DisplayName("overclaim blocked — system zone (safezone) cannot be overclaimed")
    void testOverclaimBlockedSystemZone() throws StorageException {
        when(config.isOverclaimingEnabled()).thenReturn(true);
        final BoardEntry safezoneEntry = new BoardEntry("world:0:0");
        safezoneEntry.setFactionId(FactionModel.SAFEZONE_ID);
        when(board.findByChunk("world", 0, 0)).thenReturn(Optional.of(safezoneEntry));

        assertFalse(engine.claim(player, chunk));
    }

    @Test
    @DisplayName("overclaim blocked — chunk already owned by player's own faction")
    void testOverclaimBlockedOwnFaction() throws StorageException {
        when(config.isOverclaimingEnabled()).thenReturn(true);
        final BoardEntry ownEntry = new BoardEntry("world:0:0");
        ownEntry.setFactionId(ATTACKER_FACTION_ID);
        when(board.findByChunk("world", 0, 0)).thenReturn(Optional.of(ownEntry));

        assertFalse(engine.claim(player, chunk));
    }

    @Test
    @DisplayName("overclaim blocked — enemy relation required but victim relation is neutral")
    void testOverclaimBlockedEnemyRelationGuard() throws StorageException {
        when(config.isOverclaimingEnabled()).thenReturn(true);
        when(config.isOverclaimRequireEnemyRelation()).thenReturn(true);
        // Attacker faction has no relationsJson (null) → getRelation returns NEUTRAL → blocked

        assertFalse(engine.claim(player, chunk));
    }

    @Test
    @DisplayName("overclaim blocked — victim faction still has enough power")
    void testOverclaimBlockedVictimHasPower() throws StorageException {
        when(config.isOverclaimingEnabled()).thenReturn(true);
        when(config.isOverclaimRequireEnemyRelation()).thenReturn(false);

        // Victim: 3 members × 10 power → maxLand = min(100, 30/1.0) = 30; current = 30 (at capacity)
        final PlayerModel m1 = new PlayerModel(UUID.randomUUID().toString());
        m1.setPower(10.0);
        final PlayerModel m2 = new PlayerModel(UUID.randomUUID().toString());
        m2.setPower(10.0);
        final PlayerModel m3 = new PlayerModel(UUID.randomUUID().toString());
        m3.setPower(10.0);
        when(players.findByFactionId(VICTIM_FACTION_ID)).thenReturn(List.of(m1, m2, m3));
        when(board.countByFactionId(VICTIM_FACTION_ID)).thenReturn(30);

        assertFalse(engine.claim(player, chunk));
    }
}

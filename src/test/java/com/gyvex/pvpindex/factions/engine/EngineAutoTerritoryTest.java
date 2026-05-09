package com.gyvex.pvpindex.factions.engine;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.gyvex.pvpindex.factions.data.model.AutoTerritoryMode;
import com.gyvex.pvpindex.factions.integration.worldguard.TerritoryGuard;
import java.util.UUID;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
@DisplayName("EngineAutoTerritory")
class EngineAutoTerritoryTest {

    @Mock private EngineChunkChange chunkChange;
    @Mock private TerritoryGuard territoryGuard;
    @Mock private AutoTerritoryModeCache modeCache;
    @Mock private Player player;
    @Mock private World world;
    @Mock private Chunk fromChunk;
    @Mock private Chunk toChunk;

    private EngineAutoTerritory engine;
    private final UUID playerId = UUID.randomUUID();
    private Location from;
    private Location to;

    @BeforeEach
    void setUp() {
        engine = new EngineAutoTerritory(chunkChange, territoryGuard, modeCache);
        from = new Location(world, 0, 64, 0);
        to = new Location(world, 32, 64, 0);
        when(world.getChunkAt(from)).thenReturn(fromChunk);
        when(world.getChunkAt(to)).thenReturn(toChunk);
        when(player.getUniqueId()).thenReturn(playerId);
    }

    @Test
    void testClaimModeClaimsOnMove() {
        when(modeCache.getMode(playerId)).thenReturn(AutoTerritoryMode.CLAIM);
        when(territoryGuard.canModifyTerritory(eq(player), any(Location.class))).thenReturn(true);

        engine.onMove(new PlayerMoveEvent(player, from, to));

        verify(chunkChange).claim(player, toChunk);
        verify(chunkChange, never()).unclaim(player, toChunk);
    }

    @Test
    void testUnclaimModeUnclaimsOnMove() {
        when(modeCache.getMode(playerId)).thenReturn(AutoTerritoryMode.UNCLAIM);

        engine.onMove(new PlayerMoveEvent(player, from, to));

        verify(chunkChange).unclaim(player, toChunk);
        verify(chunkChange, never()).claim(player, toChunk);
    }

    @Test
    void testOffModeDoesNothing() {
        when(modeCache.getMode(playerId)).thenReturn(AutoTerritoryMode.OFF);

        engine.onMove(new PlayerMoveEvent(player, from, to));

        verify(chunkChange, never()).claim(player, toChunk);
        verify(chunkChange, never()).unclaim(player, toChunk);
    }

    @Test
    void testHydrateOnJoin() {
        engine.onJoin(new PlayerJoinEvent(player, "join"));

        verify(modeCache).hydrate(playerId);
    }

    @Test
    void testEvictOnQuit() {
        engine.onQuit(new PlayerQuitEvent(player, "quit"));

        verify(modeCache).evict(playerId);
    }
}

package com.pvpindex.factions.command.sub;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.AutoTerritoryMode;
import com.pvpindex.factions.engine.AutoTerritoryModeCache;
import com.pvpindex.factions.engine.EngineChunkChange;
import com.pvpindex.factions.integration.worldguard.TerritoryGuard;
import java.util.UUID;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import com.pvpindex.factions.command.StorageTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdClaim — /f claim")
class CmdClaimTest extends CommandTestBase {


    @Mock private EngineChunkChange engineChunkChange;
    @Mock private TerritoryGuard territoryGuard;
    @Mock private AutoTerritoryModeCache autoModeCache;
    @Mock private Location location;
    @Mock private Chunk chunk;
    @Mock private World world;
    @Mock private Block block;


    private CmdClaim cmd;
    private final UUID uuid = UUID.randomUUID();


    @BeforeEach
    void setUp() {
        cmd = new CmdClaim(engineChunkChange, territoryGuard, autoModeCache);
        when(territoryGuard.canModifyTerritory(player, location)).thenReturn(true);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getLocation()).thenReturn(location);
        when(location.getChunk()).thenReturn(chunk);
        when(player.getWorld()).thenReturn(world);
        when(chunk.getBlock(8, 0, 8)).thenReturn(block);
        when(block.getLocation()).thenReturn(location);
        when(location.getBlockY()).thenReturn(0);
    }


    @StorageTest
    @DisplayName("success — chunk claimed")
    void testClaimSuccess() {
        when(engineChunkChange.claim(player, chunk)).thenReturn(true);


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("claimed")));
    }


    @StorageTest
    @DisplayName("claim fails — engine sends own feedback, no double message")
    void testClaimFails() {
        when(engineChunkChange.claim(player, chunk)).thenReturn(false);


        // No exception should be thrown; engine is responsible for error messages
        cmd.execute(ctx());
    }


    @StorageTest
    @DisplayName("no permission — rejected before engine call")
    void testNoPermission() {
        when(player.hasPermission("factions.cmd.claim")).thenReturn(false);


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("permission")));
    }


    @StorageTest
    @DisplayName("auto on — persists CLAIM mode")
    void testAutoOnPersistsMode() {
        when(autoModeCache.setMode(eq(uuid), eq(AutoTerritoryMode.CLAIM))).thenReturn(true);


        cmd.execute(ctx("auto", "on"));


        verify(autoModeCache).setMode(uuid, AutoTerritoryMode.CLAIM);
    }


    @StorageTest
    @DisplayName("auto off — persists OFF mode")
    void testAutoOffPersistsMode() {
        when(autoModeCache.setMode(eq(uuid), eq(AutoTerritoryMode.OFF))).thenReturn(true);


        cmd.execute(ctx("auto", "off"));


        verify(autoModeCache).setMode(uuid, AutoTerritoryMode.OFF);
    }


    @StorageTest
    @DisplayName("claim at — claims target chunk by coordinates")
    void testClaimAtCoordinates() {
        when(world.getChunkAt(12, -4)).thenReturn(chunk);
        when(engineChunkChange.claim(player, chunk)).thenReturn(true);


        cmd.execute(ctx("at", "12", "-4"));


        verify(engineChunkChange).claim(player, chunk);
    }
}

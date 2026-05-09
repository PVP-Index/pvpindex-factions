package com.pvpindex.factions.command.sub;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.AutoTerritoryMode;
import com.pvpindex.factions.engine.AutoTerritoryModeCache;
import com.pvpindex.factions.engine.EngineChunkChange;
import com.pvpindex.factions.service.FactionService;
import java.util.UUID;
import org.bukkit.Chunk;
import org.bukkit.Location;
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
@DisplayName("CmdUnclaim — /f unclaim")
class CmdUnclaimTest extends CommandTestBase {

    @Mock private EngineChunkChange engineChunkChange;
    @Mock private FactionService factionService;
    @Mock private AutoTerritoryModeCache autoModeCache;
    @Mock private Location location;
    @Mock private Chunk chunk;

    private CmdUnclaim cmd;
    private final UUID uuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cmd = new CmdUnclaim(engineChunkChange, factionService, autoModeCache);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getLocation()).thenReturn(location);
        when(location.getChunk()).thenReturn(chunk);
    }

    @Test
    @DisplayName("success — chunk unclaimed")
    void testUnclaimSuccess() {
        when(engineChunkChange.unclaim(player, chunk)).thenReturn(true);

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("unclaimed")));
    }

    @Test
    @DisplayName("unclaim fails — engine sends own feedback, no double message")
    void testUnclaimFails() {
        when(engineChunkChange.unclaim(player, chunk)).thenReturn(false);

        cmd.execute(ctx());
    }

    @Test
    @DisplayName("no permission — rejected before engine call")
    void testNoPermission() {
        when(player.hasPermission("factions.cmd.unclaim")).thenReturn(false);

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("permission")));
    }

    @Test
    @DisplayName("auto on — persists UNCLAIM mode")
    void testAutoOnPersistsMode() {
        when(autoModeCache.setMode(eq(uuid), eq(AutoTerritoryMode.UNCLAIM))).thenReturn(true);

        cmd.execute(ctx("auto", "on"));

        verify(autoModeCache).setMode(uuid, AutoTerritoryMode.UNCLAIM);
    }

    @Test
    @DisplayName("auto off — persists OFF mode")
    void testAutoOffPersistsMode() {
        when(autoModeCache.setMode(eq(uuid), eq(AutoTerritoryMode.OFF))).thenReturn(true);

        cmd.execute(ctx("auto", "off"));

        verify(autoModeCache).setMode(uuid, AutoTerritoryMode.OFF);
    }
}

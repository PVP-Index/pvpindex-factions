package com.pvpindex.factions.command.sub.warp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.WarpModel;
import com.pvpindex.factions.integration.essentials.EssentialsInterop;
import com.pvpindex.factions.integration.worldguard.TerritoryGuard;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.WarpService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
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
@DisplayName("CmdWarp — /f warp [name | set | delete]")
class CmdWarpTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private WarpService warpService;
    @Mock private TerritoryGuard territoryGuard;
    @Mock private EssentialsInterop essentialsInterop;
    @Mock private FactionModel faction;

    private CmdWarp cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        cmd = new CmdWarp(factionService, warpService, territoryGuard, essentialsInterop);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
    }

    @Test
    @DisplayName("no args — lists faction warps")
    void testListWarps() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        final WarpModel warp = mock(WarpModel.class);
        when(warp.getName()).thenReturn("spawn");
        when(warpService.getWarps(factionId)).thenReturn(List.of(warp));

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("Faction Warps")));
        verify(player).sendMessage(argThat(componentContains("spawn")));
    }

    @Test
    @DisplayName("no args, no warps — empty message shown")
    void testListWarpsEmpty() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(warpService.getWarps(factionId)).thenReturn(List.of());

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("no warps")));
    }

    @Test
    @DisplayName("name arg — teleports to warp location")
    void testTeleportSuccess() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        final WarpModel warp = mock(WarpModel.class);
        final World world = mock(World.class);
        final Location loc = new Location(world, 0, 64, 0);
        when(warp.toLocation()).thenReturn(loc);
        when(warpService.getWarp(factionId, "spawn")).thenReturn(Optional.of(warp));

        cmd.execute(ctx("spawn"));

        verify(player).teleport(loc);
        verify(player).sendMessage(argThat(componentContains("Teleported")));
    }

    @Test
    @DisplayName("name arg — warp not found")
    void testTeleportWarpNotFound() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(warpService.getWarp(factionId, "ghost")).thenReturn(Optional.empty());

        cmd.execute(ctx("ghost"));

        verify(player).sendMessage(argThat(componentContains("not found")));
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    @DisplayName("name arg — warp world not loaded")
    void testTeleportNullWorld() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        final WarpModel warp = mock(WarpModel.class);
        when(warp.toLocation()).thenReturn(null);
        when(warpService.getWarp(factionId, "spawn")).thenReturn(Optional.of(warp));

        cmd.execute(ctx("spawn"));

        verify(player).sendMessage(argThat(componentContains("world not loaded")));
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    @DisplayName("not in faction — rejected")
    void testNotInFaction() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.empty());

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("not in a faction")));
        verify(warpService, never()).getWarps(any());
    }
}

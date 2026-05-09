package com.pvpindex.factions.command.sub.warp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.integration.worldguard.TerritoryGuard;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.WarpService;
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
@DisplayName("CmdWarpSet — /f warp set <name>")
class CmdWarpSetTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private WarpService warpService;
    @Mock private TerritoryGuard territoryGuard;
    @Mock private FactionModel faction;

    private CmdWarpSet cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        cmd = new CmdWarpSet(factionService, warpService, territoryGuard);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
        lenient().when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        lenient().when(territoryGuard.canModifyTerritory(any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("success — warp created at player location")
    void testSetWarpSuccess() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        final World world = mock(World.class);
        final Location loc = new Location(world, 0, 64, 0);
        when(player.getLocation()).thenReturn(loc);
        when(warpService.setWarp(eq(factionId), eq("spawn"), eq(loc), eq(uuid))).thenReturn(true);

        cmd.execute(ctx("spawn"));

        verify(player).sendMessage(argThat(componentContains("set")));
    }

    @Test
    @DisplayName("setWarp fails (limit reached) — error message")
    void testSetWarpFails() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        final World world = mock(World.class);
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(warpService.setWarp(any(), any(), any(), any())).thenReturn(false);

        cmd.execute(ctx("spawn"));

        verify(player).sendMessage(argThat(componentContains("limit")));
    }

    @Test
    @DisplayName("not in faction — rejected")
    void testNotInFaction() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.empty());

        cmd.execute(ctx("spawn"));

        verify(player).sendMessage(argThat(componentContains("not in a faction")));
        verify(warpService, never()).setWarp(any(), any(), any(), any());
    }

    @Test
    @DisplayName("missing arg — usage shown")
    void testMissingArg() {
        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("Usage")));
        verify(factionService, never()).getFactionByPlayer(any());
    }
}

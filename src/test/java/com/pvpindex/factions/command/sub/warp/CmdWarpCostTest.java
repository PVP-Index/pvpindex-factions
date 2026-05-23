package com.pvpindex.factions.command.sub.warp;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.WarpModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.WarpService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdWarpCost — /f warp cost <name> <amount>")
class CmdWarpCostTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private WarpService warpService;
    @Mock private FactionModel faction;
    @Mock private WarpModel warp;

    private CmdWarpCost cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        cmd = new CmdWarpCost(factionService, warpService);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
        when(faction.getName()).thenReturn("Alpha");
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        when(warpService.getWarp(factionId, "spawn")).thenReturn(Optional.of(warp));
    }

    @StorageTest
    @DisplayName("set cost — success")
    void testSetCost() {
        cmd.execute(ctx("spawn", "100.0"));

        verify(warpService).setWarpCost(factionId, "spawn", 100.0);
        verify(player).sendMessage(argThat(componentContains("100.00")));
    }

    @StorageTest
    @DisplayName("set cost zero — makes warp free")
    void testSetCostZero() {
        cmd.execute(ctx("spawn", "0"));

        verify(warpService).setWarpCost(factionId, "spawn", 0.0);
        verify(player).sendMessage(argThat(componentContains("0.00")));
    }

    @StorageTest
    @DisplayName("invalid amount (text) — rejected")
    void testInvalidAmount() {
        cmd.execute(ctx("spawn", "abc"));

        verify(warpService, never()).setWarpCost(any(), any(), any(double.class));
        verify(player).sendMessage(argThat(componentContains("non-negative")));
    }

    @StorageTest
    @DisplayName("negative amount — rejected")
    void testNegativeAmount() {
        cmd.execute(ctx("spawn", "-5"));

        verify(warpService, never()).setWarpCost(any(), any(), any(double.class));
        verify(player).sendMessage(argThat(componentContains("non-negative")));
    }

    @StorageTest
    @DisplayName("warp not found — rejected")
    void testWarpNotFound() {
        when(warpService.getWarp(factionId, "nowhere")).thenReturn(Optional.empty());

        cmd.execute(ctx("nowhere", "50"));

        verify(warpService, never()).setWarpCost(any(), any(), any(double.class));
        verify(player).sendMessage(argThat(componentContains("not found")));
    }

    @StorageTest
    @DisplayName("not officer — rejected")
    void testNotOfficer() {
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(false);

        cmd.execute(ctx("spawn", "50"));

        verify(warpService, never()).setWarpCost(any(), any(), any(double.class));
    }

    @StorageTest
    @DisplayName("not in faction — rejected")
    void testNotInFaction() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.empty());

        cmd.execute(ctx("spawn", "50"));

        verify(warpService, never()).setWarpCost(any(), any(), any(double.class));
    }

    @StorageTest
    @DisplayName("missing second arg — rejected")
    void testMissingAmount() {
        cmd.execute(ctx("spawn"));

        verify(warpService, never()).setWarpCost(any(), any(), any(double.class));
        verify(player).sendMessage(argThat(componentContains("Usage")));
    }
}

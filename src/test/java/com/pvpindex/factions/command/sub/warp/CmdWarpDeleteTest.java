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
import com.pvpindex.factions.data.model.WarpModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.WarpService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
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
@DisplayName("CmdWarpDelete — /f warp delete <name>")
class CmdWarpDeleteTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private WarpService warpService;
    @Mock private FactionModel faction;

    private CmdWarpDelete cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        cmd = new CmdWarpDelete(factionService, warpService);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
        lenient().when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
    }

    @Test
    @DisplayName("success — warp deleted")
    void testDeleteSuccess() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(warpService.deleteWarp(factionId, "spawn")).thenReturn(true);

        cmd.execute(ctx("spawn"));

        verify(player).sendMessage(argThat(componentContains("deleted")));
    }

    @Test
    @DisplayName("warp not found — error message")
    void testWarpNotFound() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(warpService.deleteWarp(factionId, "ghost")).thenReturn(false);

        cmd.execute(ctx("ghost"));

        verify(player).sendMessage(argThat(componentContains("not found")));
    }

    @Test
    @DisplayName("not in faction — rejected")
    void testNotInFaction() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.empty());

        cmd.execute(ctx("spawn"));

        verify(player).sendMessage(argThat(componentContains("not in a faction")));
        verify(warpService, never()).deleteWarp(any(), any());
    }

    @Test
    @DisplayName("missing arg — usage shown")
    void testMissingArg() {
        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("Usage")));
        verify(factionService, never()).getFactionByPlayer(any());
    }

    @Test
    @DisplayName("tab complete — returns faction warp names")
    void testTabComplete() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        final WarpModel warp = mock(WarpModel.class);
        when(warp.getName()).thenReturn("spawn");
        when(warpService.getWarps(eq(factionId))).thenReturn(List.of(warp));

        final List<String> completions = cmd.tabComplete(ctx());

        Assertions.assertTrue(completions.contains("spawn"));
    }
}

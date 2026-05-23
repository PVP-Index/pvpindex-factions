package com.pvpindex.factions.command.sub.warp;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
@DisplayName("CmdWarpPassword — /f warp password <name> [password | clear]")
class CmdWarpPasswordTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private WarpService warpService;
    @Mock private FactionModel faction;
    @Mock private WarpModel warp;

    private CmdWarpPassword cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        cmd = new CmdWarpPassword(factionService, warpService);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
        when(faction.getName()).thenReturn("Alpha");
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        when(warpService.getWarp(factionId, "spawn")).thenReturn(Optional.of(warp));
    }

    @StorageTest
    @DisplayName("set password — success")
    void testSetPassword() {
        cmd.execute(ctx("spawn", "secret123"));

        verify(warpService).setWarpPassword(factionId, "spawn", "secret123");
        verify(player).sendMessage(argThat(componentContains("Password set")));
    }

    @StorageTest
    @DisplayName("clear password — success")
    void testClearPassword() {
        cmd.execute(ctx("spawn", "clear"));

        verify(warpService).setWarpPassword(factionId, "spawn", null);
        verify(player).sendMessage(argThat(componentContains("cleared")));
    }

    @StorageTest
    @DisplayName("show status — warp has password")
    void testShowStatusWithPassword() {
        when(warp.hasPassword()).thenReturn(true);

        cmd.execute(ctx("spawn"));

        verify(warpService, never()).setWarpPassword(any(), any(), any());
        verify(player).sendMessage(argThat(componentContains("has a password")));
    }

    @StorageTest
    @DisplayName("show status — warp has no password")
    void testShowStatusNoPassword() {
        when(warp.hasPassword()).thenReturn(false);

        cmd.execute(ctx("spawn"));

        verify(warpService, never()).setWarpPassword(any(), any(), any());
        verify(player).sendMessage(argThat(componentContains("no password")));
    }

    @StorageTest
    @DisplayName("warp not found — rejected")
    void testWarpNotFound() {
        when(warpService.getWarp(factionId, "nowhere")).thenReturn(Optional.empty());

        cmd.execute(ctx("nowhere", "secret"));

        verify(warpService, never()).setWarpPassword(any(), any(), any());
        verify(player).sendMessage(argThat(componentContains("not found")));
    }

    @StorageTest
    @DisplayName("not officer — rejected")
    void testNotOfficer() {
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(false);

        cmd.execute(ctx("spawn", "secret"));

        verify(warpService, never()).setWarpPassword(any(), any(), any());
    }

    @StorageTest
    @DisplayName("not in faction — rejected")
    void testNotInFaction() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.empty());

        cmd.execute(ctx("spawn", "secret"));

        verify(warpService, never()).setWarpPassword(any(), any(), any());
    }
}

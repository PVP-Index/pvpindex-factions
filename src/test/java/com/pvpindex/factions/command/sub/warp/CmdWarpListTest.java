package com.pvpindex.factions.command.sub.warp;


import static org.mockito.ArgumentMatchers.argThat;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import com.pvpindex.factions.command.StorageTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdWarpList — /f warp list")
class CmdWarpListTest extends CommandTestBase {


    @Mock private FactionService factionService;
    @Mock private WarpService warpService;
    @Mock private FactionModel faction;


    private CmdWarpList cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();


    @BeforeEach
    void setUp() {
        cmd = new CmdWarpList(factionService, warpService);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
        when(config.getWarpListPageSize()).thenReturn(5);
    }


    @StorageTest
    @DisplayName("not in faction — rejected")
    void testNotInFaction() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.empty());


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("not in a faction")));
    }


    @StorageTest
    @DisplayName("no warps — empty message shown")
    void testNoWarps() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(warpService.getWarps(factionId)).thenReturn(List.of());


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("no warps")));
    }


    @StorageTest
    @DisplayName("has warps — warp name shown")
    void testWarpListed() {
        final WarpModel warp = Mockito.mock(WarpModel.class);
        when(warp.getName()).thenReturn("home");
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(warpService.getWarps(factionId)).thenReturn(List.of(warp));


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("home")));
    }
}

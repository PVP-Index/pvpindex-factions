package com.pvpindex.factions.command.sub.flag;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.FactionFlag;
import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.FlagService;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdFlagSet + CmdFlagList")
class CmdFlagTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private FlagService flagService;
    @Mock private FactionModel faction;

    private CmdFlagSet cmdFlagSet;
    private CmdFlagList cmdFlagList;

    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        cmdFlagSet = new CmdFlagSet(factionService, flagService);
        cmdFlagList = new CmdFlagList(factionService, flagService);

        when(player.getUniqueId()).thenReturn(actorId);
        when(faction.getName()).thenReturn("TestFaction");

        final org.bukkit.Server mockServer = Mockito.mock(org.bukkit.Server.class);
        final Field serverField = org.bukkit.Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, mockServer);
    }

    @AfterEach
    void tearDown() throws Exception {
        final Field serverField = org.bukkit.Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, null);
    }

    // =========================================================================
    // CmdFlagSet
    // =========================================================================

    @StorageTest
    @DisplayName("flag set — not in faction")
    void testSetNotInFaction() {
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.empty());

        cmdFlagSet.execute(ctx("pvp", "off"));

        verify(flagService, never()).setFlag(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        verify(player).sendMessage(argThat(componentContains("not in a faction")));
    }

    @StorageTest
    @DisplayName("flag set — not officer")
    void testSetNotOfficer() {
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.of(faction));
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(false);

        cmdFlagSet.execute(ctx("pvp", "off"));

        verify(flagService, never()).setFlag(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        verify(player).sendMessage(argThat(componentContains("officers")));
    }

    @StorageTest
    @DisplayName("flag set — unknown flag")
    void testSetUnknownFlag() {
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.of(faction));
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(true);

        cmdFlagSet.execute(ctx("notaflag", "on"));

        verify(flagService, never()).setFlag(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        verify(player).sendMessage(argThat(componentContains("Unknown flag")));
    }

    @StorageTest
    @DisplayName("flag set — locked flag blocked")
    void testSetLockedFlag() {
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.of(faction));
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(true);
        when(flagService.isFlagEditable(FactionFlag.OPEN)).thenReturn(false);

        cmdFlagSet.execute(ctx("open", "on"));

        verify(flagService, never()).setFlag(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        verify(player).sendMessage(argThat(componentContains("locked")));
    }

    @StorageTest
    @DisplayName("flag set — sets to true with 'on'")
    void testSetFlagOn() {
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.of(faction));
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(true);
        when(flagService.isFlagEditable(FactionFlag.PVP)).thenReturn(true);

        cmdFlagSet.execute(ctx("pvp", "on"));

        verify(flagService).setFlag(eq(faction), eq(FactionFlag.PVP), eq(true));
    }

    @StorageTest
    @DisplayName("flag set — sets to false with 'off'")
    void testSetFlagOff() {
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.of(faction));
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(true);
        when(flagService.isFlagEditable(FactionFlag.PVP)).thenReturn(true);

        cmdFlagSet.execute(ctx("pvp", "off"));

        verify(flagService).setFlag(eq(faction), eq(FactionFlag.PVP), eq(false));
    }

    @StorageTest
    @DisplayName("flag set — no value arg toggles flag")
    void testSetFlagToggle() {
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.of(faction));
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(true);
        when(flagService.isFlagEditable(FactionFlag.EXPLOSIONS)).thenReturn(true);
        when(flagService.getFlag(faction, FactionFlag.EXPLOSIONS)).thenReturn(false);

        cmdFlagSet.execute(ctx("explosions"));

        verify(flagService).setFlag(eq(faction), eq(FactionFlag.EXPLOSIONS), eq(true));
    }

    // =========================================================================
    // CmdFlagList
    // =========================================================================

    @StorageTest
    @DisplayName("flag list — not in faction")
    void testListNotInFaction() {
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.empty());

        cmdFlagList.execute(ctx());

        verify(flagService, never()).getAllFlags(Mockito.any());
    }

    @StorageTest
    @DisplayName("flag list — shows all flags")
    void testListShowsFlags() {
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.of(faction));

        final Map<FactionFlag, Boolean> flags = new EnumMap<>(FactionFlag.class);
        for (final FactionFlag flag : FactionFlag.values()) {
            flags.put(flag, flag.getDefaultValue());
        }
        when(flagService.getAllFlags(faction)).thenReturn(flags);
        when(flagService.isFlagEditable(Mockito.any())).thenReturn(true);

        cmdFlagList.execute(ctx());

        verify(flagService).getAllFlags(faction);
    }
}

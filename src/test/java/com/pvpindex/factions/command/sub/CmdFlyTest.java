package com.pvpindex.factions.command.sub;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import java.util.Optional;
import java.util.UUID;
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
@DisplayName("CmdFly — /f fly")
class CmdFlyTest extends CommandTestBase {


    @Mock private FactionService factionService;
    @Mock private FactionModel faction;


    private CmdFly cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();


    @BeforeEach
    void setUp() {
        cmd = new CmdFly(factionService);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
    }


    @StorageTest
    @DisplayName("success — fly enabled")
    void testFlyEnabled() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(config.isFlyEnabled()).thenReturn(true);
        when(config.isFlyRequireOwnTerritory()).thenReturn(false);
        when(factionService.isFactionFlyEnabled(uuid)).thenReturn(false);
        when(factionService.setFactionFlyEnabled(uuid, true)).thenReturn(true);


        cmd.execute(ctx());


        verify(factionService).setFactionFlyEnabled(uuid, true);
        verify(player).setAllowFlight(true);
        verify(player).sendMessage(argThat(componentContains("enabled")));
    }


    @StorageTest
    @DisplayName("success — fly disabled")
    void testFlyDisabled() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(config.isFlyEnabled()).thenReturn(true);
        when(config.isFlyRequireOwnTerritory()).thenReturn(false);
        when(factionService.isFactionFlyEnabled(uuid)).thenReturn(true);
        when(factionService.setFactionFlyEnabled(uuid, false)).thenReturn(true);
        when(player.isFlying()).thenReturn(true);


        cmd.execute(ctx());


        verify(factionService).setFactionFlyEnabled(uuid, false);
        verify(player).setAllowFlight(false);
        verify(player).sendMessage(argThat(componentContains("disabled")));
    }


    @StorageTest
    @DisplayName("not in faction — rejected")
    void testNotInFaction() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.empty());


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("not in a faction")));
        verify(factionService, never()).setFactionFlyEnabled(uuid, true);
    }


    @StorageTest
    @DisplayName("fly feature disabled — rejected")
    void testFlyFeatureDisabled() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(config.isFlyEnabled()).thenReturn(false);


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("disabled")));
        verify(factionService, never()).setFactionFlyEnabled(uuid, true);
    }
}

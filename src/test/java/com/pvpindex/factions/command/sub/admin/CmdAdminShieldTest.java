package com.pvpindex.factions.command.sub.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.repository.FactionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdAdminShield — /fa shield <faction> <action>")
class CmdAdminShieldTest extends CommandTestBase {

    @Mock private FactionRepository factionRepository;
    @Mock private FactionModel faction;

    private CmdAdminShield cmd;

    @BeforeEach
    void setUp() throws Exception {
        cmd = new CmdAdminShield();
        when(repos.factions()).thenReturn(factionRepository);
        when(faction.getName()).thenReturn("Alpha");
        when(config.getWarShieldMaxDurationHours()).thenReturn(8);
    }

    @StorageTest
    @DisplayName("war shields disabled — feature-disabled message")
    void testShieldsDisabled() throws Exception {
        when(config.isWarShieldEnabled()).thenReturn(false);

        cmd.execute(ctx("Alpha", "clear"));

        verify(player).sendMessage(argThat(componentContains("not enabled")));
        verify(factionRepository, never()).save(any());
    }

    @StorageTest
    @DisplayName("faction not found — error message")
    void testFactionNotFound() throws Exception {
        when(config.isWarShieldEnabled()).thenReturn(true);
        when(factionRepository.findAll()).thenReturn(List.of());

        cmd.execute(ctx("Alpha", "clear"));

        verify(player).sendMessage(argThat(componentContains("not found")));
    }

    @StorageTest
    @DisplayName("clear action — shield cleared")
    void testClearShield() throws Exception {
        when(config.isWarShieldEnabled()).thenReturn(true);
        when(factionRepository.findAll()).thenReturn(List.of(faction));

        cmd.execute(ctx("Alpha", "clear"));

        verify(faction).setShieldStartHour(null);
        verify(faction).setShieldDurationHours(0);
        verify(factionRepository).save(faction);
        verify(player).sendMessage(argThat(componentContains("cleared")));
    }

    @StorageTest
    @DisplayName("valid hour and duration — shield set")
    void testSetShield() throws Exception {
        when(config.isWarShieldEnabled()).thenReturn(true);
        when(factionRepository.findAll()).thenReturn(List.of(faction));

        cmd.execute(ctx("Alpha", "20", "4"));

        verify(faction).setShieldStartHour(20);
        verify(faction).setShieldDurationHours(4);
        verify(factionRepository).save(faction);
        verify(player).sendMessage(argThat(componentContains("shield")));
    }
}

package com.pvpindex.factions.command.sub;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
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
@DisplayName("CmdMotd — /f motd [text | clear]")
class CmdMotdTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private FactionModel faction;

    private CmdMotd cmd;
    private final UUID uuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cmd = new CmdMotd(factionService);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(UUID.randomUUID().toString());
        when(faction.getName()).thenReturn("Alpha");
    }

    @StorageTest
    @DisplayName("no args — shows current MOTD when set")
    void testShowMotd() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(faction.getMotd()).thenReturn("Welcome to Alpha!");

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("Welcome to Alpha!")));
    }

    @StorageTest
    @DisplayName("no args — shows none message when MOTD is empty")
    void testShowMotdEmpty() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(faction.getMotd()).thenReturn(null);

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("no MOTD")));
    }

    @StorageTest
    @DisplayName("set MOTD — success")
    void testSetMotd() {
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(factionService.setFactionMotd(eq(uuid), any())).thenReturn(true);

        cmd.execute(ctx("Hello", "world", "from", "Alpha"));

        verify(factionService).setFactionMotd(uuid, "Hello world from Alpha");
        verify(player).sendMessage(argThat(componentContains("updated")));
    }

    @StorageTest
    @DisplayName("clear MOTD — success")
    void testClearMotd() {
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(factionService.setFactionMotd(uuid, "")).thenReturn(true);

        cmd.execute(ctx("clear"));

        verify(player).sendMessage(argThat(componentContains("cleared")));
    }

    @StorageTest
    @DisplayName("MOTD too long — rejected")
    void testMotdTooLong() {
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        final String tooLong = "a".repeat(251);

        cmd.execute(ctx(tooLong));

        verify(player).sendMessage(argThat(componentContains("too long")));
        verify(factionService, never()).setFactionMotd(any(), any());
    }

    @StorageTest
    @DisplayName("not officer — rejected")
    void testNotOfficer() {
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(false);
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));

        cmd.execute(ctx("Some text"));

        verify(factionService, never()).setFactionMotd(any(), any());
    }

    @StorageTest
    @DisplayName("service failure — error message shown")
    void testServiceFailure() {
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(factionService.setFactionMotd(eq(uuid), any())).thenReturn(false);

        cmd.execute(ctx("Hello"));

        verify(player).sendMessage(argThat(componentContains("Could not")));
    }
}

package com.pvpindex.factions.command.sub.power;


import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.PowerHistoryModel;
import com.pvpindex.factions.data.repository.PowerHistoryRepository;
import java.util.List;
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
@DisplayName("CmdPowerHistory — /f powerhistory")
class CmdPowerHistoryTest extends CommandTestBase {


    @Mock private PowerHistoryRepository powerHistoryRepo;


    private CmdPowerHistory cmd;
    private final UUID uuid = UUID.randomUUID();


    @BeforeEach
    void setUp() {
        cmd = new CmdPowerHistory();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("TestPlayer");
        when(repos.powerHistory()).thenReturn(powerHistoryRepo);
    }


    private PowerHistoryModel buildEntry(final String reason, final double delta, final double after) {
        final PowerHistoryModel m = new PowerHistoryModel(UUID.randomUUID().toString());
        m.setPlayerUuid(uuid.toString());
        m.setReason(reason);
        m.setDelta(delta);
        m.setPowerAfter(after);
        m.setCreatedAt(System.currentTimeMillis());
        return m;
    }


    @StorageTest
    @DisplayName("own history — shows entries")
    void testOwnHistoryShowsEntries() throws Exception {
        when(powerHistoryRepo.findRecentByPlayerUuid(anyString(), anyInt(), anyInt()))
            .thenReturn(List.of(
                buildEntry("DEATH", -4.0, 6.0),
                buildEntry("KILL", 2.0, 8.0)
            ));


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("TestPlayer")));
        verify(player).sendMessage(argThat(componentContains("DEATH")));
        verify(player).sendMessage(argThat(componentContains("KILL")));
    }


    @StorageTest
    @DisplayName("own history — empty results show empty message")
    void testOwnHistoryEmpty() throws Exception {
        when(powerHistoryRepo.findRecentByPlayerUuid(anyString(), anyInt(), anyInt()))
            .thenReturn(List.of());


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("No power history")));
    }


    @StorageTest
    @DisplayName("own history with page number — uses correct offset")
    void testOwnHistoryWithPage() throws Exception {
        when(powerHistoryRepo.findRecentByPlayerUuid(uuid.toString(), 10, 10))
            .thenReturn(List.of(buildEntry("BUY", 5.0, 15.0)));


        cmd.execute(ctx("2"));


        verify(powerHistoryRepo).findRecentByPlayerUuid(uuid.toString(), 10, 10);
    }


    @StorageTest
    @DisplayName("other player — no permission denied")
    void testOtherPlayerNoPermission() throws Exception {
        when(player.hasPermission("factions.cmd.power.history.other")).thenReturn(false);


        cmd.execute(ctx("OtherPlayer"));


        verify(player).sendMessage(argThat(componentContains("permission")));
    }
}

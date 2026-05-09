package com.gyvex.pvpindex.factions.command.sub;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.gyvex.pvpindex.factions.command.CommandTestBase;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.data.model.PlayerModel;
import com.gyvex.pvpindex.factions.data.repository.BoardRepository;
import com.gyvex.pvpindex.factions.data.repository.PlayerRepository;
import com.gyvex.pvpindex.factions.service.FactionService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.command.CommandSender;
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
@DisplayName("CmdInfo — /f info [name]")
class CmdInfoTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private FactionModel faction;
    @Mock private PlayerRepository playerRepository;
    @Mock private BoardRepository boardRepository;

    private CmdInfo cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() throws StorageException {
        cmd = new CmdInfo(factionService);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
        when(faction.getName()).thenReturn("Alpha");
        when(faction.getOwnerId()).thenReturn("owner-uuid");
        when(faction.getBank()).thenReturn(1000.0);
        when(faction.hasHome()).thenReturn(false);
        when(faction.getDescription()).thenReturn("");
        when(config.getMaxMembers()).thenReturn(50);
        when(config.getMaxPower()).thenReturn(10.0);
        when(repos.players()).thenReturn(playerRepository);
        when(repos.board()).thenReturn(boardRepository);
        final PlayerModel p1 = new PlayerModel("p1");
        p1.setPower(5.0);
        final PlayerModel p2 = new PlayerModel("p2");
        p2.setPower(7.5);
        final PlayerModel p3 = new PlayerModel("p3");
        p3.setPower(2.5);
        when(playerRepository.findByFactionId(factionId)).thenReturn(List.of(p1, p2, p3));
        when(boardRepository.countByFactionId(factionId)).thenReturn(5);
    }

    @Test
    @DisplayName("player — own faction info shown")
    void testPlayerOwnFaction() throws StorageException {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("Alpha")));
        verify(player).sendMessage(argThat(componentContains("Members")));
        verify(player).sendMessage(argThat(componentContains("Leader")));
        verify(player).sendMessage(argThat(componentContains("Power")));
        verify(player).sendMessage(argThat(componentContains("Land")));
        verify(player).sendMessage(argThat(componentContains("Bank")));
        verify(player).sendMessage(argThat(componentContains("Home")));
    }

    @Test
    @DisplayName("player — named faction info shown")
    void testPlayerNamedFaction() throws StorageException {
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.of(faction));

        cmd.execute(ctx("Alpha"));

        verify(player).sendMessage(argThat(componentContains("Alpha")));
    }

    @Test
    @DisplayName("faction not found — error message")
    void testFactionNotFound() {
        when(factionService.getFactionByName("Unknown")).thenReturn(Optional.empty());

        cmd.execute(ctx("Unknown"));

        verify(player).sendMessage(argThat(componentContains("not found")));
    }

    @Test
    @DisplayName("console with name arg — faction info shown")
    void testConsoleWithName() throws StorageException {
        final CommandSender console = org.mockito.Mockito.mock(CommandSender.class);
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.of(faction));

        cmd.execute(ctx(console, "Alpha"));

        verify(console).sendMessage(argThat(componentContains("Alpha")));
    }

    @Test
    @DisplayName("console without arg — usage shown")
    void testConsoleNoArg() {
        final CommandSender console = org.mockito.Mockito.mock(CommandSender.class);

        cmd.execute(ctx(console));

        verify(console).sendMessage(argThat(componentContains("Usage")));
        verify(factionService, never()).getFactionByName(any());
    }

    @Test
    @DisplayName("storage error — graceful error message")
    void testStorageException() throws StorageException {
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.of(faction));
        when(playerRepository.findByFactionId(anyString()))
            .thenThrow(new StorageException("disk error"));

        cmd.execute(ctx("Alpha"));

        verify(player).sendMessage(argThat(componentContains("error")));
        verify(logger).severe(anyString());
    }
}

package com.gyvex.pvpindex.factions.command.sub.bank;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gyvex.pvpindex.factions.command.CommandTestBase;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.engine.EngineEconomy;
import com.gyvex.pvpindex.factions.service.FactionService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CmdBankTransferTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private EngineEconomy economy;
    @Mock private FactionModel source;
    @Mock private FactionModel target;

    private CmdBankTransfer cmd;
    private final UUID actor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cmd = new CmdBankTransfer(factionService, economy);
        when(player.getUniqueId()).thenReturn(actor);
        when(factionService.getFactionByPlayer(actor)).thenReturn(Optional.of(source));
        when(factionService.isOfficerOrAbove(actor)).thenReturn(true);
        when(source.getId()).thenReturn("A");
        when(target.getId()).thenReturn("B");
    }

    @Test
    void transferSuccess() {
        when(factionService.getFactionByName("Beta")).thenReturn(Optional.of(target));
        when(target.getName()).thenReturn("Beta");
        when(economy.transfer(actor, "A", "B", 50.0)).thenReturn(true);
        cmd.execute(ctx("Beta", "50"));
        verify(player).sendMessage(argThat(componentContains("Transferred")));
    }

    @Test
    void transferShorthand() {
        when(factionService.getFactionByName("Beta")).thenReturn(Optional.of(target));
        when(target.getName()).thenReturn("Beta");
        when(economy.transfer(actor, "A", "B", 10_000_000_000_000d)).thenReturn(true);
        cmd.execute(ctx("Beta", "10t"));
        verify(player).sendMessage(argThat(componentContains("Transferred")));
    }

    @Test
    void transferInvalidAmount() {
        when(factionService.getFactionByName("Beta")).thenReturn(Optional.of(target));
        cmd.execute(ctx("Beta", "nope"));
        verify(player).sendMessage(argThat(componentContains("Invalid amount")));
        verify(economy, never()).transfer(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), anyDouble());
    }
}

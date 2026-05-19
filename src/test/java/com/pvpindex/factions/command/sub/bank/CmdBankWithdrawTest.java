package com.pvpindex.factions.command.sub.bank;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.engine.EngineEconomy;
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
@DisplayName("CmdBankWithdraw — /f bank withdraw <amount>")
class CmdBankWithdrawTest extends CommandTestBase {


    @Mock private FactionService factionService;
    @Mock private EngineEconomy engineEconomy;
    @Mock private FactionModel faction;


    private CmdBankWithdraw cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();


    @BeforeEach
    void setUp() {
        cmd = new CmdBankWithdraw(factionService, engineEconomy);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
    }


    @StorageTest
    @DisplayName("success — withdraws valid amount")
    void testWithdrawSuccess() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));


        cmd.execute(ctx("50.0"));


        verify(engineEconomy).withdraw(eq(player), eq(factionId), eq(50.0));
    }


    @StorageTest
    @DisplayName("success — parses shorthand amount")
    void testWithdrawShorthand() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));


        cmd.execute(ctx("2m"));


        verify(engineEconomy).withdraw(eq(player), eq(factionId), eq(2_000_000.0));
    }


    @StorageTest
    @DisplayName("not in faction — rejected")
    void testNotInFaction() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.empty());


        cmd.execute(ctx("50.0"));


        verify(player).sendMessage(argThat(componentContains("not in a faction")));
        verify(engineEconomy, never()).withdraw(any(), any(), any(double.class));
    }


    @StorageTest
    @DisplayName("invalid amount (text) — rejected")
    void testInvalidAmountNaN() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));


        cmd.execute(ctx("abc"));


        verify(player).sendMessage(argThat(componentContains("Invalid amount")));
        verify(engineEconomy, never()).withdraw(any(), any(), any(double.class));
    }


    @StorageTest
    @DisplayName("zero amount — rejected")
    void testZeroAmount() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));


        cmd.execute(ctx("0"));


        verify(player).sendMessage(argThat(componentContains("positive")));
        verify(engineEconomy, never()).withdraw(any(), any(), any(double.class));
    }


    @StorageTest
    @DisplayName("negative amount — rejected")
    void testNegativeAmount() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));


        cmd.execute(ctx("-1"));


        verify(player).sendMessage(argThat(componentContains("positive")));
        verify(engineEconomy, never()).withdraw(any(), any(), any(double.class));
    }


    @StorageTest
    @DisplayName("missing arg — usage shown")
    void testMissingArg() {
        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("Usage")));
        verify(factionService, never()).getFactionByPlayer(any());
    }
}

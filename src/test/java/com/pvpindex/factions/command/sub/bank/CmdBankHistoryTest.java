package com.pvpindex.factions.command.sub.bank;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.repository.BankTransactionRepository;
import com.pvpindex.factions.service.FactionService;
import java.util.List;
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
@DisplayName("CmdBankHistory — /f bank history")
class CmdBankHistoryTest extends CommandTestBase {


    @Mock private FactionService factionService;
    @Mock private FactionModel faction;
    @Mock private BankTransactionRepository bankTransactionRepository;


    private CmdBankHistory cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();


    @BeforeEach
    void setUp() {
        cmd = new CmdBankHistory(factionService);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
        when(config.getBankHistoryPageSize()).thenReturn(5);
        when(repos.bankTransactions()).thenReturn(bankTransactionRepository);
    }


    @StorageTest
    @DisplayName("not in faction — rejected")
    void testNotInFaction() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.empty());


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("not in a faction")));
    }


    @StorageTest
    @DisplayName("no transactions — empty message shown")
    void testNoTransactions() throws Exception {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(bankTransactionRepository.findRecentByFactionId(factionId, 5, 0)).thenReturn(List.of());


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("No bank transactions")));
    }
}

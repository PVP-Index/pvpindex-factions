package com.pvpindex.factions.command.sub.bank;


import static org.mockito.ArgumentMatchers.argThat;
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
@DisplayName("CmdBank — /f bank")
class CmdBankTest extends CommandTestBase {


    @Mock private FactionService factionService;
    @Mock private EngineEconomy engineEconomy;
    @Mock private FactionModel faction;


    private CmdBank cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();


    @BeforeEach
    void setUp() {
        cmd = new CmdBank(factionService, engineEconomy);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
        when(faction.getBank()).thenReturn(250.0);
    }


    @StorageTest
    @DisplayName("shows bank balance when in faction")
    void testShowsBalance() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("250.00")));
    }


    @StorageTest
    @DisplayName("not in faction — rejected")
    void testNotInFaction() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.empty());


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("not in a faction")));
        verify(player, never()).sendMessage(argThat(componentContains("balance")));
    }
}

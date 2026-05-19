package com.pvpindex.factions.command.sub;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
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
@DisplayName("CmdList — /f list")
class CmdListTest extends CommandTestBase {


    @Mock private FactionService factionService;
    @Mock private FactionModel faction;


    private CmdList cmd;
    private final String factionId = UUID.randomUUID().toString();


    @BeforeEach
    void setUp() {
        cmd = new CmdList(factionService);
        when(config.getListPageSize()).thenReturn(10);
        when(faction.getName()).thenReturn("Alpha");
        when(faction.getId()).thenReturn(factionId);
        when(faction.getBank()).thenReturn(0.0);
        when(faction.getPowerBoost()).thenReturn(0.0);
    }


    @StorageTest
    @DisplayName("no factions — empty message")
    void testNoFactions() {
        when(factionService.getAllFactions()).thenReturn(List.of());


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("No factions")));
    }


    @StorageTest
    @DisplayName("has factions — faction name shown")
    void testFactionListed() {
        when(factionService.getAllFactions()).thenReturn(List.of(faction));


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("Alpha")));
    }


    @StorageTest
    @DisplayName("sort by bank — runs without error")
    void testSortByBank() {
        when(factionService.getAllFactions()).thenReturn(List.of(faction));


        cmd.execute(ctx("1", "bank"));


        verify(player).sendMessage(argThat(componentContains("Alpha")));
    }
}

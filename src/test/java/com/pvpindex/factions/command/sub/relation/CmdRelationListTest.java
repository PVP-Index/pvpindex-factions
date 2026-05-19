package com.pvpindex.factions.command.sub.relation;


import static org.mockito.ArgumentMatchers.argThat;
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
@DisplayName("CmdRelationList — /f relation list")
class CmdRelationListTest extends CommandTestBase {


    @Mock private FactionService factionService;
    @Mock private FactionModel faction;


    private CmdRelationList cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();


    @BeforeEach
    void setUp() {
        cmd = new CmdRelationList(factionService);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
    }


    @StorageTest
    @DisplayName("not in faction — rejected")
    void testNotInFaction() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.empty());


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("not in a faction")));
    }


    @StorageTest
    @DisplayName("empty relations — no entries message shown")
    void testEmptyRelations() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(faction.getRelationsJson()).thenReturn("{}");


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("No relation entries")));
    }
}

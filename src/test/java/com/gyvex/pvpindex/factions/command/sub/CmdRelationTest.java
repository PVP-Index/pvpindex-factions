package com.gyvex.pvpindex.factions.command.sub;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gyvex.pvpindex.factions.Relation;
import com.gyvex.pvpindex.factions.command.CommandTestBase;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.service.FactionService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CmdRelationTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private FactionModel sourceFaction;
    @Mock private FactionModel targetFaction;

    private CmdRelation cmd;
    private final UUID uuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cmd = new CmdRelation(factionService);
        when(player.getUniqueId()).thenReturn(uuid);
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(sourceFaction));
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        when(sourceFaction.getId()).thenReturn("A");
        when(targetFaction.getId()).thenReturn("B");
        when(targetFaction.getName()).thenReturn("Beta");
    }

    @Test
    void setsRelation() {
        when(factionService.getFactionByName("Beta")).thenReturn(Optional.of(targetFaction));
        when(factionService.setRelation(uuid, "Beta", Relation.ALLY)).thenReturn(Optional.of(Relation.ALLY));

        cmd.execute(ctx("Beta", "ally"));

        verify(player).sendMessage(argThat(componentContains("set to")));
    }
}


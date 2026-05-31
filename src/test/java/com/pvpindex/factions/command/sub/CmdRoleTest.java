package com.pvpindex.factions.command.sub;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.service.FactionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdRole — /f role ...")
class CmdRoleTest extends CommandTestBase {

    @Mock private FactionService factionService;

    @StorageTest
    @DisplayName("list subcommand renders roles")
    void listRendersRoles() {
        final UUID actorId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(actorId);

        final RankModel owner = new RankModel(UUID.randomUUID().toString());
        owner.setName("Owner");
        owner.setPriority(100);
        final RankModel scout = new RankModel(UUID.randomUUID().toString());
        scout.setName("Scout");
        scout.setPriority(30);
        scout.setPrefix("<gray>[S]</gray>");
        when(factionService.getFactionByPlayer(actorId)).thenReturn(
            java.util.Optional.of(new com.pvpindex.factions.data.model.FactionModel(UUID.randomUUID().toString())));
        when(factionService.listRoles(actorId)).thenReturn(List.of(owner, scout));

        final CmdRole cmd = new CmdRole(factionService);
        cmd.execute(ctx("list"));

        verify(player).sendMessage(argThat(componentContains("Faction Roles")));
        verify(player).sendMessage(argThat(componentContains("Scout")));
    }
}

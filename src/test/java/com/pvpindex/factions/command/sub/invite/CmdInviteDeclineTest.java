package com.pvpindex.factions.command.sub.invite;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.InviteService;
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
@DisplayName("CmdInviteDecline — /f invite decline <faction>")
class CmdInviteDeclineTest extends CommandTestBase {


    @Mock private InviteService inviteService;
    @Mock private FactionService factionService;
    @Mock private FactionModel faction;


    private CmdInviteDecline cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();


    @BeforeEach
    void setUp() {
        cmd = new CmdInviteDecline(inviteService, factionService);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
        when(faction.getName()).thenReturn("Alpha");
    }


    @StorageTest
    @DisplayName("success — invite declined")
    void testDeclineSuccess() {
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.of(faction));
        when(inviteService.declineInvite(factionId, uuid)).thenReturn(true);


        cmd.execute(ctx("Alpha"));


        verify(player).sendMessage(argThat(componentContains("declined")));
    }


    @StorageTest
    @DisplayName("faction not found — rejected")
    void testFactionNotFound() {
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.empty());


        cmd.execute(ctx("Alpha"));


        verify(player).sendMessage(argThat(componentContains("not found")));
        verify(inviteService, never()).declineInvite(factionId, uuid);
    }


    @StorageTest
    @DisplayName("no invite for faction — rejected")
    void testNoInvite() {
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.of(faction));
        when(inviteService.declineInvite(factionId, uuid)).thenReturn(false);


        cmd.execute(ctx("Alpha"));


        verify(player).sendMessage(argThat(componentContains("do not have")));
    }
}

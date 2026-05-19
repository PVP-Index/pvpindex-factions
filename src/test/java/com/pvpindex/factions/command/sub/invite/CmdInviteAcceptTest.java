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
@DisplayName("CmdInviteAccept — /f invite accept <faction>")
class CmdInviteAcceptTest extends CommandTestBase {


    @Mock private InviteService inviteService;
    @Mock private FactionService factionService;
    @Mock private FactionModel faction;


    private CmdInviteAccept cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();


    @BeforeEach
    void setUp() {
        cmd = new CmdInviteAccept(inviteService, factionService);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
        when(faction.getName()).thenReturn("Alpha");
    }


    @StorageTest
    @DisplayName("success — joins faction")
    void testJoinsSuccessfully() {
        when(factionService.isInFaction(uuid)).thenReturn(false);
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.of(faction));
        when(inviteService.acceptInvite(factionId, uuid)).thenReturn(Optional.of(faction));


        cmd.execute(ctx("Alpha"));


        verify(player).sendMessage(argThat(componentContains("joined")));
    }


    @StorageTest
    @DisplayName("already in faction — rejected")
    void testAlreadyInFaction() {
        when(factionService.isInFaction(uuid)).thenReturn(true);


        cmd.execute(ctx("Alpha"));


        verify(player).sendMessage(argThat(componentContains("already in")));
        verify(inviteService, never()).acceptInvite(factionId, uuid);
    }


    @StorageTest
    @DisplayName("faction not found — rejected")
    void testFactionNotFound() {
        when(factionService.isInFaction(uuid)).thenReturn(false);
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.empty());


        cmd.execute(ctx("Alpha"));


        verify(player).sendMessage(argThat(componentContains("not found")));
        verify(inviteService, never()).acceptInvite(factionId, uuid);
    }


    @StorageTest
    @DisplayName("no invite — rejected")
    void testNoInvite() {
        when(factionService.isInFaction(uuid)).thenReturn(false);
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.of(faction));
        when(inviteService.acceptInvite(factionId, uuid)).thenReturn(Optional.empty());


        cmd.execute(ctx("Alpha"));


        verify(player).sendMessage(argThat(componentContains("do not have")));
    }
}

package com.pvpindex.factions.command.sub.invite;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.InviteService;
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
@DisplayName("CmdInviteList — /f invite list")
class CmdInviteListTest extends CommandTestBase {


    @Mock private FactionService factionService;
    @Mock private InviteService inviteService;


    private CmdInviteList cmd;
    private final UUID uuid = UUID.randomUUID();


    @BeforeEach
    void setUp() {
        cmd = new CmdInviteList(factionService, inviteService);
        when(player.getUniqueId()).thenReturn(uuid);
    }


    @StorageTest
    @DisplayName("no pending invites — empty message shown")
    void testNoPendingInvites() {
        when(inviteService.listActiveInvitesForPlayer(uuid)).thenReturn(List.of());


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("no pending")));
    }


    @StorageTest
    @DisplayName("faction not found for admin view — error message shown")
    void testFactionNotFoundForAdminView() {
        when(factionService.getFactionByName("Alpha")).thenReturn(java.util.Optional.empty());


        cmd.execute(ctx("Alpha"));


        verify(player).sendMessage(argThat(componentContains("not found")));
    }
}

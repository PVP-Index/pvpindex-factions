package com.pvpindex.factions.command.sub.invite;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.service.InviteService;
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
@DisplayName("CmdInviteDeclineAll — /f invite declineall")
class CmdInviteDeclineAllTest extends CommandTestBase {


    @Mock private InviteService inviteService;


    private CmdInviteDeclineAll cmd;
    private final UUID uuid = UUID.randomUUID();


    @BeforeEach
    void setUp() {
        cmd = new CmdInviteDeclineAll(inviteService);
        when(player.getUniqueId()).thenReturn(uuid);
    }


    @StorageTest
    @DisplayName("no pending invites — empty message shown")
    void testNoPendingInvites() {
        when(inviteService.declineAllInvites(uuid)).thenReturn(0);


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("no pending")));
    }


    @StorageTest
    @DisplayName("invites declined — count shown")
    void testInvitesDeclined() {
        when(inviteService.declineAllInvites(uuid)).thenReturn(3);


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("Declined 3")));
    }
}

package com.pvpindex.factions.command.sub.invite;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.InviteService;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import com.pvpindex.factions.command.StorageTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdInviteRevoke — /f invite revoke <player>")
class CmdInviteRevokeTest extends CommandTestBase {


    @Mock private FactionService factionService;
    @Mock private InviteService inviteService;
    @Mock private FactionModel faction;


    private CmdInviteRevoke cmd;
    private final UUID actorId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();


    @BeforeEach
    void setUp() throws Exception {
        cmd = new CmdInviteRevoke(factionService, inviteService);
        when(player.getUniqueId()).thenReturn(actorId);
        when(faction.getId()).thenReturn(factionId);


        final Server mockServer = Mockito.mock(Server.class);
        final Field serverField = org.bukkit.Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, mockServer);
    }


    @AfterEach
    void tearDown() throws Exception {
        final Field serverField = org.bukkit.Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, null);
    }


    @StorageTest
    @DisplayName("success — invite revoked")
    void testRevokeSuccess() {
        when(factionService.isInFaction(actorId)).thenReturn(true);
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(true);
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.of(faction));
        final OfflinePlayer target = Mockito.mock(OfflinePlayer.class);
        when(target.getUniqueId()).thenReturn(targetId);
        when(org.bukkit.Bukkit.getServer().getOfflinePlayer("Bob")).thenReturn(target);
        when(inviteService.revokeInvite(factionId, targetId)).thenReturn(true);


        cmd.execute(ctx("Bob"));


        verify(player).sendMessage(argThat(componentContains("revoked")));
    }


    @StorageTest
    @DisplayName("not in faction — rejected")
    void testNotInFaction() {
        when(factionService.isInFaction(actorId)).thenReturn(false);


        cmd.execute(ctx("Bob"));


        verify(player).sendMessage(argThat(componentContains("not in a faction")));
        verify(inviteService, never()).revokeInvite(factionId, targetId);
    }


    @StorageTest
    @DisplayName("not officer — rejected")
    void testNotOfficer() {
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.of(faction));
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(false);


        cmd.execute(ctx("Bob"));


        verify(player).sendMessage(argThat(componentContains("officers")));
        verify(inviteService, never()).revokeInvite(factionId, targetId);
    }


    @StorageTest
    @DisplayName("no invite found — error message shown")
    void testNoInvite() {
        when(factionService.isInFaction(actorId)).thenReturn(true);
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(true);
        when(factionService.getFactionByPlayer(actorId)).thenReturn(Optional.of(faction));
        final OfflinePlayer target = Mockito.mock(OfflinePlayer.class);
        when(target.getUniqueId()).thenReturn(targetId);
        when(org.bukkit.Bukkit.getServer().getOfflinePlayer("Bob")).thenReturn(target);
        when(inviteService.revokeInvite(factionId, targetId)).thenReturn(false);


        cmd.execute(ctx("Bob"));


        verify(player).sendMessage(argThat(componentContains("No pending")));
    }
}

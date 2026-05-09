package com.pvpindex.factions.command.sub;

import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdJoin — /f join <faction>")
class CmdJoinTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private InviteService inviteService;
    @Mock private FactionModel faction;

    private CmdJoin cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        cmd = new CmdJoin(factionService, inviteService);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
        when(faction.getName()).thenReturn("Alpha");
    }

    @Test
    @DisplayName("success — joins faction with invite")
    void testJoinSuccess() {
        when(factionService.isInFaction(uuid)).thenReturn(false);
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.of(faction));
        when(inviteService.acceptInvite(factionId, uuid)).thenReturn(Optional.of(faction));

        cmd.execute(ctx("Alpha"));

        verify(player).sendMessage(argThat(componentContains("joined")));
    }

    @Test
    @DisplayName("already in faction — rejected")
    void testAlreadyInFaction() {
        when(factionService.isInFaction(uuid)).thenReturn(true);

        cmd.execute(ctx("Alpha"));

        verify(player).sendMessage(argThat(componentContains("already in a faction")));
        verify(inviteService, never()).acceptInvite(any(), any());
    }

    @Test
    @DisplayName("faction not found — rejected")
    void testFactionNotFound() {
        when(factionService.isInFaction(uuid)).thenReturn(false);
        when(factionService.getFactionByName("Unknown")).thenReturn(Optional.empty());

        cmd.execute(ctx("Unknown"));

        verify(player).sendMessage(argThat(componentContains("not found")));
        verify(inviteService, never()).acceptInvite(any(), any());
    }

    @Test
    @DisplayName("no pending invite — rejected")
    void testNoPendingInvite() {
        when(factionService.isInFaction(uuid)).thenReturn(false);
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.of(faction));
        when(inviteService.acceptInvite(factionId, uuid)).thenReturn(Optional.empty());

        cmd.execute(ctx("Alpha"));

        verify(player).sendMessage(argThat(componentContains("pending invite")));
    }

    @Test
    @DisplayName("missing arg — pending invite list feedback shown")
    void testMissingArg() {
        when(factionService.isInFaction(uuid)).thenReturn(false);
        when(inviteService.listActiveInvitesForPlayer(uuid)).thenReturn(java.util.List.of());

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("no pending invites")));
    }
}

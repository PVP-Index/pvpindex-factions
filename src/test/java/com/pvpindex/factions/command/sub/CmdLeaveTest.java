package com.pvpindex.factions.command.sub;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
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
@DisplayName("CmdLeave — /f leave")
class CmdLeaveTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private FactionModel faction;

    private CmdLeave cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        cmd = new CmdLeave(factionService);
        when(player.getUniqueId()).thenReturn(uuid);
        when(faction.getId()).thenReturn(factionId);
        when(faction.getName()).thenReturn("Alpha");
    }

    @Test
    @DisplayName("success — member leaves faction")
    void testLeaveSuccess() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(faction.isOwner(uuid)).thenReturn(false);
        when(factionService.removeMember(factionId, uuid)).thenReturn(true);

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("left")));
    }

    @Test
    @DisplayName("not in faction — rejected")
    void testNotInFaction() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.empty());

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("not in a faction")));
        verify(factionService, never()).removeMember(any(), any());
    }

    @Test
    @DisplayName("owner can't leave — rejected")
    void testOwnerCannotLeave() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(faction.isOwner(uuid)).thenReturn(true);

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("owner")));
        verify(factionService, never()).removeMember(any(), any());
    }

    @Test
    @DisplayName("removeMember fails — failure message shown")
    void testRemoveFails() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(faction.isOwner(uuid)).thenReturn(false);
        when(factionService.removeMember(factionId, uuid)).thenReturn(false);

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("Failed")));
    }
}

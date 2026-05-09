package com.pvpindex.factions.command.sub;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CmdKickTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private FactionModel faction;

    private CmdKick cmd;
    private final UUID actor = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        cmd = new CmdKick(factionService);
        when(player.getUniqueId()).thenReturn(actor);
        when(factionService.getFactionByPlayer(actor)).thenReturn(Optional.of(faction));
        when(factionService.isOfficerOrAbove(actor)).thenReturn(true);

        final Server mockServer = org.mockito.Mockito.mock(Server.class);
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

    @Test
    void kicksMember() {
        final Player target = org.mockito.Mockito.mock(Player.class);
        when(target.getUniqueId()).thenReturn(targetId);
        when(target.getName()).thenReturn("Bob");
        when(org.bukkit.Bukkit.getServer().getPlayerExact("Bob")).thenReturn(target);
        when(factionService.isOwner(targetId)).thenReturn(false);
        when(factionService.kickMember(actor, targetId)).thenReturn(true);

        cmd.execute(ctx("Bob"));

        verify(player).sendMessage(argThat(componentContains("Kicked")));
        verify(target).sendMessage(argThat(componentContains("kicked")));
    }

    @Test
    void rejectsSelfKick() {
        final Player target = org.mockito.Mockito.mock(Player.class);
        when(target.getUniqueId()).thenReturn(actor);
        when(org.bukkit.Bukkit.getServer().getPlayerExact("Me")).thenReturn(target);

        cmd.execute(ctx("Me"));

        verify(player).sendMessage(argThat(componentContains("cannot kick yourself")));
        verify(factionService, never()).kickMember(actor, actor);
    }
}

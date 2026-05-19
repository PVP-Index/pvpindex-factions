package com.pvpindex.factions.command.sub;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.service.FactionService;
import java.lang.reflect.Field;
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
@DisplayName("CmdDemote — /f demote <player>")
class CmdDemoteTest extends CommandTestBase {


    @Mock private FactionService factionService;


    private CmdDemote cmd;
    private final UUID actorId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();


    @BeforeEach
    void setUp() throws Exception {
        cmd = new CmdDemote(factionService);
        when(player.getUniqueId()).thenReturn(actorId);


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
    @DisplayName("success — member demoted")
    void testDemoteSuccess() {
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(true);
        final OfflinePlayer target = Mockito.mock(OfflinePlayer.class);
        when(target.getUniqueId()).thenReturn(targetId);
        when(org.bukkit.Bukkit.getServer().getOfflinePlayer("Bob")).thenReturn(target);
        when(factionService.demoteMember(actorId, targetId)).thenReturn(true);


        cmd.execute(ctx("Bob"));


        verify(player).sendMessage(argThat(componentContains("Demoted")));
    }


    @StorageTest
    @DisplayName("not officer — rejected")
    void testNotOfficer() {
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(false);


        cmd.execute(ctx("Bob"));


        verify(player).sendMessage(argThat(componentContains("officers")));
        verify(factionService, never()).demoteMember(actorId, targetId);
    }


    @StorageTest
    @DisplayName("service failure — error message sent")
    void testServiceFailure() {
        when(factionService.isOfficerOrAbove(actorId)).thenReturn(true);
        final OfflinePlayer target = Mockito.mock(OfflinePlayer.class);
        when(target.getUniqueId()).thenReturn(targetId);
        when(org.bukkit.Bukkit.getServer().getOfflinePlayer("Bob")).thenReturn(target);
        when(factionService.demoteMember(actorId, targetId)).thenReturn(false);


        cmd.execute(ctx("Bob"));


        verify(player).sendMessage(argThat(componentContains("Could not")));
    }
}

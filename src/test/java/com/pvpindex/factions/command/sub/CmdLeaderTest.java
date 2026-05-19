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
@DisplayName("CmdLeader — /f leader <player>")
class CmdLeaderTest extends CommandTestBase {


    @Mock private FactionService factionService;


    private CmdLeader cmd;
    private final UUID ownerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();


    @BeforeEach
    void setUp() throws Exception {
        cmd = new CmdLeader(factionService);
        when(player.getUniqueId()).thenReturn(ownerId);


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
    @DisplayName("success — ownership transferred to other player")
    void testTransferToOther() {
        when(factionService.isOwner(ownerId)).thenReturn(true);
        final OfflinePlayer target = Mockito.mock(OfflinePlayer.class);
        when(target.getUniqueId()).thenReturn(targetId);
        when(org.bukkit.Bukkit.getServer().getOfflinePlayer("NewLeader")).thenReturn(target);
        when(factionService.transferOwnership(ownerId, targetId)).thenReturn(true);


        cmd.execute(ctx("NewLeader"));


        verify(player).sendMessage(argThat(componentContains("transferred")));
    }


    @StorageTest
    @DisplayName("not owner — rejected")
    void testNotOwner() {
        when(factionService.isOwner(ownerId)).thenReturn(false);


        cmd.execute(ctx("NewLeader"));


        verify(player).sendMessage(argThat(componentContains("owner")));
        verify(factionService, never()).transferOwnership(ownerId, targetId);
    }


    @StorageTest
    @DisplayName("self transfer without confirm — rejected")
    void testSelfTransferRequiresConfirm() {
        when(factionService.isOwner(ownerId)).thenReturn(true);
        final OfflinePlayer self = Mockito.mock(OfflinePlayer.class);
        when(self.getUniqueId()).thenReturn(ownerId);
        when(player.getName()).thenReturn("SelfPlayer");
        when(org.bukkit.Bukkit.getServer().getOfflinePlayer("SelfPlayer")).thenReturn(self);


        cmd.execute(ctx("SelfPlayer"));


        verify(player).sendMessage(argThat(componentContains("confirm")));
        verify(factionService, never()).transferOwnership(ownerId, ownerId);
    }


    @StorageTest
    @DisplayName("service failure — error message sent")
    void testServiceFailure() {
        when(factionService.isOwner(ownerId)).thenReturn(true);
        final OfflinePlayer target = Mockito.mock(OfflinePlayer.class);
        when(target.getUniqueId()).thenReturn(targetId);
        when(org.bukkit.Bukkit.getServer().getOfflinePlayer("NewLeader")).thenReturn(target);
        when(factionService.transferOwnership(ownerId, targetId)).thenReturn(false);


        cmd.execute(ctx("NewLeader"));


        verify(player).sendMessage(argThat(componentContains("Could not")));
    }
}

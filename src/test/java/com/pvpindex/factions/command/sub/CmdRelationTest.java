package com.pvpindex.factions.command.sub;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.pvpindex.factions.Relation;
import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.config.NotificationsConfig;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.integration.ezcountdown.EzCountdownNotifier;
import com.pvpindex.factions.service.FactionService;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import com.pvpindex.factions.command.StorageTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CmdRelationTest extends CommandTestBase {


    @Mock private FactionService factionService;
    @Mock private FactionModel sourceFaction;
    @Mock private FactionModel targetFaction;
    @Mock private EzCountdownNotifier ezCountdownNotifier;
    @Mock private NotificationsConfig notificationsConfig;


    private CmdRelation cmd;
    private final UUID uuid = UUID.randomUUID();


    @BeforeEach
    void setUp() throws Exception {
        cmd = new CmdRelation(factionService);
        final Server mockServer = mock(Server.class);
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
    void setsRelation() {
        when(player.getUniqueId()).thenReturn(uuid);
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(sourceFaction));
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        when(sourceFaction.getId()).thenReturn("A");
        when(targetFaction.getId()).thenReturn("B");
        when(targetFaction.getName()).thenReturn("Beta");
        when(factionService.getFactionByName("Beta")).thenReturn(Optional.of(targetFaction));
        when(factionService.setRelation(uuid, "Beta", Relation.ALLY)).thenReturn(Optional.of(Relation.ALLY));


        cmd.execute(ctx("Beta", "ally"));


        verify(player).sendMessage(argThat(componentContains("set to")));
    }


    @StorageTest
    void tabCompletesRelationTypeAfterFactionName() {
        final List<String> completions = cmd.tabComplete(ctx("Beta", ""));
        assertTrue(completions.contains("ally"));
        assertTrue(completions.contains("truce"));
        assertTrue(completions.contains("neutral"));
        assertTrue(completions.contains("enemy"));
    }


    @StorageTest
    @SuppressWarnings("unchecked")
    void enemyAnnouncementBroadcastsChatWhenNoEzCountdown() {
        // cmd uses no EzCountdownNotifier (single-arg constructor) — must fall back to chat
        final Player onlinePlayer = mock(Player.class);
        when(org.bukkit.Bukkit.getServer().getOnlinePlayers())
            .thenReturn((Collection) List.of(onlinePlayer));


        when(player.getUniqueId()).thenReturn(uuid);
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(sourceFaction));
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        when(sourceFaction.getId()).thenReturn("A");
        when(sourceFaction.getName()).thenReturn("Alpha");
        when(targetFaction.getId()).thenReturn("B");
        when(targetFaction.getName()).thenReturn("Beta");
        when(factionService.getFactionByName("Beta")).thenReturn(Optional.of(targetFaction));
        when(factionService.setRelation(uuid, "Beta", Relation.ENEMY)).thenReturn(Optional.of(Relation.ENEMY));


        cmd.execute(ctx("Beta", "enemy"));


        verify(onlinePlayer).sendMessage(any(Component.class));
    }


    @StorageTest
    @SuppressWarnings("unchecked")
    void enemyAnnouncementBroadcastsChatWhenEzCountdownDisabledInConfig() {
        cmd = new CmdRelation(factionService, ezCountdownNotifier, notificationsConfig);
        when(ezCountdownNotifier.isEnabled()).thenReturn(true);
        when(notificationsConfig.isEzCountdownEnabled()).thenReturn(false);


        final Player onlinePlayer = mock(Player.class);
        when(org.bukkit.Bukkit.getServer().getOnlinePlayers())
            .thenReturn((Collection) List.of(onlinePlayer));


        when(player.getUniqueId()).thenReturn(uuid);
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(sourceFaction));
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        when(sourceFaction.getId()).thenReturn("A");
        when(sourceFaction.getName()).thenReturn("Alpha");
        when(targetFaction.getId()).thenReturn("B");
        when(targetFaction.getName()).thenReturn("Beta");
        when(factionService.getFactionByName("Beta")).thenReturn(Optional.of(targetFaction));
        when(factionService.setRelation(uuid, "Beta", Relation.ENEMY)).thenReturn(Optional.of(Relation.ENEMY));


        cmd.execute(ctx("Beta", "enemy"));


        verify(onlinePlayer).sendMessage(any(Component.class));
        verify(ezCountdownNotifier, never()).sendAnnouncement(any(), anyLong(), any());
    }


    @StorageTest
    void enemyAnnouncementUsesEzCountdownWhenAvailableAndEnabled() {
        cmd = new CmdRelation(factionService, ezCountdownNotifier, notificationsConfig);
        when(ezCountdownNotifier.isEnabled()).thenReturn(true);
        when(notificationsConfig.isEzCountdownEnabled()).thenReturn(true);
        when(notificationsConfig.getEzCountdownDurationSeconds()).thenReturn(8L);
        when(notificationsConfig.getEzCountdownDisplayTypes()).thenReturn(List.of("ACTION_BAR"));


        when(player.getUniqueId()).thenReturn(uuid);
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(sourceFaction));
        when(factionService.isOfficerOrAbove(uuid)).thenReturn(true);
        when(sourceFaction.getId()).thenReturn("A");
        when(sourceFaction.getName()).thenReturn("Alpha");
        when(targetFaction.getId()).thenReturn("B");
        when(targetFaction.getName()).thenReturn("Beta");
        when(factionService.getFactionByName("Beta")).thenReturn(Optional.of(targetFaction));
        when(factionService.setRelation(uuid, "Beta", Relation.ENEMY)).thenReturn(Optional.of(Relation.ENEMY));


        cmd.execute(ctx("Beta", "enemy"));


        verify(ezCountdownNotifier).sendAnnouncement(
            argThat(s -> s.contains("Alpha") && s.contains("Beta")),
            anyLong(),
            any());
    }
}

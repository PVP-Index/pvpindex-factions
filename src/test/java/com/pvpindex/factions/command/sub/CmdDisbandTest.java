package com.pvpindex.factions.command.sub;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.service.FactionService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
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
@DisplayName("CmdDisband — /f disband")
class CmdDisbandTest extends CommandTestBase {

    @Mock private FactionService factionService;
    @Mock private FactionModel faction;

    private CmdDisband cmd;
    private final UUID uuid = UUID.randomUUID();
    private final String factionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        cmd = new CmdDisband(factionService);
        when(player.getUniqueId()).thenReturn(uuid);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        PredefinedConfigManager.setInstance(null);
    }

    @Test
    @DisplayName("success — owner disbands faction")
    void testDisbandSuccess() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(factionService.isOwner(uuid)).thenReturn(true);
        when(faction.getId()).thenReturn(factionId);
        when(factionService.disbandFaction(factionId)).thenReturn(true);

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("disbanded")));
    }

    @Test
    @DisplayName("not in faction — rejected")
    void testNotInFaction() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.empty());

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("not in a faction")));
        verify(factionService, never()).disbandFaction(any());
    }

    @Test
    @DisplayName("not owner — rejected")
    void testNotOwner() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(faction.isOwner(uuid)).thenReturn(false);

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("owner")));
        verify(factionService, never()).disbandFaction(any());
    }

    @Test
    @DisplayName("disbandFaction fails — failure message shown")
    void testDeleteFails() {
        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(factionService.isOwner(uuid)).thenReturn(true);
        when(faction.getId()).thenReturn(factionId);
        when(factionService.disbandFaction(factionId)).thenReturn(false);

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("Failed")));
    }

    @Test
    @DisplayName("predefined disband blocked when feature enabled")
    void testPredefinedDisbandBlocked() throws IOException {
        final Path dir = Files.createTempDirectory("predefined-disband-test");
        final PredefinedConfigManager manager = new PredefinedConfigManager(dir.toFile(), logger);
        manager.initialize();
        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dir.resolve("pre-defined.yml").toFile());
        cfg.set("enabled", true);
        cfg.set("block-disband", true);
        cfg.set("factions.France.name", "France");
        cfg.save(dir.resolve("pre-defined.yml").toFile());
        manager.reload();
        PredefinedConfigManager.setInstance(manager);

        when(factionService.getFactionByPlayer(uuid)).thenReturn(Optional.of(faction));
        when(factionService.isOwner(uuid)).thenReturn(true);
        when(faction.getName()).thenReturn("France");

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("cannot be disbanded")));
        verify(factionService, never()).disbandFaction(any());
    }

    @Test
    @DisplayName("console sender — rejected")
    void testConsoleSender() {
        final CommandSender console = org.mockito.Mockito.mock(CommandSender.class);
        org.mockito.Mockito.lenient().when(console.hasPermission(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);

        cmd.execute(ctx(console));

        verify(console).sendMessage(argThat(componentContains("player")));
        verify(factionService, never()).getFactionByPlayer(any());
    }
}

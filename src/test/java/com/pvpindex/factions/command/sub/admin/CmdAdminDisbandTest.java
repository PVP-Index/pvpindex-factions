package com.pvpindex.factions.command.sub.admin;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.service.FactionService;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdAdminDisband — /fa disband <faction>")
class CmdAdminDisbandTest extends CommandTestBase {

    @TempDir Path tempDir;

    @Mock private FactionService factionService;
    @Mock private FactionModel faction;

    private CmdAdminDisband cmd;
    private final String factionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        cmd = new CmdAdminDisband(factionService);
        when(faction.getId()).thenReturn(factionId);
        when(faction.getName()).thenReturn("Alpha");
    }

    @AfterEach
    void tearDown() {
        PredefinedConfigManager.setInstance(null);
    }

    @StorageTest
    @DisplayName("faction not found — error message")
    void testFactionNotFound() {
        when(factionService.getFactionByName("Unknown")).thenReturn(Optional.empty());

        cmd.execute(ctx("Unknown"));

        verify(player).sendMessage(argThat(componentContains("not found")));
        verify(factionService, never()).disbandFaction(factionId);
    }

    @StorageTest
    @DisplayName("predefined faction — disband blocked")
    void testPredefinedBlocked() throws Exception {
        final YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("enabled", true);
        cfg.set("case-sensitive", false);
        cfg.set("block-disband", true);
        cfg.set("factions.alpha.name", "Alpha");
        cfg.set("factions.alpha.created", false);
        cfg.save(tempDir.resolve("pre-defined.yml").toFile());
        final PredefinedConfigManager mgr =
            new PredefinedConfigManager(tempDir.toFile(), Logger.getLogger("test"));
        mgr.reload();
        PredefinedConfigManager.setInstance(mgr);

        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.of(faction));

        cmd.execute(ctx("Alpha"));

        verify(player).sendMessage(argThat(componentContains("cannot be disbanded")));
        verify(factionService, never()).disbandFaction(factionId);
    }

    @StorageTest
    @DisplayName("success — faction disbanded")
    void testDisbandSuccess() {
        when(factionService.getFactionByName("Alpha")).thenReturn(Optional.of(faction));
        when(factionService.disbandFaction(factionId)).thenReturn(true);

        cmd.execute(ctx("Alpha"));

        verify(player).sendMessage(argThat(componentContains("Disbanded")));
    }
}

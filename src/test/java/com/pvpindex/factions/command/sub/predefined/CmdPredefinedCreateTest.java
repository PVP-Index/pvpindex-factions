package com.pvpindex.factions.command.sub.predefined;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
@DisplayName("CmdPredefinedCreate — /f predefined create <faction>")
class CmdPredefinedCreateTest extends CommandTestBase {

    @TempDir Path tempDir;

    @Mock private FactionService factionService;
    @Mock private FactionModel faction;

    private CmdPredefinedCreate cmd;
    private final UUID uuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cmd = new CmdPredefinedCreate(factionService);
        when(player.getUniqueId()).thenReturn(uuid);
    }

    @AfterEach
    void tearDown() {
        PredefinedConfigManager.setInstance(null);
    }

    @StorageTest
    @DisplayName("predefined disabled — disabled message")
    void testDisabled() {
        cmd.execute(ctx("Spawn"));

        verify(player).sendMessage(argThat(componentContains("disabled")));
        verify(factionService, never()).createFaction(any(), any());
    }

    @StorageTest
    @DisplayName("player already in faction — rejected")
    void testAlreadyInFaction() throws Exception {
        final YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("enabled", true);
        cfg.save(tempDir.resolve("pre-defined.yml").toFile());
        final PredefinedConfigManager mgr =
            new PredefinedConfigManager(tempDir.toFile(), Logger.getLogger("test"));
        mgr.reload();
        PredefinedConfigManager.setInstance(mgr);
        when(factionService.isInFaction(uuid)).thenReturn(true);

        cmd.execute(ctx("Spawn"));

        verify(player).sendMessage(argThat(componentContains("already")));
        verify(factionService, never()).createFaction(any(), any());
    }

    @StorageTest
    @DisplayName("unknown predefined faction name — rejected")
    void testUnknownFaction() throws Exception {
        final YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("enabled", true);
        cfg.save(tempDir.resolve("pre-defined.yml").toFile());
        final PredefinedConfigManager mgr =
            new PredefinedConfigManager(tempDir.toFile(), Logger.getLogger("test"));
        mgr.reload();
        PredefinedConfigManager.setInstance(mgr);
        when(factionService.isInFaction(uuid)).thenReturn(false);

        cmd.execute(ctx("Spawn"));

        verify(player).sendMessage(argThat(componentContains("Unknown")));
    }

    @StorageTest
    @DisplayName("success — predefined faction created")
    void testCreateSuccess() throws Exception {
        final YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("enabled", true);
        cfg.set("factions.spawn.name", "Spawn");
        cfg.set("factions.spawn.created", false);
        cfg.save(tempDir.resolve("pre-defined.yml").toFile());
        final PredefinedConfigManager mgr =
            new PredefinedConfigManager(tempDir.toFile(), Logger.getLogger("test"));
        mgr.reload();
        PredefinedConfigManager.setInstance(mgr);
        when(factionService.isInFaction(uuid)).thenReturn(false);
        when(factionService.createFaction(eq("Spawn"), eq(uuid))).thenReturn(Optional.of(faction));

        cmd.execute(ctx("Spawn"));

        verify(player).sendMessage(argThat(componentContains("created")));
    }
}

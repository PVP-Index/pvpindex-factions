package com.pvpindex.factions.command.sub.predefined;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdPredefinedList — /f predefined list")
class CmdPredefinedListTest extends CommandTestBase {

    @TempDir Path tempDir;

    private CmdPredefinedList cmd;

    @BeforeEach
    void setUp() {
        cmd = new CmdPredefinedList();
    }

    @AfterEach
    void tearDown() {
        PredefinedConfigManager.setInstance(null);
    }

    @StorageTest
    @DisplayName("predefined disabled — disabled message")
    void testDisabled() {
        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("disabled")));
    }

    @StorageTest
    @DisplayName("no presets configured — empty message")
    void testNoPresets() throws Exception {
        final YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("enabled", true);
        cfg.save(tempDir.resolve("pre-defined.yml").toFile());
        final PredefinedConfigManager mgr =
            new PredefinedConfigManager(tempDir.toFile(), Logger.getLogger("test"));
        mgr.reload();
        PredefinedConfigManager.setInstance(mgr);

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("No predefined")));
    }

    @StorageTest
    @DisplayName("presets configured — names shown")
    void testPresetsListed() throws Exception {
        final YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("enabled", true);
        cfg.set("factions.spawn.name", "Spawn");
        cfg.set("factions.spawn.created", false);
        cfg.set("factions.pvp.name", "PvP");
        cfg.set("factions.pvp.created", false);
        cfg.save(tempDir.resolve("pre-defined.yml").toFile());
        final PredefinedConfigManager mgr =
            new PredefinedConfigManager(tempDir.toFile(), Logger.getLogger("test"));
        mgr.reload();
        PredefinedConfigManager.setInstance(mgr);

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("Predefined factions")));
    }
}

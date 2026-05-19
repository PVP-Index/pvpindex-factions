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
@DisplayName("CmdPredefinedReload — /f predefined reload")
class CmdPredefinedReloadTest extends CommandTestBase {

    @TempDir Path tempDir;

    private CmdPredefinedReload cmd;

    @BeforeEach
    void setUp() {
        cmd = new CmdPredefinedReload();
    }

    @AfterEach
    void tearDown() {
        PredefinedConfigManager.setInstance(null);
    }

    @StorageTest
    @DisplayName("manager not set — not available message")
    void testManagerNotAvailable() {
        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("not available")));
    }

    @StorageTest
    @DisplayName("manager present — success message sent")
    void testReloadSuccess() throws Exception {
        final YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("enabled", false);
        cfg.save(tempDir.resolve("pre-defined.yml").toFile());
        final PredefinedConfigManager mgr =
            new PredefinedConfigManager(tempDir.toFile(), Logger.getLogger("test"));
        mgr.reload();
        PredefinedConfigManager.setInstance(mgr);

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("reloaded")));
    }
}

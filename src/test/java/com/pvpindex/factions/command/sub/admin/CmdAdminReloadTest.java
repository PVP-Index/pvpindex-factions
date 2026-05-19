package com.pvpindex.factions.command.sub.admin;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import java.nio.file.Files;
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
@DisplayName("CmdAdminReload — /fa reload")
class CmdAdminReloadTest extends CommandTestBase {

    @TempDir
    Path tempDir;

    private CmdAdminReload cmd;

    @BeforeEach
    void setUp() throws Exception {
        cmd = new CmdAdminReload();
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        Files.writeString(tempDir.resolve("messages.yml"), "# test\n");
    }

    @AfterEach
    void tearDown() {
        PredefinedConfigManager.setInstance(null);
    }

    @StorageTest
    @DisplayName("reload without predefined manager — success message sent")
    void testReloadNoPredefined() {
        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("reloaded")));
    }

    @StorageTest
    @DisplayName("reload with predefined manager — success message sent")
    void testReloadWithPredefined() throws Exception {
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

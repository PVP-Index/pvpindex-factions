package com.pvpindex.factions.command.sub.predefined;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdPredefinedClaim — /f predefined claim <faction>")
class CmdPredefinedClaimTest extends CommandTestBase {

    @TempDir Path tempDir;

    @Mock private World world;
    @Mock private Chunk chunk;

    private CmdPredefinedClaim cmd;

    @BeforeEach
    void setUp() {
        cmd = new CmdPredefinedClaim();
        final Location loc = Mockito.mock(Location.class);
        when(loc.getChunk()).thenReturn(chunk);
        when(player.getLocation()).thenReturn(loc);
        when(chunk.getWorld()).thenReturn(world);
        when(chunk.getX()).thenReturn(10);
        when(chunk.getZ()).thenReturn(20);
        when(world.getName()).thenReturn("world");
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
    }

    @StorageTest
    @DisplayName("unknown predefined faction — error message")
    void testUnknownFaction() throws Exception {
        final YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("enabled", true);
        cfg.save(tempDir.resolve("pre-defined.yml").toFile());
        final PredefinedConfigManager mgr =
            new PredefinedConfigManager(tempDir.toFile(), Logger.getLogger("test"));
        mgr.reload();
        PredefinedConfigManager.setInstance(mgr);

        cmd.execute(ctx("Spawn"));

        verify(player).sendMessage(argThat(componentContains("Unknown")));
    }

    @StorageTest
    @DisplayName("success — claim saved for predefined faction")
    void testClaimSaved() throws Exception {
        final YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("enabled", true);
        cfg.set("factions.spawn.name", "Spawn");
        cfg.set("factions.spawn.created", false);
        cfg.save(tempDir.resolve("pre-defined.yml").toFile());
        final PredefinedConfigManager mgr =
            new PredefinedConfigManager(tempDir.toFile(), Logger.getLogger("test"));
        mgr.reload();
        PredefinedConfigManager.setInstance(mgr);

        cmd.execute(ctx("Spawn"));

        verify(player).sendMessage(argThat(componentContains("Saved")));
    }
}

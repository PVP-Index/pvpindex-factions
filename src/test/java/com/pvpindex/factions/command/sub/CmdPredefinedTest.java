package com.pvpindex.factions.command.sub;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.predefined.PredefinedConfigManager;
import com.pvpindex.factions.service.FactionService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CmdPredefined — /f predefined")
class CmdPredefinedTest extends CommandTestBase {

    @Mock private FactionService factionService;

    private CmdPredefined cmd;

    @BeforeEach
    void setUp() throws IOException {
        cmd = new CmdPredefined(factionService);
        final Path dir = Files.createTempDirectory("predefined-test");
        final PredefinedConfigManager manager = new PredefinedConfigManager(dir.toFile(), logger);
        manager.initialize();
        PredefinedConfigManager.setInstance(manager);
    }

    @AfterEach
    void tearDown() {
        PredefinedConfigManager.setInstance(null);
    }

    @Test
    @DisplayName("disabled mode blocks usage")
    void disabledModeBlocksUsage() {
        cmd.execute(ctx("list"));
        verify(player).sendMessage(argThat(componentContains("disabled")));
    }

    @Test
    @DisplayName("tab complete exposes subcommands")
    void tabCompleteSubcommands() {
        final List<String> completions = cmd.tabComplete(ctx(""));
        assertTrue(completions.contains("create"));
        assertTrue(completions.contains("claim"));
        assertTrue(completions.contains("sethome"));
        assertTrue(completions.contains("reload"));
        assertTrue(completions.contains("list"));
    }
}

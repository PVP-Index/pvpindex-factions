package com.pvpindex.factions.command.sub.admin;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.registry.CommandRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdAdminHelp — /fa help")
class CmdAdminHelpTest extends CommandTestBase {

    @Mock private CommandRegistry commandRegistry;

    private CmdAdminHelp cmd;

    @BeforeEach
    void setUp() {
        cmd = new CmdAdminHelp(commandRegistry);
    }

    @StorageTest
    @DisplayName("no commands in registry — header only")
    void testEmptyRegistry() {
        when(commandRegistry.getAll()).thenReturn(List.of());

        cmd.execute(ctx());

        verify(player).sendMessage(argThat(componentContains("Factions Admin")));
    }

    @StorageTest
    @DisplayName("command in registry — entry shown")
    void testCommandListed() {
        final FactionCommand subCmd = Mockito.mock(FactionCommand.class);
        when(subCmd.getName()).thenReturn("disband");
        when(subCmd.getDescription()).thenReturn("Disband any faction.");
        when(subCmd.getPermission()).thenReturn(null); // no permission guard
        when(commandRegistry.getAll()).thenReturn(List.of(subCmd));

        cmd.execute(ctx());

        verify(player).sendMessage(argThat((String s) -> s.contains("disband")));
    }
}

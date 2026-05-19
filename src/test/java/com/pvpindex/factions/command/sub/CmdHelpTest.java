package com.pvpindex.factions.command.sub;


import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;


import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandTestBase;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.registry.CommandRegistry;
import java.util.Optional;
import org.bukkit.command.ConsoleCommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import com.pvpindex.factions.command.StorageTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CmdHelp — /f help")
class CmdHelpTest extends CommandTestBase {


    @Mock
    private CommandRegistry commandRegistry;


    private CmdHelp cmd;


    @BeforeEach
    void setUp() {
        cmd = new CmdHelp(commandRegistry);
        lenient().when(commandRegistry.get(anyString())).thenReturn(Optional.empty());
    }


    // -------------------------------------------------------------------------
    // Helper: create a lightweight FactionCommand stub
    // -------------------------------------------------------------------------


    private FactionCommand stubCmd(final String name, final String perm,
                                   final String usage, final String description) {
        return new FactionCommand(name) {
            {
                if (perm != null) setPermission(perm);
                setDescription(description);
            }
            @Override protected void perform(final CommandContext ctx) { }
            @Override public String getUsage() { return usage; }
        };
    }


    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------


    @StorageTest
    @DisplayName("always shows the header")
    void testHeaderAlwaysShown() {
        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("Factions Help")));
        verify(player).sendMessage(argThat(componentContains("Start here")));
    }


    @StorageTest
    @DisplayName("lists an accessible command")
    void testListsAccessibleCommand() {
        final FactionCommand infoCmd = stubCmd("info", null, "/f info [name]", "Show faction info.");
        when(commandRegistry.get(eq("info"))).thenReturn(Optional.of(infoCmd));


        cmd.execute(ctx());


        verify(player).sendMessage(argThat(componentContains("/f info [name]")));
    }


    @StorageTest
    @DisplayName("hides a command the sender lacks permission for")
    void testHidesPermissionDeniedCommand() {
        final FactionCommand renameCmd =
            stubCmd("rename", "pvpindex.faction.rename", "/f rename <name>", "Rename faction.");
        when(commandRegistry.get(eq("rename"))).thenReturn(Optional.of(renameCmd));
        when(player.hasPermission("pvpindex.faction.rename")).thenReturn(false);


        cmd.execute(ctx());


        verify(player, never()).sendMessage(argThat(componentContains("/f rename <name>")));
    }


    @StorageTest
    @DisplayName("shows command when sender has required permission")
    void testShowsPermittedCommand() {
        final FactionCommand claimCmd =
            stubCmd("claim", "pvpindex.faction.claim", "/f claim", "Claim a chunk.");
        when(commandRegistry.get(eq("claim"))).thenReturn(Optional.of(claimCmd));
        // default mock setup grants all permissions


        cmd.execute(ctx());


        verify(player, atLeastOnce()).sendMessage(argThat(componentContains("Claim a chunk.")));
    }


    @StorageTest
    @DisplayName("works for console sender")
    void testConsoleSender() {
        final ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        lenient().when(console.hasPermission(anyString())).thenReturn(true);


        cmd.execute(ctx(console));


        verify(console).sendMessage(argThat(componentContains("Factions Help")));
    }
}

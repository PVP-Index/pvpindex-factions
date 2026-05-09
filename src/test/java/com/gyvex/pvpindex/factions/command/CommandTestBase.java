package com.gyvex.pvpindex.factions.command;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import com.gyvex.pvpindex.factions.config.FactionsConfig;
import com.gyvex.pvpindex.factions.data.Repositories;
import java.util.List;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

/**
 * Abstract base that declares the common Mockito mocks shared by all command
 * feature tests.
 *
 * <p>Concrete test classes annotated with
 * {@code @ExtendWith(MockitoExtension.class)} inherit these fields
 * automatically.  Each test class can declare additional mocks for the specific
 * services/engines its command under test requires.
 */
public abstract class CommandTestBase {

    @Mock
    protected Plugin plugin;

    @Mock
    protected Player player;

    @Mock
    protected FactionsConfig config;

    @Mock
    protected Repositories repos;

    @Mock
    protected Logger logger;

    /**
     * Grant the mock player every permission by default so tests that exercise
     * command logic do not fail at the permission guard.  Tests that verify
     * permission-denial explicitly override this stub.
     */
    @BeforeEach
    void setUpBasePermissions() {
        lenient().when(player.hasPermission(anyString())).thenReturn(true);
    }

    // -------------------------------------------------------------------------
    // Context factory helpers
    // -------------------------------------------------------------------------

    /**
     * Build a {@link CommandContext} using the mock {@link Player} as sender.
     *
     * @param args command arguments (relative to the sub-command)
     * @return ready-to-use context
     */
    protected CommandContext ctx(final String... args) {
        return new CommandContext(plugin, player, List.of(args), repos, config, logger);
    }

    /**
     * Build a {@link CommandContext} using an arbitrary {@link CommandSender}
     * (e.g. a mocked console sender).
     *
     * @param sender arbitrary sender
     * @param args   command arguments
     * @return ready-to-use context
     */
    protected CommandContext ctx(final CommandSender sender, final String... args) {
        return new CommandContext(plugin, sender, List.of(args), repos, config, logger);
    }

    /**
     * Mockito {@link ArgumentMatcher} that checks whether the plain-text
     * serialization of an Adventure {@link Component} contains the given
     * substring.  Use with
     * {@code verify(sender).sendMessage(argThat(componentContains("text")))}.
     *
     * @param text substring to look for
     * @return matcher
     */
    protected static ArgumentMatcher<Component> componentContains(final String text) {
        return comp -> PlainTextComponentSerializer.plainText().serialize(comp).contains(text);
    }
}

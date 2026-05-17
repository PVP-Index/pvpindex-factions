package com.pvpindex.factions.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests for the {@code LegacyOps} inner class in {@link MsgUtil}.
 *
 * <p>{@code LegacyOps} is the Spigot code path: it converts MiniMessage
 * strings using shaded Adventure and delivers them to players via the
 * BungeeCord chat component API so that hover/click events are preserved.
 * Because {@link MsgUtil#ADVENTURE} is {@code true} on the Paper test
 * classpath, these tests invoke {@code LegacyOps} directly via reflection.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MsgUtil.LegacyOps – Spigot chat path")
class MsgUtilLegacyOpsTest {

    @Mock
    private Player player;
    @Mock
    private Player.Spigot spigot;
    @Mock
    private CommandSender console;

    private Method sendMethod;
    private Method toLegacyMethod;

    @BeforeEach
    void setUp() throws Exception {
        when(player.spigot()).thenReturn(spigot);

        Class<?> legacyOpsClass = null;
        for (final Class<?> inner : MsgUtil.class.getDeclaredClasses()) {
            if ("LegacyOps".equals(inner.getSimpleName())) {
                legacyOpsClass = inner;
                break;
            }
        }
        assertNotNull(legacyOpsClass, "LegacyOps inner class not found in MsgUtil");
        sendMethod = legacyOpsClass.getDeclaredMethod("send", CommandSender.class, String.class);
        sendMethod.setAccessible(true);
        toLegacyMethod = legacyOpsClass.getDeclaredMethod("toLegacy", String.class);
        toLegacyMethod.setAccessible(true);
    }

    // -------------------------------------------------------------------------
    // send() – Player routing
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("send() to Player uses player.spigot(), not sendMessage(String)")
    void sendPlayerUsesSpigotApi() throws ReflectiveOperationException {
        sendMethod.invoke(null, player, "<green>Hello");

        verify(player).spigot();
        verify(player, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("send() to Player preserves hover event in BungeeCord components")
    void sendPlayerPreservesHoverEvent() throws ReflectiveOperationException {
        sendMethod.invoke(null, player, "<hover:show_text:'World'>Hello</hover>");

        final Collection<Invocation> invocations = Mockito.mockingDetails(spigot).getInvocations();
        assertFalse(invocations.isEmpty(), "player.spigot().sendMessage() was never called");

        final Object[] rawArgs = invocations.iterator().next().getRawArguments();
        final BaseComponent[] components;
        if (rawArgs.length == 1 && rawArgs[0] instanceof BaseComponent[]) {
            components = (BaseComponent[]) rawArgs[0];
        } else {
            components = Arrays.stream(rawArgs)
                .filter(a -> a instanceof BaseComponent)
                .toArray(BaseComponent[]::new);
        }

        assertTrue(
            Arrays.stream(components).anyMatch(c -> c.getHoverEvent() != null),
            "at least one BungeeCord BaseComponent should carry a hover event");
    }

    @Test
    @DisplayName("send() to Player preserves click event in BungeeCord components")
    void sendPlayerPreservesClickEvent() throws ReflectiveOperationException {
        sendMethod.invoke(null, player, "<click:run_command:'/f info'>Click me</click>");

        final Collection<Invocation> invocations = Mockito.mockingDetails(spigot).getInvocations();
        assertFalse(invocations.isEmpty(), "player.spigot().sendMessage() was never called");

        final Object[] rawArgs = invocations.iterator().next().getRawArguments();
        final BaseComponent[] components;
        if (rawArgs.length == 1 && rawArgs[0] instanceof BaseComponent[]) {
            components = (BaseComponent[]) rawArgs[0];
        } else {
            components = Arrays.stream(rawArgs)
                .filter(a -> a instanceof BaseComponent)
                .toArray(BaseComponent[]::new);
        }

        assertTrue(
            Arrays.stream(components).anyMatch(c -> c.getClickEvent() != null),
            "at least one BungeeCord BaseComponent should carry a click event");
    }

    @Test
    @DisplayName("send() to Player delivers text content via spigot path")
    void sendPlayerDeliversParsedText() throws ReflectiveOperationException {
        sendMethod.invoke(null, player, "<green>Faction joined");

        final Collection<Invocation> invocations = Mockito.mockingDetails(spigot).getInvocations();
        assertFalse(invocations.isEmpty(), "player.spigot().sendMessage() was never called");

        final Object[] rawArgs = invocations.iterator().next().getRawArguments();
        final BaseComponent[] components;
        if (rawArgs.length == 1 && rawArgs[0] instanceof BaseComponent[]) {
            components = (BaseComponent[]) rawArgs[0];
        } else {
            components = Arrays.stream(rawArgs)
                .filter(a -> a instanceof BaseComponent)
                .toArray(BaseComponent[]::new);
        }

        final String legacyText = net.md_5.bungee.api.chat.TextComponent.toLegacyText(components);
        assertTrue(legacyText.contains("Faction joined"),
            "serialized text should contain the message body");
    }

    // -------------------------------------------------------------------------
    // send() – non-Player fallback
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("send() to non-Player delivers §-coded string via sendMessage(String)")
    void sendConsoleFallsBackToLegacyString() throws ReflectiveOperationException {
        sendMethod.invoke(null, console, "<green>Hello Console");

        verify(console).sendMessage(argThat((String s) ->
            s.contains("\u00a7a") && s.contains("Hello Console")));
    }

    @Test
    @DisplayName("send() to non-Player does not attempt player.spigot()")
    void sendConsoleDoesNotUseSpigotApi() throws ReflectiveOperationException {
        sendMethod.invoke(null, console, "plain message");

        assertTrue(
            Mockito.mockingDetails(spigot).getInvocations().isEmpty(),
            "spigot.sendMessage() should not be called for a non-Player sender");
    }

    // -------------------------------------------------------------------------
    // toLegacy()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("toLegacy() converts <green> to §a colour code")
    void toLegacyProducesGreenSectionCode() throws ReflectiveOperationException {
        final String result = (String) toLegacyMethod.invoke(null, "<green>Hello World");

        assertTrue(result.contains("\u00a7a"), "expected §a (green) in output");
        assertTrue(result.contains("Hello World"), "expected plain text portion");
    }

    @Test
    @DisplayName("toLegacy() converts <red> to §c colour code")
    void toLegacyProducesRedSectionCode() throws ReflectiveOperationException {
        final String result = (String) toLegacyMethod.invoke(null, "<red>Error");

        assertTrue(result.contains("\u00a7c"), "expected §c (red) in output");
    }

    @Test
    @DisplayName("toLegacy() converts <bold> to §l formatting code")
    void toLegacyProducesBoldSectionCode() throws ReflectiveOperationException {
        final String result = (String) toLegacyMethod.invoke(null, "<bold>Title");

        assertTrue(result.contains("\u00a7l"), "expected §l (bold) in output");
        assertTrue(result.contains("Title"), "expected plain text portion");
    }

    @Test
    @DisplayName("toLegacy() passes through plain text unchanged")
    void toLegacyPassesThroughPlainText() throws ReflectiveOperationException {
        final String result = (String) toLegacyMethod.invoke(null, "Plain Text Only");

        assertTrue(result.contains("Plain Text Only"));
    }

    @Test
    @DisplayName("toLegacy() strips hover tags (hover events have no §-code equivalent)")
    void toLegacyStripsHoverTags() throws ReflectiveOperationException {
        final String result = (String) toLegacyMethod.invoke(null,
            "<hover:show_text:'tooltip'>Visible</hover>");

        assertTrue(result.contains("Visible"), "visible text should be present");
        assertFalse(result.contains("tooltip"), "tooltip text should not appear in legacy output");
        assertFalse(result.contains("hover"), "MiniMessage tag should not appear in output");
    }

    // -------------------------------------------------------------------------
    // Reflection helper – wraps InvocationTargetException for cleaner errors
    // -------------------------------------------------------------------------

    private void sendMethod(final CommandSender sender, final String msg)
        throws ReflectiveOperationException {
        try {
            sendMethod.invoke(null, sender, msg);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw ex;
        }
    }
}

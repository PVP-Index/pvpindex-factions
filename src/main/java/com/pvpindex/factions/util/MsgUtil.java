package com.pvpindex.factions.util;

import com.pvpindex.factions.config.MessagesConfig;
import org.bukkit.command.CommandSender;

/**
 * Utility methods for sending MiniMessage-formatted messages and building
 * rich text strings with hover / click events.
 *
 * <p>On Paper the private {@code AdventureOps} inner class deserialises MiniMessage
 * strings to native Adventure {@code Component} objects using the server's own
 * {@code MiniMessage} via cached {@link java.lang.invoke.MethodHandle}s, so that
 * hover/click formatting is fully preserved.  On Spigot, {@code LegacyOps} converts
 * MiniMessage to §-coded strings and sends them via
 * {@code CommandSender#sendMessage(String)}.
 */
public final class MsgUtil {

    private static volatile MessagesConfig messagesConfig;

    /**
     * True when Adventure-API and adventure-text-minimessage are both resolvable
     * from the plugin class loader at startup time.
     */
    public static final boolean ADVENTURE;

    static {
        boolean available = false;
        try {
            final Class<?> component = Class.forName("net.kyori.adventure.text.Component");
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            // Verify that CommandSender actually exposes the adventure overload.
            // Spigot ships adventure internally but does NOT add sendMessage(Component)
            // to its CommandSender API; only Paper does. Without this check the plugin
            // would throw NoSuchMethodError on every command on Spigot.
            org.bukkit.command.CommandSender.class.getMethod("sendMessage", component);
            available = true;
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
            // Running on a platform without full adventure support (e.g. Spigot).
        }
        ADVENTURE = available;
    }

    private MsgUtil() { }

    /**
     * Escape a value for safe embedding inside a MiniMessage single-quoted tag argument.
     * Escapes {@code \} first, then {@code '}.
     */
    private static String mmEscape(final String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    public static void setMessagesConfig(final MessagesConfig config) {
        messagesConfig = config;
    }

    // -------------------------------------------------------------------------
    // Basic send
    // -------------------------------------------------------------------------

    /**
     * Send a MiniMessage-formatted message to a {@link CommandSender}.
     *
     * <p>On Paper the message is deserialized via the server's native Adventure
     * classes and sent as a rich component (hover / click events preserved).
     * On Spigot the shaded Adventure copy converts the message to §-colour codes
     * so styling is preserved even without native Adventure support.
     *
     * @param sender  recipient
     * @param message MiniMessage string
     */
    public static void send(final CommandSender sender, final String message) {
        if (ADVENTURE) {
            AdventureOps.send(sender, message);
        } else {
            LegacyOps.send(sender, message);
        }
    }

    public static void sendKey(
            final CommandSender sender,
            final String key,
            final String fallback,
            final String... kvPairs) {
        final String template = message(key, fallback);
        send(sender, replace(template, kvPairs));
    }

    public static String message(final String key, final String fallback) {
        final MessagesConfig cfg = messagesConfig;
        return cfg == null ? fallback : cfg.get(key, fallback);
    }

    /**
     * Send a prefixed MiniMessage-formatted message.
     *
     * @param sender  recipient
     * @param prefix  MiniMessage prefix string
     * @param message MiniMessage message string
     */
    public static void sendPrefixed(
            final CommandSender sender, final String prefix, final String message) {
        send(sender, prefix + message);
    }

    /**
     * Apply simple token replacement on a message template.
     *
     * <p>Tokens are in the form {@code {key}}. Example:
     * <pre>MsgUtil.replace("{player} joined!", "player", "Steve")</pre>
     *
     * @param template MiniMessage string with {@code {key}} placeholders
     * @param kvPairs  alternating key / value pairs
     * @return replaced string (not yet parsed)
     */
    public static String replace(final String template, final String... kvPairs) {
        String result = template;
        for (int i = 0; i + 1 < kvPairs.length; i += 2) {
            result = result.replace("{" + kvPairs[i] + "}", kvPairs[i + 1]);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // String utilities
    // -------------------------------------------------------------------------

    public static String stripTags(final String mm) {
        // First pass: remove tags whose arguments are wrapped in single quotes
        // (e.g. <hover:show_text:'text with >chars'>, <click:run_command:'/cmd'>).
        // The simple [^>]* regex stops at the first > inside the quoted value and
        // leaves a stray '> in the output. Match the quoted argument explicitly.
        String result = mm.replaceAll("<[a-zA-Z_][^'<>]*'[^']*'[^>]*>", "");
        // Second pass: remove any remaining simple and closing tags
        // (<gold>, </hover>, <newline>, <#rrggbb>, etc.). By this point no tag
        // content contains > so the simpler [^>]* pattern is safe.
        result = result.replaceAll("<[^>]*>", "");
        return result;
    }

    /**
     * Convert a MiniMessage string to a §-colour-code string.
     *
     * <p>Uses the shaded copy of Adventure's {@code LegacyComponentSerializer} so
     * the result is safe to pass to Bukkit inventory titles and item display names
     * on both Paper and Spigot.
     *
     * @param mm MiniMessage string
     * @return legacy §-formatted string
     */
    public static String toLegacy(final String mm) {
        return LegacyOps.toLegacy(mm);
    }

    // -------------------------------------------------------------------------
    // Rich string builders (hover / click) – return MiniMessage strings
    // -------------------------------------------------------------------------

    /**
     * Build a {@code /f help} list entry with hover and click-to-suggest behaviour.
     *
     * @param usage       full usage string, e.g. {@code "/f create <name>"}
     * @param description short description shown on hover
     * @return MiniMessage string ready to pass to {@link #send}
     */
    public static String helpEntry(final String usage, final String description) {
        final String line = replace(
            message("help.entry-line", "<yellow>{usage}<gray> - {description}"),
            "usage", usage,
            "description", description);
        final String hover = replace(
            message("help.entry-hover", "<gray>{description}<newline><dark_gray>Click to suggest command"),
            "description", description);
        return "<hover:show_text:'" + mmEscape(hover) + "'>"
            + "<click:suggest_command:'" + mmEscape(usage) + "'>"
            + line
            + "</click></hover>";
    }

    /**
     * Build the faction-info header string.
     *
     * @param factionName display name of the faction
     * @return MiniMessage string ready to pass to {@link #send}
     */
    public static String infoHeader(final String factionName) {
        return "<gold>== "
            + "<hover:show_text:'<gray>Click to refresh info'>"
            + "<click:suggest_command:'/f info " + mmEscape(factionName) + "'>"
            + "<yellow><bold>" + factionName
            + "</click></hover>"
            + "<gold> ==";
    }

    /**
     * Build a single warp-list entry.
     *
     * @param warpName name of the warp
     * @return MiniMessage string ready to pass to {@link #send}
     */
    public static String warpEntry(final String warpName) {
        return "<yellow>  \u00BB "
            + "<hover:show_text:'<green>Click to teleport to <white>" + mmEscape(warpName) + "'>"
            + "<click:run_command:'/f warp " + mmEscape(warpName) + "'>"
            + "<white>" + warpName
            + "</click></hover>";
    }

    /**
     * Build the invite notification sent to the invited player.
     *
     * @param factionName name of the inviting faction
     * @return MiniMessage string ready to pass to {@link #send}
     */
    public static String inviteNotification(final String factionName) {
        final String msg = replace(
            message("invite.received-short", "<yellow>{faction}<gray> invited you. "),
            "faction", factionName);
        final String acceptHover = replace(
            message("invite.accept-hover", "<green>Click to join <white>{faction}"),
            "faction", factionName);
        final String denyHover = message("invite.decline-hover", "<red>Simply ignore to decline");
        return msg
            + "<hover:show_text:'" + mmEscape(acceptHover) + "'>"
            + "<click:run_command:'/f join " + mmEscape(factionName) + "'>"
            + "<green>[Accept]"
            + "</click></hover>"
            + "<gray> | "
            + "<hover:show_text:'" + mmEscape(denyHover) + "'>"
            + "<red>[Deny]"
            + "</hover>";
    }

    /**
     * Build a clickable pending-invite list entry for a faction/inviter pair.
     *
     * @param factionName faction name to join
     * @param inviterName inviter display name
     * @return MiniMessage string ready to pass to {@link #send}
     */
    public static String inviteListEntry(final String factionName, final String inviterName) {
        final String line = replace(
            message(
                "invite.summary-line",
                "<yellow>- <white>{faction}</white> <gray>(invited by <white>{inviter}</white>)"),
            "faction", factionName,
            "inviter", inviterName);
        final String acceptHover = replace(
            message("invite.accept-hover", "<green>Click to join <white>{faction}"),
            "faction", factionName);
        return line + " "
            + "<hover:show_text:'" + mmEscape(acceptHover) + "'>"
            + "<click:run_command:'/f join " + mmEscape(factionName) + "'>"
            + "<green>[Accept]"
            + "</click></hover>";
    }

    /**
     * Build the "unknown sub-command" error with a clickable {@code [Help]} link.
     *
     * @param input the unrecognised token the player typed
     * @return MiniMessage string ready to pass to {@link #send}
     */
    public static String unknownCommand(final String input) {
        final String base = replace(
            message("general.unknown-subcommand-detailed", "<red>Unknown command '<yellow>{input}<red>'. "),
            "input",
            input);
        final String hover = message("general.help-hover", "<gray>Click to view all faction commands");
        return base
            + "<hover:show_text:'" + mmEscape(hover) + "'>"
            + "<click:run_command:'/f help'>"
            + "<yellow>[Help]"
            + "</click></hover>";
    }

    /**
     * Build a reusable faction hover string.
     *
     * @param visibleText text shown in chat
     * @param factionName faction display name
     * @param detailLines optional detail lines shown on hover
     * @return MiniMessage string ready to pass to {@link #send}
     */
    public static String factionInfoHover(
            final String visibleText,
            final String factionName,
            final String... detailLines) {
        final StringBuilder hover = new StringBuilder("<gold>")
            .append(factionName)
            .append("<newline><gray>Click to view faction info");
        for (final String line : detailLines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            hover.append("<newline>").append(line);
        }
        return "<hover:show_text:'" + mmEscape(hover.toString()) + "'>"
            + "<click:suggest_command:'/f info " + mmEscape(factionName) + "'>"
            + visibleText
            + "</click></hover>";
    }

    // -------------------------------------------------------------------------
    // Adventure inner classes — loaded lazily on first use
    // -------------------------------------------------------------------------

    /**
     * Invokes Paper's native Adventure API via cached MethodHandles.
     *
     * <p>All {@code Class.forName} calls use string literals that are NOT
     * rewritten by the Maven Shade relocator.  This ensures that Paper's
     * native {@code net.kyori.adventure.*} classes are resolved rather than
     * the shaded copies located at {@code com.pvpindex.lib.adventure.*}.
     */
    /**
     * Paper-only send path.  Deserialises MiniMessage strings to native Adventure
     * {@code Component} objects via a cached {@link java.lang.invoke.MethodHandle}
     * and dispatches {@code CommandSender#sendMessage(Component)}.
     *
     * <p>All Adventure class names are kept as string literals so that the
     * shade relocator does <em>not</em> rewrite them; the JVM resolves them
     * against the server's classloader at runtime.  The vararg overload
     * {@code MiniMessage#deserialize(String, TagResolver...)} is used because
     * the single-argument form was removed from the MiniMessage interface in
     * Adventure 4.20.0.
     */
    private static final class AdventureOps {

        private AdventureOps() { }

        private static final java.lang.invoke.MethodHandle SEND_MESSAGE;
        private static final java.lang.invoke.MethodHandle MM_DESERIALIZE;
        private static final Object MM_INSTANCE;
        private static final Object EMPTY_RESOLVERS;

        static {
            java.lang.invoke.MethodHandle send = null;
            java.lang.invoke.MethodHandle deser = null;
            Object mm = null;
            Object emptyRes = null;
            try {
                final java.lang.invoke.MethodHandles.Lookup lookup =
                    java.lang.invoke.MethodHandles.publicLookup();
                final Class<?> comp = Class.forName("net.kyori.adventure.text.Component");
                final Class<?> mmCls = Class.forName(
                    "net.kyori.adventure.text.minimessage.MiniMessage");
                final Class<?> tagResCls = Class.forName(
                    "net.kyori.adventure.text.minimessage.tag.resolver.TagResolver");
                mm = mmCls.getMethod("miniMessage").invoke(null);
                emptyRes = java.lang.reflect.Array.newInstance(tagResCls, 0);
                deser = lookup.unreflect(
                    mmCls.getMethod("deserialize", String.class, tagResCls.arrayType()))
                    .asFixedArity();
                send = lookup.unreflect(
                    CommandSender.class.getMethod("sendMessage", comp));
            } catch (Exception ignored) { /* ADVENTURE flag guards call sites */ }
            MM_INSTANCE = mm;
            MM_DESERIALIZE = deser;
            SEND_MESSAGE = send;
            EMPTY_RESOLVERS = emptyRes;
        }

        static void send(final CommandSender sender, final String miniMsg) {
            try {
                SEND_MESSAGE.invoke(sender,
                    MM_DESERIALIZE.invoke(MM_INSTANCE, miniMsg, EMPTY_RESOLVERS));
            } catch (Throwable t) {
                sender.sendMessage(MsgUtil.stripTags(miniMsg));
            }
        }
    }

    /**
     * Converts MiniMessage strings to §-colour-code strings using the shaded
     * (relocated) Adventure copy bundled inside the plugin JAR.
     *
     * <p>The imports here are rewritten by the shade relocator to
     * {@code com.pvpindex.lib.adventure.*}; they are fully independent of the
     * server's native {@code net.kyori.adventure.*} classes.  Used as the
     * fallback sending path on Spigot and for inventory / item-name conversions
     * on all platforms.
     */
    private static final class LegacyOps {

        private LegacyOps() { }

        private static final net.kyori.adventure.text.minimessage.MiniMessage MINI =
            net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
        private static final net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer LEGACY =
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection();

        static void send(final CommandSender sender, final String miniMsg) {
            sender.sendMessage(LEGACY.serialize(MINI.deserialize(miniMsg)));
        }

        static String toLegacy(final String miniMsg) {
            return LEGACY.serialize(MINI.deserialize(miniMsg));
        }
    }
}

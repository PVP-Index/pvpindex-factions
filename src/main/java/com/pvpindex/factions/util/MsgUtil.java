package com.pvpindex.factions.util;

import com.pvpindex.factions.config.MessagesConfig;
import org.bukkit.command.CommandSender;

/**
 * Utility methods for sending MiniMessage-formatted messages and building
 * rich text strings with hover / click events.
 *
 * <p>Adventure API classes are accessed lazily through the private {@code AdventureOps}
 * inner class so that this class can be loaded on Spigot servers where Adventure may not
 * be accessible from the plugin class loader at startup time.
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
     * <p>When Adventure is available the message is deserialized and sent as a rich
     * component; otherwise MiniMessage tags are stripped and the plain text is sent.
     *
     * @param sender  recipient
     * @param message MiniMessage string
     */
    public static void send(final CommandSender sender, final String message) {
        if (ADVENTURE) {
            AdventureOps.send(sender, message);
        } else {
            sender.sendMessage(stripTags(message));
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
        return mm.replaceAll("<[^>]*>", "");
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
    // Adventure inner class — only loaded when ADVENTURE == true
    // -------------------------------------------------------------------------

    /**
     * Holds all Adventure API references. This is a separate class file so the JVM
     * does not need to resolve Adventure types when {@link MsgUtil} is first loaded.
     */
    private static final class AdventureOps {

        private static final net.kyori.adventure.text.minimessage.MiniMessage MINI =
            net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

        private AdventureOps() { }

        static void send(final CommandSender sender, final String miniMsg) {
            sender.sendMessage(MINI.deserialize(miniMsg));
        }
    }
}

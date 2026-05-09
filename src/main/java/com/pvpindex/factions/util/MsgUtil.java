package com.pvpindex.factions.util;

import com.pvpindex.factions.config.MessagesConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

/**
 * Utility methods for sending MiniMessage-formatted messages and building
 * rich Adventure {@link Component} objects with hover / click events.
 */
public final class MsgUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static volatile MessagesConfig messagesConfig;

    /** Shared plugin prefix component. */
    public static final Component PREFIX =
        parse("<dark_gray>[<gold>Factions<dark_gray>] <reset>");

    private MsgUtil() { }

    public static void setMessagesConfig(final MessagesConfig config) {
        messagesConfig = config;
    }

    // -------------------------------------------------------------------------
    // Basic send / parse
    // -------------------------------------------------------------------------

    /**
     * Parse a MiniMessage string into an Adventure {@link Component}.
     *
     * @param text raw MiniMessage string
     * @return parsed component
     */
    public static Component parse(final String text) {
        return MINI.deserialize(text);
    }

    /**
     * Send a MiniMessage-formatted message to a {@link CommandSender}.
     *
     * @param sender  recipient
     * @param message MiniMessage string
     */
    public static void send(final CommandSender sender, final String message) {
        sender.sendMessage(MINI.deserialize(message));
    }

    /**
     * Send an Adventure {@link Component} directly to a {@link CommandSender}.
     *
     * @param sender    recipient
     * @param component pre-built component
     */
    public static void send(final CommandSender sender, final Component component) {
        sender.sendMessage(component);
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
        sender.sendMessage(MINI.deserialize(prefix + message));
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
    // Rich Component builders (hover / click)
    // -------------------------------------------------------------------------

    /**
     * Build a {@code /f help} list entry with:
     * <ul>
     *   <li>Click → suggest the command in the chat bar</li>
     *   <li>Hover → show description + hint text</li>
     * </ul>
     *
     * @param usage       full usage string, e.g. {@code "/f create <name>"}
     * @param description short description shown on hover
     * @return rich component ready to send
     */
    public static Component helpEntry(final String usage, final String description) {
        final String line = replace(
            message("help.entry-line", "<yellow>{usage}<gray> - {description}"),
            "usage", usage,
            "description", description);
        final String hover = replace(
            message("help.entry-hover", "<gray>{description}<newline><dark_gray>Click to suggest command"),
            "description", description);
        return parse(line)
            .hoverEvent(HoverEvent.showText(
                parse(hover)))
            .clickEvent(ClickEvent.suggestCommand(usage));
    }

    /**
     * Build the faction-info header component.
     *
     * <p>The faction name is clickable — clicking it suggests
     * {@code /f info <factionName>} to refresh the view.
     *
     * @param factionName display name of the faction
     * @return rich header component
     */
    public static Component infoHeader(final String factionName) {
        return Component.text()
            .append(parse("<gold>== "))
            .append(parse("<yellow><bold>" + factionName)
                .clickEvent(ClickEvent.suggestCommand("/f info " + factionName))
                .hoverEvent(HoverEvent.showText(parse("<gray>Click to refresh info"))))
            .append(parse("<gold> =="))
            .build();
    }

    /**
     * Build a single warp-list entry.
     *
     * <p>The warp name is clickable — clicking it runs
     * {@code /f warp <warpName>} immediately.
     *
     * @param warpName name of the warp
     * @return rich warp entry component
     */
    public static Component warpEntry(final String warpName) {
        return Component.text()
            .append(parse("<yellow>  \u00BB "))
            .append(parse("<white>" + warpName)
                .clickEvent(ClickEvent.runCommand("/f warp " + warpName))
                .hoverEvent(HoverEvent.showText(
                    parse("<green>Click to teleport to <white>" + warpName))))
            .build();
    }

    /**
     * Build the invite notification sent to the invited player.
     *
     * <p>Includes a clickable <green>[Accept]</green> button that runs
     * {@code /f join <factionName>}.
     *
     * @param factionName name of the inviting faction
     * @return rich invite notification component
     */
    public static Component inviteNotification(final String factionName) {
        final String msg = replace(
            message("invite.received-short", "<yellow>{faction}<gray> invited you. "),
            "faction", factionName);
        final String acceptHover = replace(
            message("invite.accept-hover", "<green>Click to join <white>{faction}"),
            "faction", factionName);
        final String denyHover = message("invite.decline-hover", "<red>Simply ignore to decline");
        return Component.text()
            .append(parse(msg))
            .append(parse("<green>[Accept]")
                .clickEvent(ClickEvent.runCommand("/f join " + factionName))
                .hoverEvent(HoverEvent.showText(parse(acceptHover))))
            .append(parse("<gray> | "))
            .append(parse("<red>[Deny]")
                .hoverEvent(HoverEvent.showText(parse(denyHover))))
            .build();
    }

    /**
     * Build a clickable pending-invite list entry for a faction/inviter pair.
     *
     * @param factionName faction name to join
     * @param inviterName inviter display name
     * @return rich component entry
     */
    public static Component inviteListEntry(final String factionName, final String inviterName) {
        final String line = replace(
            message(
                "invite.summary-line",
                "<yellow>- <white>{faction}</white> <gray>(invited by <white>{inviter}</white>)"),
            "faction", factionName,
            "inviter", inviterName);
        final String acceptHover = replace(
            message("invite.accept-hover", "<green>Click to join <white>{faction}"),
            "faction", factionName);
        return Component.text()
            .append(parse(line + " "))
            .append(parse("<green>[Accept]")
                .clickEvent(ClickEvent.runCommand("/f join " + factionName))
                .hoverEvent(HoverEvent.showText(parse(acceptHover))))
            .build();
    }

    /**
     * Build the "unknown sub-command" error shown by the root executor.
     *
     * <p>Includes a clickable <yellow>[Help]</yellow> link that runs
     * {@code /f help}.
     *
     * @param input the unrecognised token the player typed
     * @return rich error component
     */
    public static Component unknownCommand(final String input) {
        final String base = replace(
            message("general.unknown-subcommand-detailed", "<red>Unknown command '<yellow>{input}<red>'. "),
            "input",
            input);
        final String hover = message("general.help-hover", "<gray>Click to view all faction commands");
        return Component.text()
            .append(parse(base))
            .append(parse("<yellow>[Help]")
                .clickEvent(ClickEvent.runCommand("/f help"))
                .hoverEvent(HoverEvent.showText(parse(hover))))
            .build();
    }

    /**
     * Build a reusable faction hover component.
     *
     * <p>The visible text is supplied by the caller. Hover shows faction details
     * and click suggests {@code /f info <factionName>}.
     *
     * @param visibleText text shown in chat
     * @param factionName faction display name
     * @param detailLines optional detail lines shown on hover
     * @return rich component with hover + click info behavior
     */
    public static Component factionInfoHover(
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
        return parse(visibleText)
            .hoverEvent(HoverEvent.showText(parse(hover.toString())))
            .clickEvent(ClickEvent.suggestCommand("/f info " + factionName));
    }
}

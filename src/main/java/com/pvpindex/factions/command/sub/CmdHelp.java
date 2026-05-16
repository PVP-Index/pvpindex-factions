package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.registry.CommandRegistry;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;

/**
 * {@code /f help} — List all accessible faction commands.
 *
 * <p>Only commands the sender has permission to use are shown. The command
 * also registers the alias {@code ?} so players can type {@code /f ?}.
 *
 * <p>Each entry is a rich {@link net.kyori.adventure.text.Component} with:
 * <ul>
 *   <li>Hover — shows the command description</li>
 *   <li>Click — suggests the command in the chat bar</li>
 * </ul>
 */
public final class CmdHelp extends FactionCommand {

    private final CommandRegistry commandRegistry;

    public CmdHelp(final CommandRegistry commandRegistry) {
        super("help");
        setDescription("List all available faction commands.");
        setAliases("?");
        this.commandRegistry = commandRegistry;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        MsgUtil.sendKey(
            ctx.getSender(),
            "help.title",
            "<gradient:#f6d365:#fda085><bold>PvPIndex Factions Help</bold></gradient>");
        MsgUtil.sendKey(ctx.getSender(), "help.start-here", "<gray>Start here:");
        MsgUtil.sendKey(
            ctx.getSender(),
            "help.start-step-1",
            "<gray>1) <white>/f create <name></white> <dark_gray>- create your faction");
        MsgUtil.sendKey(
            ctx.getSender(),
            "help.start-step-2",
            "<gray>2) <white>/f invite <player></white> <dark_gray>- recruit your team");
        MsgUtil.sendKey(
            ctx.getSender(),
            "help.start-step-3",
            "<gray>3) <white>/f claim</white> <dark_gray>- claim your first chunk");
        MsgUtil.sendKey(
            ctx.getSender(),
            "help.start-step-4",
            "<gray>4) <white>/f sethome</white> <dark_gray>- set your base home");
        MsgUtil.sendKey(ctx.getSender(), "help.separator", "<dark_gray>----------------------------------------");

        sendSection(ctx, "Core", List.of("help", "info", "list", "map", "top", "gui"));
        sendSection(ctx, "Faction Setup", List.of("create", "rename", "desc", "disband"));
        sendSection(ctx, "Members & Invites", List.of("invite", "join", "leave", "kick", "promote", "demote", "leader"));
        sendSection(ctx, "Land & Navigation", List.of("claim", "unclaim", "home", "sethome", "unsethome", "warp", "fly"));
        sendSection(ctx, "Economy & Utility", List.of("bank", "notify", "relation"));

        if (ctx.getSender().hasPermission("factions.cmd.kick")) {
            MsgUtil.sendKey(ctx.getSender(), "help.officer-title", "<gold>Officer/Moderator Tips</gold>");
            MsgUtil.sendKey(
                ctx.getSender(),
                "help.officer-line-1",
                "<gray>- Manage invites: <white>/f invite list|revoke|declineall</white>");
            MsgUtil.sendKey(
                ctx.getSender(),
                "help.officer-line-2",
                "<gray>- Manage roles: <white>/f promote</white>, <white>/f demote</white>, <white>/f leader</white>");
            MsgUtil.sendKey(
                ctx.getSender(),
                "help.officer-line-3",
                "<gray>- Protect territory: <white>/f claim auto on</white> or <white>/f unclaim auto on</white>");
        }

        if (ctx.getSender().hasPermission("factions.admin")) {
            MsgUtil.sendKey(ctx.getSender(), "help.admin-title", "<red><bold>Admin Commands</bold></red>");
            MsgUtil.send(ctx.getSender(), MsgUtil.helpEntry("/fa help", "List admin commands."));
            MsgUtil.send(ctx.getSender(), MsgUtil.helpEntry("/fa bypass", "Toggle protection bypass."));
            MsgUtil.send(ctx.getSender(), MsgUtil.helpEntry("/fa claim", "Admin-claim current chunk."));
            MsgUtil.send(ctx.getSender(), MsgUtil.helpEntry("/fa unclaim", "Admin-unclaim current chunk."));
            MsgUtil.send(ctx.getSender(), MsgUtil.helpEntry("/fa disband <faction>", "Force-disband any faction."));
            MsgUtil.send(ctx.getSender(), MsgUtil.helpEntry("/fa reload", "Reload plugin configuration."));
        }
        MsgUtil.sendKey(
            ctx.getSender(),
            "help.tip-notify",
            "<dark_gray>Tip: <gray>Use <white>/f notify status</white> to manage your notifications.");
    }

    private void sendSection(final CommandContext ctx, final String title, final List<String> keys) {
        MsgUtil.sendKey(
            ctx.getSender(),
            "help.section." + title.toLowerCase().replace(" ", "-").replace("&", "and"),
            "<gold>" + title + "</gold>");
        for (final String key : keys) {
            final Optional<FactionCommand> opt = commandRegistry.get(key);
            if (opt.isEmpty()) {
                continue;
            }
            final FactionCommand cmd = opt.get();
            if (cmd.getPermission() != null && !ctx.getSender().hasPermission(cmd.getPermission())) {
                continue;
            }
            MsgUtil.send(ctx.getSender(), MsgUtil.helpEntry(cmd.getUsage(), cmd.getDescription()));
        }
    }
}

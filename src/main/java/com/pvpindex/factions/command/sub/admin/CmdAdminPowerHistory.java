package com.pvpindex.factions.command.sub.admin;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.command.sub.power.CmdPowerHistory;
import com.pvpindex.factions.service.PowerService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;

/** {@code /fa power history <player> [page]}. */
public final class CmdAdminPowerHistory extends FactionCommand {

    public CmdAdminPowerHistory(final PowerService powerService) {
        super("history");
        setPermission("factions.cmd.admin.power.history");
        setDescription("View a player's power history.");
        setRequiredArgs("<player>");
        setOptionalArgs("[page]");
    }

    @Override
    protected void perform(final CommandContext ctx) {
        if (ctx.arg(0).isBlank()) {
            MsgUtil.send(ctx.getSender(), "<red>Usage: <yellow>/fa power history <player> [page]");
            return;
        }
        new CmdPowerHistory().execute(new CommandContext(
            ctx.getPlugin(),
            ctx.getSender(),
            List.of(ctx.arg(0), ctx.arg(1)).stream().filter(s -> !s.isBlank()).toList(),
            ctx.getRepos(),
            ctx.getConfig(),
            ctx.getLogger()));
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return AdminPowerCommandUtil.onlineNames();
        }
        return List.of();
    }
}

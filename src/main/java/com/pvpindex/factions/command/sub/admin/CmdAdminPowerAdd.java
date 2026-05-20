package com.pvpindex.factions.command.sub.admin;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.service.PowerService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;

/** {@code /fa power add <player> <amount> <reason...>}. */
public final class CmdAdminPowerAdd extends FactionCommand {

    private final PowerService powerService;

    public CmdAdminPowerAdd(final PowerService powerService) {
        super("add");
        setPermission("factions.cmd.admin.power.add");
        setDescription("Add power to a player.");
        setRequiredArgs("<player>", "<amount>", "<reason...>");
        this.powerService = powerService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final double amount;
        try {
            amount = Math.abs(Double.parseDouble(ctx.arg(1)));
        } catch (NumberFormatException e) {
            MsgUtil.send(ctx.getSender(), "<red>Invalid amount: <white>" + ctx.arg(1));
            return;
        }
        final String reason = AdminPowerCommandUtil.joinReason(ctx.getArgs(), 2);
        if (reason.isBlank()) {
            MsgUtil.send(ctx.getSender(), "<red>A reason is required.");
            return;
        }
        mutate(ctx, amount, reason);
    }

    private void mutate(final CommandContext ctx, final double amount, final String reason) {
        try {
            final Optional<PlayerModel> target = AdminPowerCommandUtil.resolvePlayer(powerService, ctx.arg(0));
            if (target.isEmpty()) {
                MsgUtil.sendKey(ctx.getSender(), "general.player-not-found",
                    "<red>Player <yellow>{name}</yellow> not found.", "name", ctx.arg(0));
                return;
            }
            final PlayerModel pm = target.get();
            final PowerService.Result result = powerService.apply(new PowerService.Request(
                pm.getId(), PowerService.Source.ADMIN_ADD, amount, ctx.getSender().getName(),
                "ADMIN_ADD:" + reason, null, null, ctx.getConfig().isPowerFreezeAllowAdminBypass()));
            final String name = AdminPowerCommandUtil.displayName(pm.getId(), ctx.arg(0));
            MsgUtil.sendKey(ctx.getSender(), "power.admin-add-success",
                "<green>Added <yellow>{delta}</yellow> power to <white>{player}</white> "
                    + "(<yellow>{before}</yellow> -> <yellow>{after}</yellow>).",
                "delta", String.format(java.util.Locale.ROOT, "%.2f", result.effectiveDelta()),
                "player", name,
                "before", String.format(java.util.Locale.ROOT, "%.2f", result.before()),
                "after", String.format(java.util.Locale.ROOT, "%.2f", result.after()));
        } catch (com.github.ezframework.jaloquent.exception.StorageException e) {
            MsgUtil.send(ctx.getSender(), "<red>Storage error while adding power.");
        }
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return AdminPowerCommandUtil.onlineNames();
        }
        if (argIndex == 1) {
            return List.of("1", "2", "5");
        }
        return List.of();
    }
}

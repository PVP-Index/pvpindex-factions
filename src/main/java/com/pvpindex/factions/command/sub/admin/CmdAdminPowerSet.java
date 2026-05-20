package com.pvpindex.factions.command.sub.admin;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.service.PowerService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;

/** {@code /fa power set <player> <amount> <reason...>}. */
public final class CmdAdminPowerSet extends FactionCommand {

    private final PowerService powerService;

    public CmdAdminPowerSet(final PowerService powerService) {
        super("set");
        setPermission("factions.cmd.admin.power.set");
        setDescription("Set a player's power to an exact value.");
        setRequiredArgs("<player>", "<amount>", "<reason...>");
        this.powerService = powerService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final double targetAmount;
        try {
            targetAmount = Double.parseDouble(ctx.arg(1));
        } catch (NumberFormatException e) {
            MsgUtil.send(ctx.getSender(), "<red>Invalid amount: <white>" + ctx.arg(1));
            return;
        }
        final String reason = AdminPowerCommandUtil.joinReason(ctx.getArgs(), 2);
        if (reason.isBlank()) {
            MsgUtil.send(ctx.getSender(), "<red>A reason is required.");
            return;
        }
        try {
            final Optional<PlayerModel> target = AdminPowerCommandUtil.resolvePlayer(powerService, ctx.arg(0));
            if (target.isEmpty()) {
                MsgUtil.sendKey(ctx.getSender(), "general.player-not-found",
                    "<red>Player <yellow>{name}</yellow> not found.", "name", ctx.arg(0));
                return;
            }
            final PlayerModel pm = target.get();
            final double delta = targetAmount - pm.getPower();
            final PowerService.Result result = powerService.apply(new PowerService.Request(
                pm.getId(), PowerService.Source.ADMIN_SET, delta, ctx.getSender().getName(),
                "ADMIN_SET:" + reason, null, null, ctx.getConfig().isPowerFreezeAllowAdminBypass()));
            final String name = AdminPowerCommandUtil.displayName(pm.getId(), ctx.arg(0));
            MsgUtil.sendKey(ctx.getSender(), "power.admin-set-success",
                "<green>Set power for <white>{player}</white>: <yellow>{before}</yellow> -> <yellow>{after}</yellow>.",
                "player", name,
                "before", String.format(java.util.Locale.ROOT, "%.2f", result.before()),
                "after", String.format(java.util.Locale.ROOT, "%.2f", result.after()));
        } catch (com.github.ezframework.jaloquent.exception.StorageException e) {
            MsgUtil.send(ctx.getSender(), "<red>Storage error while setting power.");
        }
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return AdminPowerCommandUtil.onlineNames();
        }
        if (argIndex == 1) {
            return List.of("0", "5", "10");
        }
        return List.of();
    }
}

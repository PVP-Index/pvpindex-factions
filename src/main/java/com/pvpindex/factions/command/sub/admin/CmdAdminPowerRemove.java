package com.pvpindex.factions.command.sub.admin;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.service.PowerService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;

/** {@code /fa power remove <player> <amount> <reason...>}. */
public final class CmdAdminPowerRemove extends FactionCommand {

    private final PowerService powerService;

    public CmdAdminPowerRemove(final PowerService powerService) {
        super("remove");
        setPermission("factions.cmd.admin.power.remove");
        setDescription("Remove power from a player.");
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
        try {
            final Optional<PlayerModel> target = AdminPowerCommandUtil.resolvePlayer(powerService, ctx.arg(0));
            if (target.isEmpty()) {
                MsgUtil.sendKey(ctx.getSender(), "general.player-not-found",
                    "<red>Player <yellow>{name}</yellow> not found.", "name", ctx.arg(0));
                return;
            }
            final PlayerModel pm = target.get();
            final PowerService.Result result = powerService.apply(new PowerService.Request(
                pm.getId(), PowerService.Source.ADMIN_REMOVE, -amount, ctx.getSender().getName(),
                "ADMIN_REMOVE:" + reason, null, null, ctx.getConfig().isPowerFreezeAllowAdminBypass()));
            final String name = AdminPowerCommandUtil.displayName(pm.getId(), ctx.arg(0));
            MsgUtil.sendKey(ctx.getSender(), "power.admin-remove-success",
                "<green>Removed <yellow>{delta}</yellow> power from <white>{player}</white> "
                    + "(<yellow>{before}</yellow> -> <yellow>{after}</yellow>).",
                "delta", String.format(java.util.Locale.ROOT, "%.2f", Math.abs(result.effectiveDelta())),
                "player", name,
                "before", String.format(java.util.Locale.ROOT, "%.2f", result.before()),
                "after", String.format(java.util.Locale.ROOT, "%.2f", result.after()));
        } catch (com.github.ezframework.jaloquent.exception.StorageException e) {
            MsgUtil.send(ctx.getSender(), "<red>Storage error while removing power.");
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

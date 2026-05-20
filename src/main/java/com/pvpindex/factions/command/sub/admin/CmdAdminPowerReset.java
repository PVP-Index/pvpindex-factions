package com.pvpindex.factions.command.sub.admin;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.service.PowerService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;

/** {@code /fa power reset <player> <reason...>}. */
public final class CmdAdminPowerReset extends FactionCommand {

    private final PowerService powerService;

    public CmdAdminPowerReset(final PowerService powerService) {
        super("reset");
        setPermission("factions.cmd.admin.power.reset");
        setDescription("Reset player power to configured max.");
        setRequiredArgs("<player>", "<reason...>");
        this.powerService = powerService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final String reason = AdminPowerCommandUtil.joinReason(ctx.getArgs(), 1);
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
            final double desired = ctx.getConfig().getPowerMax();
            final double delta = desired - pm.getPower();
            final PowerService.Result result = powerService.apply(new PowerService.Request(
                pm.getId(), PowerService.Source.ADMIN_RESET, delta, ctx.getSender().getName(),
                "ADMIN_RESET:" + reason, null, null, ctx.getConfig().isPowerFreezeAllowAdminBypass()));
            final String name = AdminPowerCommandUtil.displayName(pm.getId(), ctx.arg(0));
            MsgUtil.sendKey(ctx.getSender(), "power.admin-reset-success",
                "<green>Reset power for <white>{player}</white>: <yellow>{before}</yellow> -> <yellow>{after}</yellow>.",
                "player", name,
                "before", String.format(java.util.Locale.ROOT, "%.2f", result.before()),
                "after", String.format(java.util.Locale.ROOT, "%.2f", result.after()));
        } catch (com.github.ezframework.jaloquent.exception.StorageException e) {
            MsgUtil.send(ctx.getSender(), "<red>Storage error while resetting power.");
        }
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return AdminPowerCommandUtil.onlineNames();
        }
        return List.of();
    }
}

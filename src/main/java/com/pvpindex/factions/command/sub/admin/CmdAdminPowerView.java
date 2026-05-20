package com.pvpindex.factions.command.sub.admin;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.service.PowerService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;

/** {@code /fa power view <player>}. */
public final class CmdAdminPowerView extends FactionCommand {

    private final PowerService powerService;

    public CmdAdminPowerView(final PowerService powerService) {
        super("view");
        setPermission("factions.cmd.admin.power.view");
        setDescription("View a player's power state.");
        setRequiredArgs("<player>");
        this.powerService = powerService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        try {
            final Optional<PlayerModel> target = AdminPowerCommandUtil.resolvePlayer(powerService, ctx.arg(0));
            if (target.isEmpty()) {
                MsgUtil.sendKey(ctx.getSender(), "general.player-not-found",
                    "<red>Player <yellow>{name}</yellow> not found.", "name", ctx.arg(0));
                return;
            }
            final PlayerModel pm = target.get();
            final String name = AdminPowerCommandUtil.displayName(pm.getId(), ctx.arg(0));
            MsgUtil.sendKey(ctx.getSender(), "power.admin-view",
                "<gray>[Power] <white>{player}</white> power=<yellow>{power}</yellow>, frozen=<yellow>{frozen}</yellow>",
                "player", name,
                "power", String.format(java.util.Locale.ROOT, "%.2f", pm.getPower()),
                "frozen", String.valueOf(pm.isPowerFrozen()));
        } catch (com.github.ezframework.jaloquent.exception.StorageException e) {
            MsgUtil.send(ctx.getSender(), "<red>Storage error while reading player power.");
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

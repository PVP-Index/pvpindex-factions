package com.pvpindex.factions.command.sub.admin;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;

/** {@code /fa power freeze <player> <on|off> [reason...]}. */
public final class CmdAdminPowerFreeze extends FactionCommand {

    private final com.pvpindex.factions.service.PowerService powerService;

    public CmdAdminPowerFreeze(final com.pvpindex.factions.service.PowerService powerService) {
        super("freeze");
        setPermission("factions.cmd.admin.power.freeze");
        setDescription("Toggle power freeze for a player.");
        setRequiredArgs("<player>", "<on|off>");
        this.powerService = powerService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final boolean enabled;
        if ("on".equalsIgnoreCase(ctx.arg(1)) || "true".equalsIgnoreCase(ctx.arg(1))) {
            enabled = true;
        } else if ("off".equalsIgnoreCase(ctx.arg(1)) || "false".equalsIgnoreCase(ctx.arg(1))) {
            enabled = false;
        } else {
            MsgUtil.send(ctx.getSender(), "<red>Use <white>on</white> or <white>off</white>.");
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
            pm.setPowerFrozen(enabled);
            ctx.getRepos().players().save(pm);
            final String name = AdminPowerCommandUtil.displayName(pm.getId(), ctx.arg(0));
            MsgUtil.sendKey(ctx.getSender(), "power.admin-freeze-success",
                "<green>Power freeze for <white>{player}</white> is now <yellow>{state}</yellow>.",
                "player", name, "state", enabled ? "on" : "off");
        } catch (com.github.ezframework.jaloquent.exception.StorageException e) {
            MsgUtil.send(ctx.getSender(), "<red>Storage error while updating freeze state.");
        }
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0) {
            return AdminPowerCommandUtil.onlineNames();
        }
        if (argIndex == 1) {
            return List.of("on", "off");
        }
        return List.of();
    }
}

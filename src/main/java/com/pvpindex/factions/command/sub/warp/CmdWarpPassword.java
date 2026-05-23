package com.pvpindex.factions.command.sub.warp;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.WarpModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.WarpService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;
import org.bukkit.entity.Player;

/**
 * {@code /f warp password <name> [newPassword | clear]} — Set or clear the password on a
 * faction warp. Only officers or above can execute this command.
 */
public final class CmdWarpPassword extends FactionCommand {

    private final FactionService factionService;
    private final WarpService warpService;

    public CmdWarpPassword(
            final FactionService factionService,
            final WarpService warpService) {
        super("password");
        setPermission("factions.cmd.warp.password");
        setDescription("Set or clear the password for a faction warp.");
        setRequiredArgs("<warpName>");
        setOptionalArgs("[password | clear]");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.warpService = warpService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> factionOpt =
            CommandGuards.requireFaction(player, factionService);
        if (factionOpt.isEmpty()) {
            return;
        }
        if (!CommandGuards.requireOfficerOrAbove(player, factionService)) {
            return;
        }
        if (ctx.getArgs().isEmpty()) {
            MsgUtil.sendKey(player, "general.invalid-args",
                "<red>Usage: /f warp password <warpName> [password | clear]",
                "usage", "/f warp password <warpName> [password | clear]");
            return;
        }
        final String warpName = ctx.arg(0).toLowerCase();
        final Optional<WarpModel> warpOpt =
            warpService.getWarp(factionOpt.get().getId(), warpName);
        if (warpOpt.isEmpty()) {
            MsgUtil.sendKey(player, "warp.not-found",
                "<red>Warp <yellow>{name}</yellow> not found.", "name", warpName);
            return;
        }

        if (ctx.getArgs().size() < 2) {
            // Show current password status
            MsgUtil.send(player, warpOpt.get().hasPassword()
                ? "<yellow>Warp <white>" + warpName + "</white> has a password set."
                : "<yellow>Warp <white>" + warpName + "</white> has no password.");
            return;
        }

        final String value = ctx.arg(1);
        if ("clear".equalsIgnoreCase(value)) {
            warpService.setWarpPassword(factionOpt.get().getId(), warpName, null);
            MsgUtil.sendKey(player, "warp.password-cleared",
                "<yellow>Password cleared for warp <yellow>{name}</yellow>.", "name", warpName);
        } else {
            warpService.setWarpPassword(factionOpt.get().getId(), warpName, value);
            MsgUtil.sendKey(player, "warp.password-set",
                "<green>Password set for warp <yellow>{name}</yellow>.", "name", warpName);
        }
    }

    @Override
    protected List<String> complete(final CommandContext ctx, final int argIndex) {
        if (argIndex == 0 && ctx.isPlayer()) {
            final Optional<FactionModel> factionOpt =
                factionService.getFactionByPlayer(((Player) ctx.getSender()).getUniqueId());
            return factionOpt
                .map(f -> warpService.getWarps(f.getId()).stream()
                    .map(WarpModel::getName)
                    .toList())
                .orElse(List.of());
        }
        if (argIndex == 1) {
            return List.of("clear");
        }
        return List.of();
    }
}

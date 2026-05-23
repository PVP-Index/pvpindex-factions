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
 * {@code /f warp cost <name> <amount>} — Set the per-use economy cost of a faction warp.
 * Only officers or above can execute this command.
 */
public final class CmdWarpCost extends FactionCommand {

    private final FactionService factionService;
    private final WarpService warpService;

    public CmdWarpCost(
            final FactionService factionService,
            final WarpService warpService) {
        super("cost");
        setPermission("factions.cmd.warp.cost");
        setDescription("Set the per-use cost of a faction warp (0 = free).");
        setRequiredArgs("<warpName>", "<amount>");
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
        if (ctx.getArgs().size() < 2) {
            MsgUtil.sendKey(player, "general.invalid-args",
                "<red>Usage: /f warp cost <warpName> <amount>",
                "usage", "/f warp cost <warpName> <amount>");
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
        final double amount;
        try {
            amount = Double.parseDouble(ctx.arg(1));
        } catch (NumberFormatException ignored) {
            MsgUtil.sendKey(player, "bank.invalid-amount",
                "<red>Amount must be a non-negative number.");
            return;
        }
        if (amount < 0) {
            MsgUtil.sendKey(player, "bank.invalid-amount",
                "<red>Amount must be a non-negative number.");
            return;
        }
        warpService.setWarpCost(factionOpt.get().getId(), warpName, amount);
        MsgUtil.sendKey(player, "warp.cost-set",
            "<green>Use cost for warp <yellow>{name}</yellow> set to <gold>{cost}</gold>.",
            "name", warpName,
            "cost", String.format("%.2f", amount));
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
        return List.of();
    }
}

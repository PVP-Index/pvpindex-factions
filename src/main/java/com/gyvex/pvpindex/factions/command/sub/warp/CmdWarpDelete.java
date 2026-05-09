package com.gyvex.pvpindex.factions.command.sub.warp;

import com.gyvex.pvpindex.factions.command.CommandContext;
import com.gyvex.pvpindex.factions.command.FactionCommand;
import com.gyvex.pvpindex.factions.command.CommandGuards;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.data.model.WarpModel;
import com.gyvex.pvpindex.factions.service.FactionService;
import com.gyvex.pvpindex.factions.service.WarpService;
import com.gyvex.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;
import org.bukkit.entity.Player;

/** {@code /f warp delete <name>} — Delete a faction warp. */
public final class CmdWarpDelete extends FactionCommand {

    private final FactionService factionService;
    private final WarpService warpService;

    public CmdWarpDelete(
            final FactionService factionService,
            final WarpService warpService) {
        super("delete");
        setPermission("factions.cmd.setwarp");
        setDescription("Delete a faction warp.");
        setRequiredArgs("<name>");
        setAliases("remove");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.warpService = warpService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> factionOpt = CommandGuards.requireFaction(player, factionService);
        if (factionOpt.isEmpty()) {
            return;
        }
        if (!CommandGuards.requireOfficerOrAbove(player, factionService)) {
            return;
        }
        final String name = ctx.arg(0);
        if (warpService.deleteWarp(factionOpt.get().getId(), name)) {
            MsgUtil.send(player, "<yellow>Warp '" + name + "' deleted.");
        } else {
            MsgUtil.send(player, "<red>Warp '" + name + "' not found.");
        }
    }

    /** Completes existing warp names for the player's faction. */
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

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

/** {@code /f warp list [page]}. */
public final class CmdWarpList extends FactionCommand {

    private final FactionService factionService;
    private final WarpService warpService;

    public CmdWarpList(final FactionService factionService, final WarpService warpService) {
        super("list");
        setPermission("factions.cmd.warp");
        setDescription("List faction warps.");
        setOptionalArgs("[page]");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.warpService = warpService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> faction = CommandGuards.requireFaction(player, factionService);
        if (faction.isEmpty()) {
            return;
        }
        final List<WarpModel> warps = warpService.getWarps(faction.get().getId());
        if (warps.isEmpty()) {
            MsgUtil.send(player, "<yellow>Your faction has no warps.");
            return;
        }
        final int pageSize = Math.max(1, ctx.getConfig().getWarpListPageSize());
        final int page = parsePage(ctx.arg(0));
        final int start = Math.max(0, (page - 1) * pageSize);
        final int end = Math.min(warps.size(), start + pageSize);
        MsgUtil.send(player, "<gold>== Faction Warps (Page " + page + ") ==");
        for (int i = start; i < end; i++) {
            player.sendMessage(MsgUtil.warpEntry(warps.get(i).getName()));
        }
    }

    private int parsePage(final String input) {
        try {
            return Math.max(1, Integer.parseInt(input));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }
}

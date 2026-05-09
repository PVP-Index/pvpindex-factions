package com.pvpindex.factions.command.sub.warp;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.integration.worldguard.TerritoryGuard;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.WarpService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import org.bukkit.entity.Player;

/** {@code /f warp set <name>} — Set a faction warp at your current location. */
public final class CmdWarpSet extends FactionCommand {

    private final FactionService factionService;
    private final WarpService warpService;
    private final TerritoryGuard territoryGuard;

    public CmdWarpSet(
            final FactionService factionService,
            final WarpService warpService,
            final TerritoryGuard territoryGuard) {
        super("set");
        setPermission("factions.cmd.setwarp");
        setDescription("Set a faction warp at your current location.");
        setRequiredArgs("<name>");
        setOptionalArgs("[x]", "[y]", "[z]", "[world]");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.warpService = warpService;
        this.territoryGuard = territoryGuard;
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
        final org.bukkit.Location target = resolveLocation(player, ctx);
        if (!territoryGuard.canModifyTerritory(player, target)) {
            MsgUtil.send(player, "<red>You cannot set a warp in this protected region.");
            return;
        }
        if (warpService.setWarp(
                factionOpt.get().getId(), name, target, player.getUniqueId())) {
            MsgUtil.send(player, "<green>Warp '" + name + "' set.");
        } else {
            MsgUtil.send(player, "<red>Could not set warp (limit reached?).");
        }
    }

    private org.bukkit.Location resolveLocation(final Player player, final CommandContext ctx) {
        if (ctx.getArgs().size() < 4) {
            return player.getLocation();
        }
        try {
            final double x = Double.parseDouble(ctx.arg(1));
            final double y = Double.parseDouble(ctx.arg(2));
            final double z = Double.parseDouble(ctx.arg(3));
            final org.bukkit.World world = ctx.arg(4).isBlank()
                ? player.getWorld()
                : org.bukkit.Bukkit.getWorld(ctx.arg(4));
            if (world == null) {
                return player.getLocation();
            }
            return new org.bukkit.Location(world, x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch());
        } catch (NumberFormatException ignored) {
            return player.getLocation();
        }
    }
}

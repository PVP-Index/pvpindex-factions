package com.pvpindex.factions.command.sub.warp;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.WarpModel;
import com.pvpindex.factions.integration.essentials.EssentialsInterop;
import com.pvpindex.factions.integration.worldguard.TerritoryGuard;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.WarpService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * {@code /f warp [set|delete|<name>]} — Warp group command.
 *
 * <p>Children handle {@code set} and {@code delete}. When the first argument
 * does not match a child, {@link #perform} treats it as a warp name and
 * teleports the player to that warp.
 *
 * <p>With no arguments, the command lists available warps.
 */
public final class CmdWarp extends FactionCommand {

    private final FactionService factionService;
    private final WarpService warpService;
    private final TerritoryGuard territoryGuard;
    private final EssentialsInterop essentialsInterop;

    public CmdWarp(
            final FactionService factionService,
            final WarpService warpService,
            final TerritoryGuard territoryGuard,
            final EssentialsInterop essentialsInterop) {
        super("warp");
        setPermission("factions.cmd.warp");
        setDescription("Teleport to or manage faction warps.");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.warpService = warpService;
        this.territoryGuard = territoryGuard;
        this.essentialsInterop = essentialsInterop;
        addChild(new CmdWarpSet(factionService, warpService, territoryGuard));
        addChild(new CmdWarpDelete(factionService, warpService));
        addChild(new CmdWarpList(factionService, warpService));
    }

    /** Handles both the "list warps" (no args) and "teleport" (<name>) cases. */
    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> factionOpt = CommandGuards.requireFaction(player, factionService);
        if (factionOpt.isEmpty()) {
            return;
        }
        if (essentialsInterop.isJailed(player)) {
            MsgUtil.sendKey(player, "warp.jailed", "<red>You cannot use warps while jailed.");
            return;
        }
        if (ctx.getArgs().isEmpty()) {
            // List available warps
            final var warps = warpService.getWarps(factionOpt.get().getId());
            if (warps.isEmpty()) {
                MsgUtil.send(player, "<yellow>Your faction has no warps.");
                return;
            }
            MsgUtil.send(player, "<gold>== Faction Warps ==");
            warps.forEach(w -> MsgUtil.send(player, MsgUtil.warpEntry(w.getName())));
            return;
        }
        // Teleport to named warp
        final String warpName = ctx.arg(0).toLowerCase();
        final Optional<WarpModel> warpOpt =
            warpService.getWarp(factionOpt.get().getId(), warpName);
        if (warpOpt.isEmpty()) {
            MsgUtil.send(player, "<red>Warp '" + warpName + "' not found.");
            return;
        }
        final Location dest = warpOpt.get().toLocation();
        if (dest == null || dest.getWorld() == null) {
            MsgUtil.send(player, "<red>Warp world not loaded.");
            return;
        }
        if (essentialsInterop.teleport(player, dest,
                () -> MsgUtil.sendKey(player, "warp.teleported",
                    "<green>Teleported to warp <yellow>{name}</yellow>.", "name", warpName),
                () -> MsgUtil.sendKey(player, "warp.teleport-failed", "<red>Warp teleport failed."))) {
            return;
        }
        player.teleport(dest);
        MsgUtil.sendKey(player, "warp.teleported",
            "<green>Teleported to warp <yellow>{name}</yellow>.", "name", warpName);
    }

    /**
     * Provide faction warp names for tab completion at arg position 0, merged
     * (by the base class) with child names "set" and "delete".
     */
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

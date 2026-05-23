package com.pvpindex.factions.command.sub.warp;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.WarpModel;
import com.pvpindex.factions.integration.essentials.EssentialsInterop;
import com.pvpindex.factions.integration.vault.VaultEconomy;
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
    private final VaultEconomy vaultEconomy;

    public CmdWarp(
            final FactionService factionService,
            final WarpService warpService,
            final TerritoryGuard territoryGuard,
            final EssentialsInterop essentialsInterop,
            final VaultEconomy vaultEconomy) {
        super("warp");
        setPermission("factions.cmd.warp");
        setDescription("Teleport to or manage faction warps.");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.warpService = warpService;
        this.territoryGuard = territoryGuard;
        this.essentialsInterop = essentialsInterop;
        this.vaultEconomy = vaultEconomy;
        addChild(new CmdWarpSet(factionService, warpService, territoryGuard));
        addChild(new CmdWarpDelete(factionService, warpService));
        addChild(new CmdWarpList(factionService, warpService));
        addChild(new CmdWarpPassword(factionService, warpService));
        addChild(new CmdWarpCost(factionService, warpService));
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
                MsgUtil.sendKey(player, "custom.warp.none", "<yellow>Your faction has no warps.");
                return;
            }
            MsgUtil.sendKey(player, "custom.warp.header", "<gold>== Faction Warps ==");
            warps.forEach(w -> MsgUtil.send(player, MsgUtil.warpEntry(w.getName())));
            return;
        }
        // Teleport to named warp
        final String warpName = ctx.arg(0).toLowerCase();
        final Optional<WarpModel> warpOpt =
            warpService.getWarp(factionOpt.get().getId(), warpName);
        if (warpOpt.isEmpty()) {
            MsgUtil.sendKey(player, "warp.not-found", "<red>Warp <yellow>{name}</yellow> not found.", "name", warpName);
            return;
        }
        final WarpModel warp = warpOpt.get();

        // Password check
        if (warp.hasPassword()) {
            final String supplied = ctx.getArgs().size() > 1 ? ctx.arg(1) : null;
            if (supplied == null || !warp.getPassword().equals(supplied)) {
                MsgUtil.sendKey(player, "warp.password-required",
                    "<red>This warp requires a password: /f warp {name} <password>",
                    "name", warpName);
                return;
            }
        }

        // Cost check
        if (warp.hasCost()) {
            if (vaultEconomy == null || !vaultEconomy.isEnabled()) {
                MsgUtil.sendKey(player, "warp.cost-no-economy",
                    "<red>An economy plugin is required to use this warp.");
                return;
            }
            final double cost = warp.getUseCost();
            final double balance = vaultEconomy.getBalance(player);
            if (balance < cost) {
                MsgUtil.sendKey(player, "warp.cost-insufficient",
                    "<red>You need <gold>{cost}</gold> to use this warp (balance: <gold>{balance}</gold>).",
                    "cost", String.format("%.2f", cost),
                    "balance", String.format("%.2f", balance));
                return;
            }
            vaultEconomy.withdraw(player, cost);
            MsgUtil.sendKey(player, "warp.cost-charged",
                "<green>Charged <gold>{cost}</gold> for warp <yellow>{name}</yellow>.",
                "cost", String.format("%.2f", cost),
                "name", warpName,
                "balance", String.format("%.2f", vaultEconomy.getBalance(player)));
        }

        final Location dest = warp.toLocation();
        if (dest == null || dest.getWorld() == null) {
            MsgUtil.sendKey(player, "custom.warp.world-not-loaded", "<red>Warp world not loaded.");
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

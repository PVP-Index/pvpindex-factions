package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.integration.essentials.EssentialsInterop;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** {@code /f home} — teleport to faction home. */
public final class CmdHome extends FactionCommand {

    private final FactionService factionService;
    private final EssentialsInterop essentialsInterop;

    public CmdHome(final FactionService factionService, final EssentialsInterop essentialsInterop) {
        super("home");
        setPermission("factions.cmd.home");
        setDescription("Teleport to faction home.");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.essentialsInterop = essentialsInterop;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        if (CommandGuards.requireFaction(player, factionService).isEmpty()) {
            return;
        }
        final Location home = factionService.getFactionHome(player.getUniqueId()).orElse(null);
        if (home == null || home.getWorld() == null) {
            MsgUtil.send(player, "<red>Your faction has not set a home.");
            return;
        }
        if (essentialsInterop.teleportToFactionHome(player, home)) {
            return;
        }
        player.teleport(home);
        MsgUtil.send(player, "<green>Teleported to faction home.");
    }
}

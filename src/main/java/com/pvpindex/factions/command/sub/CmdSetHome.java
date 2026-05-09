package com.pvpindex.factions.command.sub;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.integration.worldguard.TerritoryGuard;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import org.bukkit.entity.Player;

/** {@code /f sethome} — set faction home at your current location. */
public final class CmdSetHome extends FactionCommand {

    private final FactionService factionService;
    private final TerritoryGuard territoryGuard;

    public CmdSetHome(final FactionService factionService, final TerritoryGuard territoryGuard) {
        super("sethome");
        setPermission("factions.cmd.sethome");
        setDescription("Set faction home at your current location.");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.territoryGuard = territoryGuard;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        if (CommandGuards.requireFaction(player, factionService).isEmpty()) {
            return;
        }
        if (!CommandGuards.requireOfficerOrAbove(player, factionService)) {
            return;
        }
        if (!territoryGuard.canModifyTerritory(player, player.getLocation())) {
            MsgUtil.send(player, "<red>You cannot set faction home in this protected region.");
            return;
        }
        if (factionService.setFactionHome(player.getUniqueId(), player.getLocation())) {
            MsgUtil.send(player, "<green>Faction home set.");
            return;
        }
        MsgUtil.send(player, "<red>Failed to set faction home.");
    }
}

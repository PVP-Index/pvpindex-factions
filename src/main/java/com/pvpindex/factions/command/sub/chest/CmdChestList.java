package com.pvpindex.factions.command.sub.chest;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.TeamChestService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import org.bukkit.entity.Player;

public final class CmdChestList extends FactionCommand {

    private final FactionService factionService;
    private final TeamChestService teamChestService;

    public CmdChestList(final FactionService factionService, final TeamChestService teamChestService) {
        super("list");
        setPermission("factions.cmd.chest");
        setDescription("List faction team chests.");
        setRequiresPlayer(true);
        this.factionService = factionService;
        this.teamChestService = teamChestService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> factionOpt = ChestCommandSupport.requireFaction(player, factionService);
        if (factionOpt.isEmpty()) {
            return;
        }
        final var names = teamChestService.getChestNames(factionOpt.get().getId());
        if (names.isEmpty()) {
            MsgUtil.sendKey(player, "chest.none", "<yellow>Your faction has no team chests.");
            return;
        }
        MsgUtil.sendKey(player, "chest.list-header", "<gold>== Team Chests ==");
        names.forEach(name -> MsgUtil.sendKey(player, "chest.list-entry", "<gray>- <yellow>{name}</yellow>", "name", name));
    }
}

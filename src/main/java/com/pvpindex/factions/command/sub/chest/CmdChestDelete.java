package com.pvpindex.factions.command.sub.chest;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.service.TeamChestService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.entity.Player;

public final class CmdChestDelete extends FactionCommand {

    private final FactionService factionService;
    private final TeamChestService teamChestService;

    public CmdChestDelete(final FactionService factionService, final TeamChestService teamChestService) {
        super("delete");
        setPermission("factions.cmd.chest.delete");
        setDescription("Delete a faction team chest.");
        setRequiresPlayer(true);
        setRequiredArgs("<name>");
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
        if (!CommandGuards.requireOfficerOrAbove(player, factionService)) {
            return;
        }
        final String name = ctx.arg(0).toLowerCase(Locale.ROOT);
        if (!teamChestService.deleteChest(factionOpt.get().getId(), name)) {
            MsgUtil.sendKey(player, "chest.not-found", "<red>Chest <yellow>{name}</yellow> was not found.", "name", name);
            return;
        }
        MsgUtil.sendKey(player, "chest.deleted", "<green>Deleted chest <yellow>{name}</yellow>.", "name", name);
    }
}

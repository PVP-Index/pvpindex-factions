package com.pvpindex.factions.command.sub.relation;

import com.pvpindex.factions.command.CommandContext;
import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.command.FactionCommand;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import org.bukkit.entity.Player;

/** {@code /f relation wishes}. */
public final class CmdRelationWishes extends FactionCommand {

    private final FactionService factionService;

    public CmdRelationWishes(final FactionService factionService) {
        super("wishes");
        setPermission("factions.cmd.relation");
        setDescription("Show your faction's relation wishes.");
        setRequiresPlayer(true);
        this.factionService = factionService;
    }

    @Override
    protected void perform(final CommandContext ctx) {
        final Player player = (Player) ctx.getSender();
        final Optional<FactionModel> source = CommandGuards.requireFaction(player, factionService);
        if (source.isEmpty()) {
            return;
        }
        final String json = source.get().getRelationsJson();
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            MsgUtil.send(player, "<gray>No relation wishes set.");
            return;
        }
        MsgUtil.send(player, "<gold>Relation wishes are currently set. Use <white>/f relation list</white><gold>.");
    }
}


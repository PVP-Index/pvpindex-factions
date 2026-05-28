package com.pvpindex.factions.command.sub.chest;

import com.pvpindex.factions.command.CommandGuards;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import java.util.Optional;
import org.bukkit.entity.Player;

final class ChestCommandSupport {

    private ChestCommandSupport() {
    }

    static Optional<FactionModel> requireFaction(final Player player, final FactionService factionService) {
        return CommandGuards.requireFaction(player, factionService);
    }
}

package com.pvpindex.factions.command;

import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.Optional;
import org.bukkit.entity.Player;

/**
 * Shared guard helpers to keep command classes small and consistent.
 */
public final class CommandGuards {

    private CommandGuards() {
    }

    public static Optional<FactionModel> requireFaction(
            final Player player, final FactionService factionService) {
        final Optional<FactionModel> factionOpt = factionService.getFactionByPlayer(player.getUniqueId());
        if (factionOpt.isEmpty()) {
            MsgUtil.send(player, "<red>You are not in a faction.");
        }
        return factionOpt;
    }

    public static boolean requireOwner(final Player player, final FactionService factionService) {
        if (!factionService.isOwner(player.getUniqueId())) {
            MsgUtil.send(player, "<red>Only the faction owner can do that.");
            return false;
        }
        return true;
    }

    public static boolean requireOfficerOrAbove(final Player player, final FactionService factionService) {
        if (!factionService.isOfficerOrAbove(player.getUniqueId())) {
            MsgUtil.send(player, "<red>Only officers or above can do that.");
            return false;
        }
        return true;
    }
}


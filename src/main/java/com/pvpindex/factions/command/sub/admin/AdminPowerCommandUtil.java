package com.pvpindex.factions.command.sub.admin;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.service.PowerService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

final class AdminPowerCommandUtil {

    private AdminPowerCommandUtil() { }

    static Optional<PlayerModel> resolvePlayer(final PowerService powerService, final String input)
        throws StorageException {
        return powerService.findPlayerByNameOrUuid(input);
    }

    static String displayName(final String playerId, final String fallbackInput) {
        try {
            final OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(playerId));
            return op.getName() != null ? op.getName() : fallbackInput;
        } catch (IllegalArgumentException e) {
            return fallbackInput;
        }
    }

    static String joinReason(final List<String> args, final int startIndex) {
        if (args.size() <= startIndex) {
            return "";
        }
        return String.join(" ", args.subList(startIndex, args.size())).trim();
    }

    static List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }
}


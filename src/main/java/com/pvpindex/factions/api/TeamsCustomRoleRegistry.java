package com.pvpindex.factions.api;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.RankModel;
import com.skyblockexp.teamsapi.api.TeamsAPI;
import com.skyblockexp.teamsapi.model.TeamRole;
import com.skyblockexp.teamsapi.model.TeamRoleDefinition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

/**
 * Registers and unregisters TeamsAPI 2.4 custom role definitions for faction ranks.
 */
public final class TeamsCustomRoleRegistry {

    private TeamsCustomRoleRegistry() {
    }

    public static String roleKey(final String factionId, final String rankId, final String rankName) {
        final String safeName = rankName == null ? "role"
            : rankName.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        return "pvpindex:" + factionId + ":" + rankId + ":" + safeName;
    }

    public static TeamRoleDefinition toRoleDefinition(final RankModel rank) {
        final TeamRole builtInRole = TeamRoleMapper.rankToRole(rank);
        final String prefix = rank.getPrefix() == null ? builtInRole.getPrefix() : rank.getPrefix();
        return new TeamRoleDefinition(
            roleKey(rank.getFactionId(), rank.getId(), rank.getName()),
            rank.getPriority(),
            prefix == null ? "" : prefix
        );
    }

    public static void registerAll(final Plugin plugin, final Repositories repos, final Logger logger) {
        try {
            for (final var faction : repos.factions().findAll()) {
                for (final RankModel rank : repos.ranks().findByFactionId(faction.getId())) {
                    TeamsAPI.registerCustomRole(plugin, toRoleDefinition(rank));
                }
            }
        } catch (StorageException e) {
            logger.warning("Failed to register TeamsAPI custom roles: " + e.getMessage());
        }
    }

    public static void unregisterAllForPluginData(final Repositories repos, final Logger logger) {
        try {
            final Collection<String> keys = collectKeys(repos);
            for (final String key : keys) {
                TeamsAPI.unregisterCustomRole(key);
            }
        } catch (StorageException e) {
            logger.warning("Failed to unregister TeamsAPI custom roles: " + e.getMessage());
        }
    }

    private static Collection<String> collectKeys(final Repositories repos) throws StorageException {
        final List<String> keys = new ArrayList<>();
        for (final var faction : repos.factions().findAll()) {
            for (final RankModel rank : repos.ranks().findByFactionId(faction.getId())) {
                keys.add(roleKey(rank.getFactionId(), rank.getId(), rank.getName()));
            }
        }
        return keys;
    }
}

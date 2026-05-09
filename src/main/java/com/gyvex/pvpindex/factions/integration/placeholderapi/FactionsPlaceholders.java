package com.gyvex.pvpindex.factions.integration.placeholderapi;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.gyvex.pvpindex.factions.data.Repositories;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.data.model.PlayerModel;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion providing {@code %pvpindex_<placeholder>%} values.
 *
 * <p>Registered only when PlaceholderAPI is present on the server.
 */
public final class FactionsPlaceholders extends PlaceholderExpansion {

    private static final String IDENTIFIER = "pvpindex";
    private static final String AUTHOR = "gyvex";
    private static final String VERSION = "1.0.0";
    private static final String NONE = "None";

    private final Repositories repos;
    private final Logger logger;

    public FactionsPlaceholders(final Repositories repos, final Logger logger) {
        this.repos = repos;
        this.logger = logger;
    }

    @Override
    public @NotNull String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public @NotNull String getAuthor() {
        return AUTHOR;
    }

    @Override
    public @NotNull String getVersion() {
        return VERSION;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(final OfflinePlayer player, final @NotNull String params) {
        try {
            return resolve(player, params);
        } catch (StorageException e) {
            logger.log(Level.WARNING, "PlaceholderAPI lookup failed for " + params, e);
            return "";
        }
    }

    // -------------------------------------------------------------------------
    // Placeholder resolution
    // -------------------------------------------------------------------------

    private String resolve(final OfflinePlayer player, final String params) throws StorageException {
        final Optional<PlayerModel> pmOpt = repos.players()
            .find(player.getUniqueId().toString());

        return switch (params) {
            case "faction_name" -> {
                if (pmOpt.isEmpty() || !pmOpt.get().isInFaction()) yield NONE;
                yield repos.factions().find(pmOpt.get().getFactionId())
                    .map(FactionModel::getName).orElse(NONE);
            }
            case "faction_power" -> {
                if (pmOpt.isEmpty() || !pmOpt.get().isInFaction()) yield "0";
                final double power = repos.players()
                    .findByFactionId(pmOpt.get().getFactionId())
                    .stream().mapToDouble(PlayerModel::getPower).sum();
                yield String.valueOf((int) power);
            }
            case "faction_members" -> {
                if (pmOpt.isEmpty() || !pmOpt.get().isInFaction()) yield "0";
                yield String.valueOf(
                    repos.players().findByFactionId(pmOpt.get().getFactionId()).size());
            }
            case "faction_land" -> {
                if (pmOpt.isEmpty() || !pmOpt.get().isInFaction()) yield "0";
                yield String.valueOf(
                    repos.board().countByFactionId(pmOpt.get().getFactionId()));
            }
            case "faction_bank" -> {
                if (pmOpt.isEmpty() || !pmOpt.get().isInFaction()) yield "0.0";
                yield repos.factions().find(pmOpt.get().getFactionId())
                    .map(f -> String.valueOf(f.getBank())).orElse("0.0");
            }
            case "player_power" -> pmOpt.map(p -> String.valueOf(p.getPower())).orElse("0");
            default -> null;
        };
    }
}

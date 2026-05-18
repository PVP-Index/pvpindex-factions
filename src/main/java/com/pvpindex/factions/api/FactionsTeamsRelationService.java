package com.pvpindex.factions.api;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.Relation;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionServiceImpl;
import com.skyblockexp.teamsapi.api.TeamsRelationService;
import com.skyblockexp.teamsapi.event.TeamRelationChangeEvent;
import com.skyblockexp.teamsapi.model.TeamRelation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;

/**
 * Adapts the internal factions relation system to the TeamsAPI
 * {@link TeamsRelationService} interface.
 *
 * <p>Relations are stored per-faction as a JSON map in {@code relations_json}.
 * The adapter translates between the internal {@link Relation} enum and
 * {@link TeamRelation}, skipping the {@code MEMBER} value which has no
 * TeamsAPI equivalent.</p>
 *
 * <p>This class is only instantiated when TeamsAPI is present on the server.</p>
 */
public class FactionsTeamsRelationService implements TeamsRelationService {

    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;

    /**
     * Creates a new {@link FactionsTeamsRelationService}.
     *
     * @param impl the internal faction service; must not be {@code null}
     */
    public FactionsTeamsRelationService(final FactionServiceImpl impl) {
        this.repos = impl.getRepos();
        this.config = impl.getConfig();
        this.logger = impl.getLogger();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Fires {@link TeamRelationChangeEvent} before persisting. Returns
     * {@code false} if the event is cancelled or either team does not exist.
     * Setting {@link TeamRelation#NEUTRAL} removes a previously declared
     * relation.</p>
     */
    @Override
    public boolean setRelation(
            final UUID fromTeamId,
            final UUID toTeamId,
            final TeamRelation relation,
            final UUID initiatorUUID) {
        try {
            final Optional<FactionModel> fromOpt = repos.factions().find(fromTeamId.toString());
            final Optional<FactionModel> toOpt = repos.factions().find(toTeamId.toString());
            if (fromOpt.isEmpty() || toOpt.isEmpty()) {
                return false;
            }
            final FactionModel fromFaction = fromOpt.get();
            final FactionModel toFaction = toOpt.get();

            final Map<String, Relation> fromMap = parseRelations(fromFaction.getRelationsJson());
            final Relation existing = fromMap.getOrDefault(toTeamId.toString(), Relation.NEUTRAL);
            final TeamRelation oldRelation = toTeamRelation(existing);

            final FactionTeam fromTeam = wrap(fromFaction);
            final FactionTeam toTeam = wrap(toFaction);
            final TeamRelationChangeEvent event = new TeamRelationChangeEvent(
                    fromTeam, toTeam, initiatorUUID, oldRelation, relation);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }

            final TeamRelation effective = event.getNewRelation();
            if (effective == TeamRelation.NEUTRAL) {
                fromMap.remove(toTeamId.toString());
            } else {
                fromMap.put(toTeamId.toString(), toInternalRelation(effective));
            }
            fromFaction.setRelationsJson(serializeRelations(fromMap));
            repos.factions().save(fromFaction);
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE,
                    "Failed to set relation from " + fromTeamId + " to " + toTeamId, e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@link TeamRelation#NEUTRAL} if no explicit relation has been
     * declared or if either team is not found.</p>
     */
    @Override
    public TeamRelation getRelation(final UUID fromTeamId, final UUID toTeamId) {
        try {
            final Optional<FactionModel> fromOpt = repos.factions().find(fromTeamId.toString());
            if (fromOpt.isEmpty()) {
                return TeamRelation.NEUTRAL;
            }
            final Map<String, Relation> fromMap = parseRelations(fromOpt.get().getRelationsJson());
            return toTeamRelation(fromMap.getOrDefault(toTeamId.toString(), Relation.NEUTRAL));
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to get relation from " + fromTeamId, e);
            return TeamRelation.NEUTRAL;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns all non-neutral relations declared by the team as an
     * unmodifiable map. The {@code MEMBER} internal relation is excluded.
     * Returns an empty map if the team is not found.</p>
     */
    @Override
    public Map<UUID, TeamRelation> getRelations(final UUID teamId) {
        try {
            final Optional<FactionModel> opt = repos.factions().find(teamId.toString());
            if (opt.isEmpty()) {
                return Collections.emptyMap();
            }
            final Map<String, Relation> raw = parseRelations(opt.get().getRelationsJson());
            final Map<UUID, TeamRelation> result = new HashMap<>();
            for (final Map.Entry<String, Relation> entry : raw.entrySet()) {
                final Relation r = entry.getValue();
                if (r == Relation.NEUTRAL || r == Relation.MEMBER) {
                    continue;
                }
                try {
                    result.put(UUID.fromString(entry.getKey()), toTeamRelation(r));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed faction-id keys
                }
            }
            return Collections.unmodifiableMap(result);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to get relations for " + teamId, e);
            return Collections.emptyMap();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Removes all outgoing relations from {@code teamId} and all incoming
     * references to {@code teamId} from every other faction. Returns
     * {@code false} if the team is not found or had no relations to clear.</p>
     */
    @Override
    public boolean clearRelations(final UUID teamId) {
        try {
            final Optional<FactionModel> opt = repos.factions().find(teamId.toString());
            if (opt.isEmpty()) {
                return false;
            }
            final FactionModel faction = opt.get();
            final Map<String, Relation> ownMap = parseRelations(faction.getRelationsJson());
            boolean hadRelations = !ownMap.isEmpty();

            if (hadRelations) {
                faction.setRelationsJson("{}");
                repos.factions().save(faction);
            }

            final Collection<FactionModel> allFactions = repos.factions().findAll();
            final String teamIdStr = teamId.toString();
            for (final FactionModel other : allFactions) {
                if (other.getId().equals(teamIdStr)) {
                    continue;
                }
                final Map<String, Relation> otherMap = parseRelations(other.getRelationsJson());
                if (otherMap.remove(teamIdStr) != null) {
                    other.setRelationsJson(serializeRelations(otherMap));
                    repos.factions().save(other);
                    hadRelations = true;
                }
            }
            return hadRelations;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to clear relations for " + teamId, e);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private FactionTeam wrap(final FactionModel model) {
        return new FactionTeam(model, repos, config, logger);
    }

    private static TeamRelation toTeamRelation(final Relation r) {
        return switch (r) {
            case ALLY -> TeamRelation.ALLY;
            case TRUCE -> TeamRelation.TRUCE;
            case ENEMY -> TeamRelation.ENEMY;
            default -> TeamRelation.NEUTRAL;
        };
    }

    private static Relation toInternalRelation(final TeamRelation r) {
        return switch (r) {
            case ALLY -> Relation.ALLY;
            case TRUCE -> Relation.TRUCE;
            case ENEMY -> Relation.ENEMY;
            default -> Relation.NEUTRAL;
        };
    }

    private Map<String, Relation> parseRelations(final String json) {
        final Map<String, Relation> out = new HashMap<>();
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return out;
        }
        final String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return out;
        }
        final String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return out;
        }
        for (final String rawEntry : body.split(",")) {
            final String[] kv = rawEntry.split(":", 2);
            if (kv.length != 2) {
                continue;
            }
            final String key = stripQuotes(kv[0].trim());
            final String value = stripQuotes(kv[1].trim());
            try {
                out.put(key, Relation.valueOf(value));
            } catch (IllegalArgumentException ignored) {
                // ignore invalid serialized relation values
            }
        }
        return out;
    }

    private String serializeRelations(final Map<String, Relation> map) {
        final StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (final Map.Entry<String, Relation> entry : map.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            out.append('"').append(entry.getKey()).append('"')
                    .append(':')
                    .append('"').append(entry.getValue().name()).append('"');
        }
        out.append('}');
        return out.toString();
    }

    private static String stripQuotes(final String value) {
        String out = value;
        if (out.startsWith("\"")) {
            out = out.substring(1);
        }
        if (out.endsWith("\"")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }
}

package com.pvpindex.factions.service;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.Relation;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.event.FactionCreateEvent;
import com.pvpindex.factions.event.FactionDisbandEvent;
import com.pvpindex.factions.event.FactionJoinEvent;
import com.pvpindex.factions.event.FactionLeaveEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

/**
 * Core faction business logic — no TeamsAPI dependency.
 *
 * <p>This class is the single source of truth for faction mutations. It fires
 * only internal faction events (e.g. {@link FactionCreateEvent}). When TeamsAPI
 * is present the {@code api.FactionsTeamsService} adapter wraps this impl and
 * additionally fires the corresponding TeamsAPI events.
 */
public class FactionServiceImpl implements FactionService {

    private final Plugin plugin;
    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;
    private final ConcurrentHashMap<UUID, Boolean> flyStateByPlayer = new ConcurrentHashMap<>();

    public FactionServiceImpl(
            final Plugin plugin,
            final Repositories repos,
            final FactionsConfig config,
            final Logger logger) {
        this.plugin = plugin;
        this.repos = repos;
        this.config = config;
        this.logger = logger;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Repositories getRepos() {
        return repos;
    }

    public FactionsConfig getConfig() {
        return config;
    }

    public Logger getLogger() {
        return logger;
    }

    // -------------------------------------------------------------------------
    // FactionService implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean isInFaction(final UUID playerUUID) {
        try {
            final Optional<PlayerModel> pm = repos.players().find(playerUUID.toString());
            return pm.isPresent() && pm.get().isInFaction();
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to check isInFaction for " + playerUUID, e);
            return false;
        }
    }

    @Override
    public Optional<FactionModel> getFactionByPlayer(final UUID playerUUID) {
        try {
            final Optional<PlayerModel> pm = repos.players().find(playerUUID.toString());
            if (pm.isEmpty() || !pm.get().isInFaction()) {
                return Optional.empty();
            }
            return repos.factions().find(pm.get().getFactionId());
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to get faction for player " + playerUUID, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<FactionModel> getFactionByName(final String name) {
        try {
            return repos.factions().findByName(name);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to get faction by name '" + name + "'", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<FactionModel> getFactionById(final String id) {
        try {
            return repos.factions().find(id);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to get faction by id " + id, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<FactionModel> createFaction(final String name, final UUID ownerUUID) {
        try {
            if (repos.factions().findByName(name).isPresent()) {
                return Optional.empty();  // name taken — caller shows "already taken" message
            }

            final String factionId = UUID.randomUUID().toString();
            final String ownerId = ownerUUID.toString();

            final String memberRankId = UUID.randomUUID().toString();
            final String officerRankId = UUID.randomUUID().toString();
            final String ownerRankId = UUID.randomUUID().toString();

            repos.factions().transaction(() -> {
                final FactionModel faction = new FactionModel(factionId);
                faction.setName(name);
                faction.setOwnerId(ownerId);
                faction.setCreatedAt(System.currentTimeMillis());
                faction.setPowerBoost(0.0);
                faction.setMoney(0.0);
                repos.factions().save(faction);

                final RankModel memberRank = new RankModel(memberRankId);
                memberRank.setFactionId(factionId);
                memberRank.setName(RankModel.RANK_MEMBER);
                memberRank.setPriority(RankModel.PRIORITY_MEMBER);
                repos.ranks().save(memberRank);

                final RankModel officerRank = new RankModel(officerRankId);
                officerRank.setFactionId(factionId);
                officerRank.setName(RankModel.RANK_OFFICER);
                officerRank.setPriority(RankModel.PRIORITY_OFFICER);
                repos.ranks().save(officerRank);

                final RankModel ownerRank = new RankModel(ownerRankId);
                ownerRank.setFactionId(factionId);
                ownerRank.setName(RankModel.RANK_OWNER);
                ownerRank.setPriority(RankModel.PRIORITY_OWNER);
                repos.ranks().save(ownerRank);

                final PlayerModel owner = repos.players().findOrCreate(ownerId);
                owner.setFactionId(factionId);
                owner.setRankId(ownerRankId);
                owner.setJoinedAt(System.currentTimeMillis());
                owner.setPower(0.0);
                owner.setPowerBoost(0.0);
                owner.setLastActivity(0L);
                owner.setOverriding(false);
                owner.setTerritoryTitles(true);
                owner.setTitle(owner.getTitle() == null ? "" : owner.getTitle());
                owner.setAutoTerritoryMode(owner.getAutoTerritoryMode());
                repos.players().save(owner);
            });

            final FactionModel faction = repos.factions().find(factionId).orElseThrow();
            Bukkit.getPluginManager().callEvent(new FactionCreateEvent(faction, ownerUUID));
            return Optional.of(faction);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to create faction '" + name + "'", e);
            throw new IllegalStateException("Database error while creating faction", e);
        }
    }

    @Override
    public boolean disbandFaction(final String factionId) {
        try {
            final Optional<FactionModel> opt = repos.factions().find(factionId);
            if (opt.isEmpty()) {
                return false;
            }

            Bukkit.getPluginManager().callEvent(new FactionDisbandEvent(opt.get()));

            repos.factions().transaction(() -> {
                repos.board().deleteByFactionId(factionId);
                repos.warps().deleteByFactionId(factionId);
                repos.invitations().deleteByFactionId(factionId);
                repos.ranks().deleteByFactionId(factionId);
                repos.players().clearFactionMembers(factionId);
                repos.factions().delete(factionId);
            });

            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to disband faction " + factionId, e);
            return false;
        }
    }

    @Override
    public boolean removeMember(final String factionId, final UUID playerUUID) {
        try {
            final Optional<FactionModel> faction = repos.factions().find(factionId);
            if (faction.isEmpty()) {
                return false;
            }
            final String playerId = playerUUID.toString();
            final Optional<PlayerModel> pm = repos.players().find(playerId);
            if (pm.isEmpty() || !factionId.equals(pm.get().getFactionId())) {
                return false;
            }

            pm.get().setFactionId(null);
            pm.get().setRankId(null);
            repos.players().save(pm.get());

            Bukkit.getPluginManager().callEvent(new FactionLeaveEvent(faction.get(), playerUUID));
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to remove member " + playerUUID + " from " + factionId, e);
            return false;
        }
    }

    @Override
    public Collection<FactionModel> getAllFactions() {
        try {
            return new ArrayList<>(repos.factions().findAll());
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to list all factions", e);
            return List.of();
        }
    }

    @Override
    public Optional<RankModel> getRankByPlayer(final UUID playerUUID) {
        try {
            final Optional<PlayerModel> pm = repos.players().find(playerUUID.toString());
            if (pm.isEmpty() || pm.get().getRankId() == null) {
                return Optional.empty();
            }
            return repos.ranks().find(pm.get().getRankId());
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to get rank for player " + playerUUID, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean isOwner(final UUID playerUUID) {
        return getRankByPlayer(playerUUID).map(RankModel::isOwner).orElse(false);
    }

    @Override
    public boolean isOfficerOrAbove(final UUID playerUUID) {
        return getRankByPlayer(playerUUID).map(RankModel::isOfficerOrAbove).orElse(false);
    }

    @Override
    public boolean kickMember(final UUID actorUUID, final UUID targetUUID) {
        try {
            final Optional<PlayerModel> actorPm = repos.players().find(actorUUID.toString());
            final Optional<PlayerModel> targetPm = repos.players().find(targetUUID.toString());
            if (actorPm.isEmpty() || targetPm.isEmpty()) {
                return false;
            }
            final String actorFaction = actorPm.get().getFactionId();
            final String targetFaction = targetPm.get().getFactionId();
            if (actorFaction == null || !actorFaction.equals(targetFaction)) {
                return false;
            }

            final Optional<RankModel> actorRank = getRankByPlayer(actorUUID);
            final Optional<RankModel> targetRank = getRankByPlayer(targetUUID);
            if (actorRank.isEmpty() || targetRank.isEmpty()) {
                return false;
            }
            if (!actorRank.get().canManage(targetRank.get())) {
                return false;
            }
            targetPm.get().setFactionId(null);
            targetPm.get().setRankId(null);
            repos.players().save(targetPm.get());
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to kick " + targetUUID + " by " + actorUUID, e);
            return false;
        }
    }

    @Override
    public boolean setFactionHome(final UUID actorUUID, final Location location) {
        final Optional<FactionModel> factionOpt = getFactionByPlayer(actorUUID);
        if (factionOpt.isEmpty()) {
            return false;
        }
        final FactionModel faction = factionOpt.get();
        faction.setHomeWorld(location.getWorld().getName());
        faction.setHomeX(location.getX());
        faction.setHomeY(location.getY());
        faction.setHomeZ(location.getZ());
        faction.setHomeYaw(location.getYaw());
        faction.setHomePitch(location.getPitch());
        try {
            repos.factions().save(faction);
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to set faction home for " + actorUUID, e);
            return false;
        }
    }

    @Override
    public Optional<Location> getFactionHome(final UUID actorUUID) {
        final Optional<FactionModel> factionOpt = getFactionByPlayer(actorUUID);
        if (factionOpt.isEmpty() || !factionOpt.get().hasHome()) {
            return Optional.empty();
        }
        final FactionModel faction = factionOpt.get();
        final World world = Bukkit.getWorld(faction.getHomeWorld());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(
            world,
            faction.getHomeX(),
            faction.getHomeY(),
            faction.getHomeZ(),
            faction.getHomeYaw(),
            faction.getHomePitch()));
    }

    @Override
    public Optional<Relation> setRelation(
            final UUID actorUUID, final String targetFactionName, final Relation relation) {
        try {
            final Optional<FactionModel> sourceOpt = getFactionByPlayer(actorUUID);
            final Optional<FactionModel> targetOpt = repos.factions().findByName(targetFactionName);
            if (sourceOpt.isEmpty() || targetOpt.isEmpty()) {
                return Optional.empty();
            }
            final FactionModel source = sourceOpt.get();
            final FactionModel target = targetOpt.get();
            if (source.getId().equals(target.getId())) {
                return Optional.empty();
            }
            final Map<String, Relation> map = parseRelations(source.getRelationsJson());
            map.put(target.getId(), relation);
            source.setRelationsJson(serializeRelations(map));
            repos.factions().save(source);
            return Optional.of(relation);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to set relation for " + actorUUID, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean setFactionFlyEnabled(final UUID playerUUID, final boolean enabled) {
        flyStateByPlayer.put(playerUUID, enabled);
        return true;
    }

    @Override
    public boolean isFactionFlyEnabled(final UUID playerUUID) {
        return flyStateByPlayer.getOrDefault(playerUUID, false);
    }

    @Override
    public boolean renameFaction(final UUID actorUUID, final String newName) {
        try {
            final Optional<FactionModel> factionOpt = getFactionByPlayer(actorUUID);
            if (factionOpt.isEmpty()) {
                return false;
            }
            final Optional<FactionModel> byName = repos.factions().findByName(newName);
            if (byName.isPresent() && !byName.get().getId().equals(factionOpt.get().getId())) {
                return false;
            }
            final FactionModel faction = factionOpt.get();
            faction.setName(newName);
            repos.factions().save(faction);
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to rename faction for actor " + actorUUID, e);
            return false;
        }
    }

    @Override
    public boolean setFactionDescription(final UUID actorUUID, final String description) {
        try {
            final Optional<FactionModel> factionOpt = getFactionByPlayer(actorUUID);
            if (factionOpt.isEmpty()) {
                return false;
            }
            final FactionModel faction = factionOpt.get();
            faction.setDescription(description);
            repos.factions().save(faction);
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to set faction description for actor " + actorUUID, e);
            return false;
        }
    }

    @Override
    public boolean unsetFactionHome(final UUID actorUUID) {
        try {
            final Optional<FactionModel> factionOpt = getFactionByPlayer(actorUUID);
            if (factionOpt.isEmpty()) {
                return false;
            }
            final FactionModel faction = factionOpt.get();
            faction.setHomeWorld(null);
            faction.setHomeX(0.0);
            faction.setHomeY(64.0);
            faction.setHomeZ(0.0);
            faction.setHomeYaw(0.0f);
            faction.setHomePitch(0.0f);
            repos.factions().save(faction);
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to unset faction home for actor " + actorUUID, e);
            return false;
        }
    }

    @Override
    public boolean transferOwnership(final UUID ownerUUID, final UUID newOwnerUUID) {
        try {
            final Optional<PlayerModel> ownerPm = repos.players().find(ownerUUID.toString());
            final Optional<PlayerModel> targetPm = repos.players().find(newOwnerUUID.toString());
            if (ownerPm.isEmpty() || targetPm.isEmpty()) {
                return false;
            }
            final String factionId = ownerPm.get().getFactionId();
            if (factionId == null || !factionId.equals(targetPm.get().getFactionId())) {
                return false;
            }
            final Optional<RankModel> ownerRank = getRankByPlayer(ownerUUID);
            if (ownerRank.isEmpty() || !ownerRank.get().isOwner()) {
                return false;
            }
            final Optional<RankModel> newOwnerRank = repos.ranks().findOwnerRank(factionId);
            final Optional<RankModel> officerRank = repos.ranks().findByFactionId(factionId).stream()
                .filter(r -> RankModel.RANK_OFFICER.equals(r.getName()))
                .findFirst();
            if (newOwnerRank.isEmpty() || officerRank.isEmpty()) {
                return false;
            }
            repos.factions().transaction(() -> {
                ownerPm.get().setRankId(officerRank.get().getId());
                targetPm.get().setRankId(newOwnerRank.get().getId());
                repos.players().save(ownerPm.get());
                repos.players().save(targetPm.get());
                final Optional<FactionModel> faction = repos.factions().find(factionId);
                if (faction.isPresent()) {
                    faction.get().setOwnerId(newOwnerUUID.toString());
                    repos.factions().save(faction.get());
                }
            });
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to transfer ownership from " + ownerUUID + " to " + newOwnerUUID, e);
            return false;
        }
    }

    @Override
    public boolean promoteMember(final UUID actorUUID, final UUID targetUUID) {
        return changeMemberRank(actorUUID, targetUUID, true);
    }

    @Override
    public boolean demoteMember(final UUID actorUUID, final UUID targetUUID) {
        return changeMemberRank(actorUUID, targetUUID, false);
    }

    // -------------------------------------------------------------------------
    // Package-level helper used by adapters
    // -------------------------------------------------------------------------

    /**
     * Add {@code playerUUID} to {@code factionId} using the faction's default
     * member rank. Fires {@link FactionJoinEvent}.
     *
     * <p>This method is also used by {@link InviteServiceImpl} after accepting
     * an invite.
     *
     * @return {@code true} if the player was added successfully.
     */
    public boolean joinFaction(final String factionId, final UUID playerUUID) {
        try {
            final Optional<FactionModel> faction = repos.factions().find(factionId);
            if (faction.isEmpty()) {
                return false;
            }
            final Optional<RankModel> defaultRank = repos.ranks().findDefaultRank(factionId);
            if (defaultRank.isEmpty()) {
                return false;
            }
            final String playerId = playerUUID.toString();

            repos.factions().transaction(() -> {
                final PlayerModel pm = repos.players().findOrCreate(playerId);
                pm.setFactionId(factionId);
                pm.setRankId(defaultRank.get().getId());
                pm.setJoinedAt(System.currentTimeMillis());
                repos.players().save(pm);
                repos.invitations().deleteByInviteeId(playerId);
            });

            Bukkit.getPluginManager().callEvent(new FactionJoinEvent(faction.get(), playerUUID));
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to add member " + playerUUID + " to " + factionId, e);
            return false;
        }
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
        final String[] entries = body.split(",");
        for (final String rawEntry : entries) {
            final String[] kv = rawEntry.split(":", 2);
            if (kv.length != 2) {
                continue;
            }
            final String key = stripQuotes(kv[0].trim());
            final String value = stripQuotes(kv[1].trim());
            try {
                out.put(key, Relation.valueOf(value));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid serialized relation values.
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

    private String stripQuotes(final String value) {
        String out = value;
        if (out.startsWith("\"")) {
            out = out.substring(1);
        }
        if (out.endsWith("\"")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    private boolean changeMemberRank(final UUID actorUUID, final UUID targetUUID, final boolean promote) {
        try {
            final Optional<PlayerModel> actorPm = repos.players().find(actorUUID.toString());
            final Optional<PlayerModel> targetPm = repos.players().find(targetUUID.toString());
            if (actorPm.isEmpty() || targetPm.isEmpty()) {
                return false;
            }
            final String factionId = actorPm.get().getFactionId();
            if (factionId == null || !factionId.equals(targetPm.get().getFactionId())) {
                return false;
            }
            final Optional<RankModel> actorRankOpt = getRankByPlayer(actorUUID);
            final Optional<RankModel> targetRankOpt = getRankByPlayer(targetUUID);
            if (actorRankOpt.isEmpty() || targetRankOpt.isEmpty()) {
                return false;
            }
            final RankModel actorRank = actorRankOpt.get();
            final RankModel targetRank = targetRankOpt.get();
            if (!actorRank.canManage(targetRank)) {
                return false;
            }
            final List<RankModel> ranks = repos.ranks().findByFactionId(factionId);
            final int idx = ranks.stream().map(RankModel::getId).toList().indexOf(targetRank.getId());
            if (idx < 0) {
                return false;
            }
            final int nextIdx = promote ? idx - 1 : idx + 1;
            if (nextIdx < 0 || nextIdx >= ranks.size()) {
                return false;
            }
            final RankModel newRank = ranks.get(nextIdx);
            if (promote && newRank.isOwner()) {
                return false;
            }
            if (!actorRank.canManage(newRank)) {
                return false;
            }
            targetPm.get().setRankId(newRank.getId());
            repos.players().save(targetPm.get());
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to change rank for " + targetUUID, e);
            return false;
        }
    }
}

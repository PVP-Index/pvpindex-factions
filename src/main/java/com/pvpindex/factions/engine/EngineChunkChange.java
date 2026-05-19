package com.pvpindex.factions.engine;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.Relation;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.event.FactionChunkClaimEvent;
import com.pvpindex.factions.event.FactionChunkUnclaimEvent;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

/**
 * Handles chunk claiming and unclaiming logic with all known bug fixes applied:
 *
 * <ol>
 *   <li>Stale power check — uses real-time DB count, never a cache.</li>
 *   <li>Border logic — allows claim if any neighbor is unclaimed, own faction, or non-enemy.
 *       Rejects if ALL neighbors are enemy territory.</li>
 *   <li>NPE after disband — re-fetches faction inside the per-chunk lock.</li>
 *   <li>Concurrent-claim race — per-chunk lock via {@link ConcurrentHashMap}.</li>
 *   <li>Stale max-land cache — always computed fresh from DB.</li>
 * </ol>
 */
public class EngineChunkChange {

    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;

    /** Guards concurrent claim attempts for the same chunk. */
    private final ConcurrentHashMap<String, Object> claimLocks = new ConcurrentHashMap<>();

    public EngineChunkChange(
            final Repositories repos,
            final FactionsConfig config,
            final Logger logger) {
        this.repos = repos;
        this.config = config;
        this.logger = logger;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Attempt to claim a chunk for the faction the given player belongs to.
     *
     * @param player the player issuing the claim
     * @param chunk  the chunk to claim
     * @return {@code true} if the claim succeeded
     */
    public boolean claim(final Player player, final Chunk chunk) {
        final String chunkKey = buildKey(chunk);
        final Object lock = claimLocks.computeIfAbsent(chunkKey, k -> new Object());
        synchronized (lock) {
            try {
                // 1. Resolve player model
                final Optional<PlayerModel> pmOpt = repos.players().find(player.getUniqueId().toString());
                if (pmOpt.isEmpty() || !pmOpt.get().isInFaction()) {
                    MsgUtil.send(player, "<red>You are not in a faction.");
                    return false;
                }
                final String factionId = pmOpt.get().getFactionId();

                // 2. Re-fetch faction (Bug fix #3 — disband during claim)
                final Optional<FactionModel> factionOpt = repos.factions().find(factionId);
                if (factionOpt.isEmpty()) {
                    MsgUtil.send(player, "<red>Your faction no longer exists.");
                    return false;
                }
                final FactionModel faction = factionOpt.get();

                // 3. Check chunk is not already claimed — or attempt an overclaim
                final Optional<BoardEntry> existing = repos.board().findByChunk(
                    chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                FactionModel victimFaction = null;
                if (existing.isPresent()) {
                    final String existingFactionId = existing.get().getFactionId();
                    if (!config.isOverclaimingEnabled()) {
                        MsgUtil.send(player, "<red>This chunk is already claimed.");
                        return false;
                    }
                    if (FactionModel.SAFEZONE_ID.equals(existingFactionId)
                            || FactionModel.WARZONE_ID.equals(existingFactionId)) {
                        MsgUtil.send(player, "<red>This chunk is already claimed.");
                        return false;
                    }
                    if (existingFactionId.equals(factionId)) {
                        MsgUtil.send(player, "<red>This chunk is already claimed by your faction.");
                        return false;
                    }
                    if (config.isOverclaimRequireEnemyRelation()
                            && getRelation(faction, existingFactionId) != Relation.ENEMY) {
                        MsgUtil.send(player,
                            "<red>You can only overclaim land from factions you are at war with.");
                        return false;
                    }
                    final Optional<FactionModel> victimOpt = repos.factions().find(existingFactionId);
                    if (victimOpt.isPresent()) {
                        final FactionModel candidate = victimOpt.get();
                        final int victimLand = repos.board().countByFactionId(existingFactionId);
                        final int victimMaxLand = computeMaxLand(candidate, existingFactionId);
                        if (victimLand <= victimMaxLand) {
                            MsgUtil.send(player, MsgUtil.replace(
                                MsgUtil.message("claim.enemy-not-raidable",
                                    "<red>{faction} still has enough power"
                                    + " — you cannot overclaim their land yet."),
                                "faction", candidate.getName()));
                            return false;
                        }
                        // F5: Offline protection — block overclaim while all defenders are offline
                        if (config.isOfflineProtectionEnabled()) {
                            final boolean hasOnline = repos.players()
                                    .findByFactionId(existingFactionId).stream()
                                    .anyMatch(m -> {
                                        try {
                                            return Bukkit.getPlayer(
                                                    java.util.UUID.fromString(m.getId())) != null;
                                        } catch (IllegalArgumentException ignored) {
                                            return false;
                                        }
                                    });
                            if (!hasOnline) {
                                MsgUtil.send(player, MsgUtil.replace(
                                    MsgUtil.message("claim.enemy-offline-protected",
                                        "<red>{faction} is offline — you cannot overclaim"
                                        + " their land while all members are offline."),
                                    "faction", candidate.getName()));
                                return false;
                            }
                        }
                        // F6: War shield — block overclaim during the faction's protection window
                        if (config.isWarShieldEnabled() && candidate.isShieldActive()) {
                            MsgUtil.send(player, MsgUtil.replace(
                                MsgUtil.message("claim.shield-active",
                                    "<red>{faction} has an active war shield"
                                    + " — their territory is protected right now."),
                                "faction", candidate.getName()));
                            return false;
                        }
                        victimFaction = candidate;
                    }
                    // victimFaction null means victim row is gone — allow the claim to clean it up
                }

                // 4. Power / land check (Bug fixes #1 and #5 — real-time, no cache)
                final int currentLand = repos.board().countByFactionId(factionId);
                final int maxLand = computeMaxLand(faction, factionId);
                if (currentLand >= maxLand) {
                    MsgUtil.send(player, "<red>Your faction does not have enough power to claim more land.");
                    return false;
                }

                // 5. Border check (Bug fix #2 — correct adjacency logic)
                // Skipped when overclaiming — any underpowered enemy chunk is fair game.
                if (victimFaction == null && !isValidBorder(chunk, factionId, faction)) {
                    MsgUtil.send(player, "<red>You may only claim land that borders your own territory or wilderness.");
                    return false;
                }

                // 6. Fire cancellable plugin event
                final FactionChunkClaimEvent event = new FactionChunkClaimEvent(
                    faction, player.getUniqueId(),
                    chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), victimFaction);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return false;
                }

                // 7. Persist
                repos.board().claimChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), factionId);

                // Notify parties on overclaim
                if (victimFaction != null) {
                    MsgUtil.send(player, MsgUtil.replace(
                        MsgUtil.message("claim.overclaimed",
                            "<green>You overclaimed a chunk from <yellow>{faction}<green>!"),
                        "faction", victimFaction.getName()));
                    final String victimFactionId = victimFaction.getId();
                    final int remaining = repos.board().countByFactionId(victimFactionId);
                    final String victimMsg = MsgUtil.replace(
                        MsgUtil.replace(
                            MsgUtil.message("claim.overclaimed-victim",
                                "<red><yellow>{attacker}<red> overclaimed a chunk from your territory!"
                                + " <yellow>{remaining}<red> chunk(s) remain."),
                            "attacker", faction.getName()),
                        "remaining", String.valueOf(remaining));
                    final List<PlayerModel> victimMembers =
                        repos.players().findByFactionId(victimFactionId);
                    for (final PlayerModel memberPm : victimMembers) {
                        final Player memberPlayer = Bukkit.getPlayer(
                            java.util.UUID.fromString(memberPm.getId()));
                        if (memberPlayer != null) {
                            MsgUtil.send(memberPlayer, victimMsg);
                        }
                    }
                }
                return true;
            } catch (StorageException e) {
                logger.log(Level.SEVERE, "Failed to claim chunk " + chunkKey, e);
                MsgUtil.send(player, "<red>An internal error occurred. Please try again.");
                return false;
            } finally {
                claimLocks.remove(chunkKey);
            }
        }
    }

    /**
     * Attempt to unclaim a chunk on behalf of a player.
     *
     * @param player the player issuing the unclaim
     * @param chunk  the chunk to unclaim
     * @return {@code true} if the unclaim succeeded
     */
    public boolean unclaim(final Player player, final Chunk chunk) {
        final String chunkKey = buildKey(chunk);
        final Object lock = claimLocks.computeIfAbsent(chunkKey, k -> new Object());
        synchronized (lock) {
            try {
                final Optional<PlayerModel> pmOpt = repos.players().find(player.getUniqueId().toString());
                if (pmOpt.isEmpty() || !pmOpt.get().isInFaction()) {
                    MsgUtil.send(player, "<red>You are not in a faction.");
                    return false;
                }
                final String factionId = pmOpt.get().getFactionId();

                final Optional<BoardEntry> entry = repos.board().findByChunk(
                    chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                if (entry.isEmpty()) {
                    MsgUtil.send(player, "<red>This chunk is not claimed.");
                    return false;
                }
                if (!factionId.equals(entry.get().getFactionId())) {
                    MsgUtil.send(player, "<red>You can only unclaim your own faction's territory.");
                    return false;
                }

                final Optional<FactionModel> factionOpt = repos.factions().find(factionId);
                if (factionOpt.isEmpty()) {
                    // Faction gone — clean up the claim anyway
                    repos.board().unclaimChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                    return true;
                }

                final FactionChunkUnclaimEvent event = new FactionChunkUnclaimEvent(
                    factionOpt.get(), player.getUniqueId(),
                    chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return false;
                }

                repos.board().unclaimChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                return true;
            } catch (StorageException e) {
                logger.log(Level.SEVERE, "Failed to unclaim chunk " + chunkKey, e);
                MsgUtil.send(player, "<red>An internal error occurred. Please try again.");
                return false;
            } finally {
                claimLocks.remove(chunkKey);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Max land = total faction power (sum of member power + faction power boost) / land-per-power ratio.
     *
     * <p>Bug fix #1 and #5: always computed real-time from the DB, never cached.</p>
     *
     * <p>F1 — inactive exclusion: members offline longer than the configured threshold do
     * not contribute to the total, discouraging dead factions from holding territory.</p>
     */
    private int computeMaxLand(final FactionModel faction, final String factionId)
            throws StorageException {
        double totalPower = faction.getPowerBoost();
        final boolean inactiveExclude = config.isPowerInactiveExclusionEnabled();
        final long inactiveMs = config.getPowerInactiveDays() * 24L * 3600L * 1000L;
        final long now = System.currentTimeMillis();
        for (final PlayerModel pm : repos.players().findByFactionId(factionId)) {
            if (inactiveExclude) {
                final long last = pm.getLastActivity();
                if (last > 0 && now - last > inactiveMs) {
                    continue; // member too inactive — skip their power
                }
            }
            totalPower += pm.getPower();
        }
        final double landPerPower = config.getLandPerPower();
        if (landPerPower <= 0) {
            return config.getMaxLand();
        }
        return Math.min(config.getMaxLand(), (int) (totalPower / landPerPower));
    }

    /**
     * Check that the chunk borders acceptable territory.
     *
     * <p>Bug fix #2: A claim is valid if at least one of the 4 cardinal neighbors is:
     * <ul>
     *   <li>unclaimed (wilderness), or</li>
     *   <li>owned by the same faction, or</li>
     *   <li>owned by a faction that is not {@link Relation#ENEMY}.</li>
     * </ul>
     * A claim is invalid only when ALL neighbors are enemy territory.
     */
    private boolean isValidBorder(
            final Chunk chunk, final String factionId, final FactionModel faction)
            throws StorageException {
        // First claim by this faction is always valid (no existing land yet)
        if (repos.board().countByFactionId(factionId) == 0) {
            return true;
        }

        final int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        final String worldName = chunk.getWorld().getName();
        for (final int[] offset : offsets) {
            final Optional<BoardEntry> neighbor = repos.board().findByChunk(
                worldName, chunk.getX() + offset[0], chunk.getZ() + offset[1]);
            if (neighbor.isEmpty()) {
                // Wilderness border — valid
                return true;
            }
            final String neighborFactionId = neighbor.get().getFactionId();
            if (factionId.equals(neighborFactionId)) {
                // Own territory — valid
                return true;
            }
            // Check relation to neighbor faction
            final Relation rel = getRelation(faction, neighborFactionId);
            if (rel != Relation.ENEMY) {
                return true;
            }
        }
        // All neighbors are enemy territory — not valid
        return false;
    }

    /** Determine the {@link Relation} between two factions. */
    private Relation getRelation(final FactionModel faction, final String otherFactionId)
            throws StorageException {
        if (FactionModel.SAFEZONE_ID.equals(otherFactionId)
                || FactionModel.WARZONE_ID.equals(otherFactionId)) {
            return Relation.NEUTRAL;
        }
        final Optional<FactionModel> other = repos.factions().find(otherFactionId);
        if (other.isEmpty()) {
            return Relation.NEUTRAL;
        }
        final String relationJson = faction.getRelationsJson();
        if (relationJson == null) {
            return Relation.NEUTRAL;
        }
        // Simple lookup in the relations map stored as JSON {"<id>":"ALLY",...}
        // We use a lightweight parse rather than a full Gson dependency inside the engine.
        final String token = "\"" + otherFactionId + "\":\"";
        final int start = relationJson.indexOf(token);
        if (start < 0) {
            return Relation.NEUTRAL;
        }
        final int valueStart = start + token.length();
        final int valueEnd = relationJson.indexOf('"', valueStart);
        if (valueEnd < 0) {
            return Relation.NEUTRAL;
        }
        try {
            return Relation.valueOf(relationJson.substring(valueStart, valueEnd));
        } catch (IllegalArgumentException e) {
            return Relation.NEUTRAL;
        }
    }

    private static String buildKey(final Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }
}

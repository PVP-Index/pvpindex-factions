package com.pvpindex.factions.service;

import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.Relation;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;

/**
 * Internal faction service interface — no TeamsAPI dependency.
 *
 * <p>Commands and engines depend on this interface so that the plugin can
 * function without TeamsAPI present.  When TeamsAPI is available, the
 * {@code api} package adapters implement the TeamsAPI interfaces on top of
 * an implementation of this interface.
 */
public interface FactionService {

    /** @return {@code true} if the player belongs to any faction. */
    boolean isInFaction(UUID playerUUID);

    /** @return the faction the player belongs to, or empty. */
    Optional<FactionModel> getFactionByPlayer(UUID playerUUID);

    /** @return the faction with the given name (case-sensitive), or empty. */
    Optional<FactionModel> getFactionByName(String name);

    /** @return the faction with the given string ID, or empty. */
    Optional<FactionModel> getFactionById(String id);

    /**
     * Create a new faction owned by {@code ownerUUID}.
     *
     * @return the created faction, or empty if the name is taken.
     */
    Optional<FactionModel> createFaction(String name, UUID ownerUUID);

    /**
     * Disband the faction identified by {@code factionId}, removing all
     * associated data (members, warps, claims, invitations, ranks).
     *
     * @return {@code true} if the faction was found and disbanded.
     */
    boolean disbandFaction(String factionId);

    /**
     * Remove {@code playerUUID} from {@code factionId}.
     *
     * @return {@code true} if the player was a member and has been removed.
     */
    boolean removeMember(String factionId, UUID playerUUID);

    /** @return all factions currently stored. */
    Collection<FactionModel> getAllFactions();

    /** @return player's rank model when they are in a faction and rank exists. */
    Optional<RankModel> getRankByPlayer(UUID playerUUID);

    /** @return true when player is owner rank in their faction. */
    boolean isOwner(UUID playerUUID);

    /** @return true when player is officer or owner in their faction. */
    boolean isOfficerOrAbove(UUID playerUUID);

    /**
     * Kick {@code targetUUID} from actor's faction.
     *
     * @return true when target was removed.
     */
    boolean kickMember(UUID actorUUID, UUID targetUUID);

    /** Set actor faction home to given location. */
    boolean setFactionHome(UUID actorUUID, Location location);

    /** Read actor faction home location. */
    Optional<Location> getFactionHome(UUID actorUUID);

    /** Set relation from actor's faction to target faction. */
    Optional<Relation> setRelation(UUID actorUUID, String targetFactionName, Relation relation);

    /** Toggle faction fly preference for a player. */
    boolean setFactionFlyEnabled(UUID playerUUID, boolean enabled);

    /** Query current faction fly preference for a player. */
    boolean isFactionFlyEnabled(UUID playerUUID);

    /** Rename actor's faction. */
    boolean renameFaction(UUID actorUUID, String newName);

    /** Update actor faction description text. */
    boolean setFactionDescription(UUID actorUUID, String description);

    /** Update the MOTD for the actor's faction. Pass empty string to clear. */
    boolean setFactionMotd(UUID actorUUID, String motd);

    /**
     * @return {@code true} if the faction's member count is at or above the configured
     *         {@code factions.max-members} limit. Always {@code false} when the limit is 0.
     */
    boolean isFactionFull(String factionId);

    /** Clear actor faction home. */
    boolean unsetFactionHome(UUID actorUUID);

    /** Transfer faction ownership to another member. */
    boolean transferOwnership(UUID ownerUUID, UUID newOwnerUUID);

    /** Promote a member one rank step upward (except to owner). */
    boolean promoteMember(UUID actorUUID, UUID targetUUID);

    /** Demote a member one rank step downward (owner cannot be demoted). */
    boolean demoteMember(UUID actorUUID, UUID targetUUID);

    /** List all roles in actor's faction (sorted by priority descending). */
    List<RankModel> listRoles(UUID actorUUID);

    /** Create a custom role in actor's faction. */
    boolean createRole(UUID actorUUID, String name, int priority, String prefix);

    /** Rename a role in actor's faction by current name. */
    boolean renameRole(UUID actorUUID, String roleName, String newName);

    /** Set role priority for a role in actor's faction. */
    boolean setRolePriority(UUID actorUUID, String roleName, int priority);

    /** Set or clear role prefix for a role in actor's faction. */
    boolean setRolePrefix(UUID actorUUID, String roleName, String prefix);

    /** Delete a custom role in actor's faction. */
    boolean deleteRole(UUID actorUUID, String roleName);

    /** Assign a named role in actor's faction to a target member. */
    boolean assignRole(UUID actorUUID, UUID targetUUID, String roleName);

    /**
     * Directly add {@code playerUUID} to {@code factionId} without requiring a
     * pending invite. Used by the open-faction join path.
     *
     * @return {@code true} if the player was successfully added.
     */
    boolean joinFaction(String factionId, UUID playerUUID);

    /**
     * Merge {@code senderFactionId} into {@code targetFactionId}.
     *
     * <p>Transfers all claims, warps, and bank balance to the target faction, then
     * migrates every member of the sender faction to the target at default (MEMBER)
     * rank. Finally, removes the sender faction and its ranks/invitations/relations.</p>
     *
     * @return {@code true} if the merge completed successfully.
     */
    boolean mergeFaction(String senderFactionId, String targetFactionId, UUID actorUUID);
}

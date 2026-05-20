package com.pvpindex.factions.service;

import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.MergeRequestModel;
import java.util.List;
import java.util.UUID;

/**
 * Internal merge-request service — no TeamsAPI dependency.
 *
 * <p>A merge request is initiated by an officer+ of the sender faction and
 * must be explicitly accepted by an officer+ of the target faction. Accepting
 * the request delegates to {@link FactionService#mergeFaction} which transfers
 * all assets and members before disbanding the sender faction.</p>
 */
public interface MergeService {

    /**
     * Send a merge request from {@code senderFactionId} to {@code targetFactionId}.
     *
     * @param senderFactionId the faction that will be disbanded on acceptance
     * @param targetFactionId the faction that will absorb members and assets
     * @param actorUUID       the player initiating the request (must be officer+)
     * @return {@code true} if the request was stored successfully
     */
    boolean sendMergeRequest(String senderFactionId, String targetFactionId, UUID actorUUID);

    /**
     * Accept a pending merge request directed at {@code targetFactionId} from
     * {@code senderFactionId}, completing the merge.
     *
     * @param senderFactionId the faction whose request is being accepted
     * @param targetFactionId the accepting faction
     * @param actorUUID       the player accepting (must be officer+ of target)
     * @return the merged-into faction model on success, or empty on failure
     */
    java.util.Optional<FactionModel> acceptMergeRequest(
        String senderFactionId, String targetFactionId, UUID actorUUID);

    /**
     * List all pending merge requests sent by {@code factionId}.
     */
    List<MergeRequestModel> listRequestsBySender(String factionId);

    /**
     * List all pending merge requests targeting {@code factionId}.
     */
    List<MergeRequestModel> listRequestsForTarget(String factionId);
}

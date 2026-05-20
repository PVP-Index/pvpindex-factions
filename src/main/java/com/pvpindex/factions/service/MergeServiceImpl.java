package com.pvpindex.factions.service;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.MergeRequestModel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core merge-request business logic — no TeamsAPI dependency.
 */
public class MergeServiceImpl implements MergeService {

    private final FactionServiceImpl factionService;
    private final Repositories repos;
    private final Logger logger;

    public MergeServiceImpl(
            final FactionServiceImpl factionService,
            final Repositories repos,
            final Logger logger) {
        this.factionService = factionService;
        this.repos = repos;
        this.logger = logger;
    }

    // -------------------------------------------------------------------------
    // MergeService implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean sendMergeRequest(
            final String senderFactionId, final String targetFactionId, final UUID actorUUID) {
        try {
            final Optional<FactionModel> sender = repos.factions().find(senderFactionId);
            final Optional<FactionModel> target = repos.factions().find(targetFactionId);
            if (sender.isEmpty() || target.isEmpty()) {
                return false;
            }

            // Reject duplicate requests
            if (repos.mergeRequests().findBySenderAndTarget(senderFactionId, targetFactionId).isPresent()) {
                return false;
            }

            final MergeRequestModel request = new MergeRequestModel(UUID.randomUUID().toString());
            request.setSenderFactionId(senderFactionId);
            request.setTargetFactionId(targetFactionId);
            request.setActorId(actorUUID.toString());
            request.setCreatedAt(System.currentTimeMillis());
            repos.mergeRequests().save(request);
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to send merge request from " + senderFactionId
                + " to " + targetFactionId, e);
            return false;
        }
    }

    @Override
    public Optional<FactionModel> acceptMergeRequest(
            final String senderFactionId, final String targetFactionId, final UUID actorUUID) {
        try {
            if (repos.mergeRequests().findBySenderAndTarget(senderFactionId, targetFactionId).isEmpty()) {
                return Optional.empty();
            }

            final boolean merged = factionService.mergeFaction(senderFactionId, targetFactionId, actorUUID);
            if (!merged) {
                return Optional.empty();
            }

            // The sender faction has been deleted; return the target faction
            return repos.factions().find(targetFactionId);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to accept merge request from " + senderFactionId
                + " into " + targetFactionId, e);
            return Optional.empty();
        }
    }

    @Override
    public List<MergeRequestModel> listRequestsBySender(final String factionId) {
        try {
            return repos.mergeRequests().findBySenderFactionId(factionId);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to list merge requests for sender " + factionId, e);
            return List.of();
        }
    }

    @Override
    public List<MergeRequestModel> listRequestsForTarget(final String factionId) {
        try {
            return repos.mergeRequests().findByTargetFactionId(factionId);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to list merge requests for target " + factionId, e);
            return List.of();
        }
    }
}

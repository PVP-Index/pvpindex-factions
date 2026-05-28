package com.pvpindex.factions.service;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.TeamChestModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;

/**
 * Core team chest business logic.
 */
public class TeamChestServiceImpl implements TeamChestService {

    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;

    public TeamChestServiceImpl(final Repositories repos, final FactionsConfig config, final Logger logger) {
        this.repos = repos;
        this.config = config;
        this.logger = logger;
    }

    @Override
    public List<String> getChestNames(final String factionId) {
        try {
            return repos.teamChests().findByFactionId(factionId).stream()
                .map(TeamChestModel::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to list team chests for faction " + factionId, e);
            return List.of();
        }
    }

    @Override
    public boolean createChest(final String factionId, final String name) {
        final String normalized = normalizeName(name);
        if (normalized == null) {
            return false;
        }
        try {
            final List<TeamChestModel> existing = repos.teamChests().findByFactionId(factionId);
            final boolean duplicate = existing.stream().anyMatch(c -> c.getName().equalsIgnoreCase(normalized));
            if (duplicate || existing.size() >= config.getMaxTeamChests()) {
                return false;
            }
            final TeamChestModel chest = new TeamChestModel(UUID.randomUUID().toString());
            chest.setFactionId(factionId);
            chest.setName(normalized);
            chest.setCreatedAt(System.currentTimeMillis());
            chest.setContents(TeamChestSerialization.encode(List.of()));
            repos.teamChests().save(chest);
            return true;
        } catch (StorageException | IOException e) {
            logger.log(Level.SEVERE, "Failed to create team chest " + normalized + " for faction " + factionId, e);
            return false;
        }
    }

    @Override
    public boolean deleteChest(final String factionId, final String name) {
        final String normalized = normalizeName(name);
        if (normalized == null) {
            return false;
        }
        try {
            final Optional<TeamChestModel> model = repos.teamChests().findByFactionIdAndName(factionId, normalized);
            if (model.isEmpty()) {
                return false;
            }
            repos.teamChests().delete(model.get().getId());
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to delete team chest " + normalized + " for faction " + factionId, e);
            return false;
        }
    }

    @Override
    public Optional<List<ItemStack>> getChestContents(final String factionId, final String name) {
        final String normalized = normalizeName(name);
        if (normalized == null) {
            return Optional.empty();
        }
        try {
            final Optional<TeamChestModel> model = repos.teamChests().findByFactionIdAndName(factionId, normalized);
            if (model.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(TeamChestSerialization.decode(model.get().getContents()));
        } catch (StorageException | IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Failed to load team chest " + normalized + " for faction " + factionId, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean setChestContents(final String factionId, final String name, final List<ItemStack> contents) {
        final String normalized = normalizeName(name);
        if (normalized == null) {
            return false;
        }
        try {
            final Optional<TeamChestModel> model = repos.teamChests().findByFactionIdAndName(factionId, normalized);
            if (model.isEmpty()) {
                return false;
            }
            model.get().setContents(TeamChestSerialization.encode(contents == null ? List.of() : new ArrayList<>(contents)));
            repos.teamChests().save(model.get());
            return true;
        } catch (StorageException | IOException e) {
            logger.log(Level.SEVERE, "Failed to save team chest " + normalized + " for faction " + factionId, e);
            return false;
        }
    }

    @Override
    public Optional<String> ensureChestExistsForOpen(final String factionId, final String requestedName) {
        final String normalized = normalizeName(requestedName);
        if (normalized == null) {
            return Optional.empty();
        }
        try {
            final List<TeamChestModel> existing = repos.teamChests().findByFactionId(factionId);
            final Optional<TeamChestModel> match = existing.stream()
                .filter(chest -> chest.getName().equalsIgnoreCase(normalized))
                .findFirst();
            if (match.isPresent()) {
                return Optional.of(match.get().getName());
            }
            if (existing.size() >= config.getMaxTeamChests()) {
                return Optional.empty();
            }
            if (!createChest(factionId, normalized)) {
                return Optional.empty();
            }
            return Optional.of(normalized);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to ensure team chest " + normalized + " for faction " + factionId, e);
            return Optional.empty();
        }
    }

    private String normalizeName(final String raw) {
        if (raw == null) {
            return null;
        }
        final String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}

package com.pvpindex.factions.api;

import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.service.FactionServiceImpl;
import com.pvpindex.factions.service.TeamChestServiceImpl;
import com.skyblockexp.teamsapi.api.TeamsChestService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/**
 * Adapts internal team chest storage to TeamsAPI {@link TeamsChestService}.
 */
public final class FactionsTeamsChestService implements TeamsChestService {

    private final TeamChestServiceImpl impl;
    private final FactionServiceImpl factionImpl;

    public FactionsTeamsChestService(final TeamChestServiceImpl impl, final FactionServiceImpl factionImpl) {
        this.impl = impl;
        this.factionImpl = factionImpl;
    }

    @Override
    public Collection<String> getChestIds(final UUID teamId) {
        return resolveFactionId(teamId)
            .map(impl::getChestNames)
            .orElse(List.of());
    }

    @Override
    public Collection<ItemStack> getContents(final UUID teamId) {
        return getContents(teamId, DEFAULT_CHEST_ID);
    }

    @Override
    public Collection<ItemStack> getContents(final UUID teamId, final String chestId) {
        return resolveFactionId(teamId)
            .flatMap(factionId -> impl.getChestContents(factionId, chestId))
            .map(list -> (Collection<ItemStack>) list)
            .orElse(List.of());
    }

    @Override
    public boolean setContents(final UUID teamId, final String chestId, final Collection<ItemStack> contents) {
        return resolveFactionId(teamId)
            .map(factionId -> impl.setChestContents(factionId, chestId, List.copyOf(contents)))
            .orElse(false);
    }

    @Override
    public boolean addItem(final UUID teamId, final ItemStack itemStack) {
        return addItem(teamId, DEFAULT_CHEST_ID, itemStack);
    }

    @Override
    public boolean addItem(final UUID teamId, final String chestId, final ItemStack itemStack) {
        final Optional<String> factionIdOpt = resolveFactionId(teamId);
        if (factionIdOpt.isEmpty()) {
            return false;
        }
        final String factionId = factionIdOpt.get();
        final List<ItemStack> current = impl.getChestContents(factionId, chestId).orElse(null);
        if (current == null) {
            return false;
        }
        current.add(itemStack);
        return impl.setChestContents(factionId, chestId, current);
    }

    @Override
    public boolean removeItem(final UUID teamId, final ItemStack itemStack) {
        return removeItem(teamId, DEFAULT_CHEST_ID, itemStack);
    }

    @Override
    public boolean removeItem(final UUID teamId, final String chestId, final ItemStack itemStack) {
        final Optional<String> factionIdOpt = resolveFactionId(teamId);
        if (factionIdOpt.isEmpty()) {
            return false;
        }
        final String factionId = factionIdOpt.get();
        final List<ItemStack> current = impl.getChestContents(factionId, chestId).orElse(null);
        if (current == null) {
            return false;
        }
        final boolean removed = current.removeIf(item -> item != null && item.equals(itemStack));
        if (!removed) {
            return false;
        }
        return impl.setChestContents(factionId, chestId, current);
    }

    private Optional<String> resolveFactionId(final UUID teamId) {
        final Optional<FactionModel> faction = factionImpl.getFactionById(teamId.toString());
        return faction.map(FactionModel::getId);
    }
}

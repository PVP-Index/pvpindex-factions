package com.pvpindex.factions.service;

import java.util.List;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;

/**
 * Internal team chest service interface.
 */
public interface TeamChestService {

    List<String> getChestNames(String factionId);

    boolean createChest(String factionId, String name);

    boolean deleteChest(String factionId, String name);

    Optional<List<ItemStack>> getChestContents(String factionId, String name);

    boolean setChestContents(String factionId, String name, List<ItemStack> contents);

    Optional<String> ensureChestExistsForOpen(String factionId, String requestedName);
}

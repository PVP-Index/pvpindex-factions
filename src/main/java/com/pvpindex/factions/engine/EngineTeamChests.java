package com.pvpindex.factions.engine;

import com.pvpindex.factions.service.TeamChestService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Tracks open faction chest inventories and persists them on close.
 */
public class EngineTeamChests implements Listener {

    private static final int CHEST_SIZE = 54;

    private final TeamChestService teamChestService;
    private final Logger logger;
    private final Map<UUID, OpenChestSession> sessions = new ConcurrentHashMap<>();

    public EngineTeamChests(final TeamChestService teamChestService, final Logger logger) {
        this.teamChestService = teamChestService;
        this.logger = logger;
    }

    public void register(final Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public boolean openChest(final Player player, final String factionId, final String chestName, final String title) {
        final var contentsOpt = teamChestService.getChestContents(factionId, chestName);
        if (contentsOpt.isEmpty()) {
            return false;
        }
        final Inventory inventory = Bukkit.createInventory(player, CHEST_SIZE, title);
        final List<ItemStack> contents = contentsOpt.get();
        for (int i = 0; i < Math.min(contents.size(), CHEST_SIZE); i++) {
            inventory.setItem(i, contents.get(i));
        }
        sessions.put(player.getUniqueId(), new OpenChestSession(factionId, chestName));
        player.openInventory(inventory);
        return true;
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        final OpenChestSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        final List<ItemStack> items = new ArrayList<>(event.getInventory().getSize());
        for (final ItemStack item : event.getInventory().getContents()) {
            items.add(item);
        }
        if (!teamChestService.setChestContents(session.factionId(), session.chestName(), items)) {
            logger.warning("Failed to persist team chest " + session.chestName() + " for faction " + session.factionId());
        }
    }

    private record OpenChestSession(String factionId, String chestName) {
    }
}

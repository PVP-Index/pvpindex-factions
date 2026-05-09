package com.pvpindex.factions.gui;

import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.config.GuiConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.service.FactionService;
import com.pvpindex.factions.util.MsgUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/**
 * Handles /f GUI rendering and click actions.
 */
public class FactionsGuiManager implements Listener {

    private static final String ROOT = "gui.menus";
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final Plugin plugin;
    private final GuiConfig guiConfig;
    private final Repositories repos;
    private final FactionService factionService;
    private final FactionsConfig cfg;
    private final Logger logger;
    private final Map<UUID, String> openMenus = new ConcurrentHashMap<>();

    public FactionsGuiManager(
            final Plugin plugin,
            final GuiConfig guiConfig,
            final Repositories repos,
            final FactionService factionService,
            final FactionsConfig cfg,
            final Logger logger) {
        this.plugin = plugin;
        this.guiConfig = guiConfig;
        this.repos = repos;
        this.factionService = factionService;
        this.cfg = cfg;
        this.logger = logger;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public boolean openDefault(final Player player) {
        if (!guiConfig.isEnabled()) {
            return false;
        }
        return openMenu(player, guiConfig.getDefaultMenu());
    }

    public boolean openMenu(final Player player, final String menuId) {
        final ConfigurationSection section = guiConfig.raw().getConfigurationSection(ROOT + "." + menuId);
        if (section == null) {
            return false;
        }
        final int size = normalizeSize(section.getInt("size", 54));
        final String title = render(section.getString("title", "<gold>Factions"), player);
        final Inventory inventory = Bukkit.createInventory(new MenuHolder(menuId), size, MsgUtil.parse(title));
        final ConfigurationSection items = section.getConfigurationSection("items");
        if (items != null) {
            for (final String key : items.getKeys(false)) {
                final ConfigurationSection itemSection = items.getConfigurationSection(key);
                if (itemSection == null) {
                    continue;
                }
                final int slot = itemSection.getInt("slot", -1);
                if (slot < 0 || slot >= size) {
                    continue;
                }
                inventory.setItem(slot, buildItem(itemSection, player));
            }
        }
        openMenus.put(player.getUniqueId(), menuId);
        player.openInventory(inventory);
        return true;
    }

    @EventHandler
    public void onClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        final InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof MenuHolder menuHolder)) {
            return;
        }
        event.setCancelled(true);
        final ConfigurationSection menu = guiConfig.raw().getConfigurationSection(ROOT + "." + menuHolder.id());
        if (menu == null) {
            return;
        }
        final ConfigurationSection items = menu.getConfigurationSection("items");
        if (items == null) {
            return;
        }
        for (final String key : items.getKeys(false)) {
            final ConfigurationSection itemSection = items.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }
            if (itemSection.getInt("slot", -1) != event.getRawSlot()) {
                continue;
            }
            runAction(player, itemSection);
            return;
        }
    }

    @EventHandler
    public void onClose(final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            openMenus.remove(player.getUniqueId());
        }
    }

    private void runAction(final Player player, final ConfigurationSection section) {
        final String action = section.getString("action", "NONE").toUpperCase(Locale.ROOT);
        switch (action) {
            case "RUN_COMMAND" -> {
                final String command = render(section.getString("command", "f help"), player);
                player.closeInventory();
                player.performCommand(command.startsWith("/") ? command.substring(1) : command);
            }
            case "SUGGEST_COMMAND" -> {
                final String command = render(section.getString("command", "/f help"), player);
                player.closeInventory();
                player.sendMessage(MsgUtil.parse("<gray>Suggested: <yellow>" + command));
            }
            case "OPEN_MENU" -> {
                final String target = section.getString("menu", guiConfig.getDefaultMenu());
                if (!openMenu(player, target)) {
                    MsgUtil.send(player, "<red>Menu '" + target + "' is not configured.");
                }
            }
            case "CLOSE" -> player.closeInventory();
            case "REFRESH" -> openMenu(player, openMenus.getOrDefault(player.getUniqueId(), guiConfig.getDefaultMenu()));
            default -> {
                // no-op
            }
        }
    }

    private ItemStack buildItem(final ConfigurationSection section, final Player player) {
        final Material material = Material.matchMaterial(section.getString("material", "PAPER"));
        final ItemStack item = new ItemStack(material == null ? Material.PAPER : material);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MsgUtil.parse(render(section.getString("name", "<white>Factions"), player)));
            final List<String> loreRaw = section.getStringList("lore");
            if (!loreRaw.isEmpty()) {
                final List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                for (final String line : loreRaw) {
                    lore.add(MsgUtil.parse(render(line, player)));
                }
                meta.lore(lore);
            }
            if (section.getBoolean("glow", false)) {
                meta.setEnchantmentGlintOverride(true);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String render(final String input, final Player player) {
        try {
            final UUID uuid = player.getUniqueId();
            final Optional<FactionModel> factionOpt = factionService.getFactionByPlayer(uuid);
            final PlayerModel self = repos.players().find(uuid.toString()).orElse(null);
            final String factionName = factionOpt.map(FactionModel::getName).orElse("Wilderness");
            final String factionId = factionOpt.map(FactionModel::getId).orElse("");
            final int factionMembers = factionId.isEmpty() ? 0 : repos.players().findByFactionId(factionId).size();
            final int factionLand = factionId.isEmpty() ? 0 : repos.board().countByFactionId(factionId);
            final double factionBank = factionOpt.map(FactionModel::getBank).orElse(0.0D);
            final double power = self == null ? 0.0D : self.getPower();
            return input
                .replace("{player}", player.getName())
                .replace("{faction}", factionName)
                .replace("{faction_members}", Integer.toString(factionMembers))
                .replace("{faction_land}", Integer.toString(factionLand))
                .replace("{faction_bank}", String.format(Locale.US, "%.2f", factionBank))
                .replace("{power}", String.format(Locale.US, "%.2f", power))
                .replace("{max_power}", String.format(Locale.US, "%.2f", cfg.getMaxPower()));
        } catch (StorageException ex) {
            logger.warning("Failed to render faction GUI placeholders for " + player.getName() + ": " + ex.getMessage());
            return input.replace("{player}", player.getName())
                .replace("{faction}", "Wilderness")
                .replace("{faction_members}", "0")
                .replace("{faction_land}", "0")
                .replace("{faction_bank}", "0.00")
                .replace("{power}", "0.00")
                .replace("{max_power}", String.format(Locale.US, "%.2f", cfg.getMaxPower()));
        }
    }

    private int normalizeSize(final int configured) {
        final int capped = Math.max(9, Math.min(54, configured));
        final int rows = (int) Math.ceil(capped / 9.0D);
        return rows * 9;
    }

    private record MenuHolder(String id) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return Bukkit.createInventory(this, 9, PLAIN.deserialize("internal"));
        }
    }
}

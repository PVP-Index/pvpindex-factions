package com.gyvex.pvpindex.factions.engine;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.gyvex.pvpindex.factions.config.FactionsConfig;
import com.gyvex.pvpindex.factions.data.Repositories;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.data.model.PlayerModel;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

/**
 * Formats player chat messages with faction prefix and relation-coloured name.
 *
 * @deprecated {@link AsyncPlayerChatEvent} is deprecated in Paper 1.21; migrate to
 *             the new ChatEvent API when available.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class EngineChat implements Listener {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;

    public EngineChat(
            final Repositories repos, final FactionsConfig config, final Logger logger) {
        this.repos = repos;
        this.config = config;
        this.logger = logger;
    }

    public void register(final Plugin plugin) {
        org.bukkit.Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(final AsyncPlayerChatEvent event) {
        if (!config.isChatFormatEnabled()) {
            return;
        }
        try {
            final Optional<PlayerModel> pm = repos.players()
                .find(event.getPlayer().getUniqueId().toString());
            final String factionTag;
            if (pm.isPresent() && pm.get().isInFaction()) {
                final Optional<FactionModel> faction = repos.factions().find(pm.get().getFactionId());
                factionTag = faction.map(FactionModel::getName)
                    .map(name -> "<gray>[<white>" + name + "<gray>]</gray> ")
                    .orElse("");
            } else {
                factionTag = "";
            }
            event.setFormat(factionTag + "%s" + "<gray>: <white>%s");
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to format chat for " + event.getPlayer().getName(), e);
        }
    }
}

package com.pvpindex.factions.engine;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

/**
 * Formats player chat messages with faction prefix and relation-coloured name.
 *
 * <p>Registers a Paper {@code AsyncChatEvent} listener on Paper servers, and falls back to the
 * legacy {@code AsyncPlayerChatEvent} on Spigot.
 */
public final class EngineChat {

    private static final boolean PAPER_CHAT;

    static {
        boolean paperChat = false;
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            paperChat = true;
        } catch (ClassNotFoundException ignored) {
            // Running on Spigot — fall back to legacy handler.
        }
        PAPER_CHAT = paperChat;
    }

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
        if (PAPER_CHAT) {
            org.bukkit.Bukkit.getPluginManager().registerEvents(new PaperChatListener(), plugin);
        } else {
            org.bukkit.Bukkit.getPluginManager().registerEvents(new LegacyChatListener(), plugin);
        }
    }

    private String buildFactionTag(final Player player) throws StorageException {
        final Optional<PlayerModel> pm = repos.players().find(player.getUniqueId().toString());
        if (pm.isPresent() && pm.get().isInFaction()) {
            final Optional<FactionModel> faction = repos.factions().find(pm.get().getFactionId());
            return faction.map(FactionModel::getName)
                .map(name -> "<gray>[<white>" + name + "<gray>]</gray> ")
                .orElse("");
        }
        return "";
    }

    private final class PaperChatListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onChat(final io.papermc.paper.event.player.AsyncChatEvent event) {
            if (!config.isChatFormatEnabled()) {
                return;
            }
            try {
                final String tag = buildFactionTag(event.getPlayer());
                final MiniMessage mm = MiniMessage.miniMessage();
                final Component prefix = mm.deserialize(tag);
                final Component sep = mm.deserialize("<gray>: <white>");
                event.renderer((source, displayName, message, viewer) ->
                    prefix.append(displayName).append(sep).append(message));
            } catch (StorageException e) {
                logger.log(Level.WARNING, "Failed to format chat for " + event.getPlayer().getName(), e);
            }
        }
    }

    private final class LegacyChatListener implements Listener {

        @SuppressWarnings({"deprecation", "removal"})
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onChat(final AsyncPlayerChatEvent event) {
            if (!config.isChatFormatEnabled()) {
                return;
            }
            try {
                final String tag = buildFactionTag(event.getPlayer());
                event.setFormat(tag + "%s" + "<gray>: <white>%s");
            } catch (StorageException e) {
                logger.log(Level.WARNING, "Failed to format chat for " + event.getPlayer().getName(), e);
            }
        }
    }
}

package com.pvpindex.factions.engine;

import com.github.ezplugins.updater.ChainedUpdateResult;
import com.github.ezplugins.updater.UpdateResult;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.update.UpdateNotificationManager;
import com.pvpindex.factions.util.MsgUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

/**
 * Sends update notices to operators when they join.
 */
public final class EngineUpdateNotifier implements Listener {

    private final FactionsConfig config;
    private final UpdateNotificationManager updateManager;

    public EngineUpdateNotifier(final FactionsConfig config, final UpdateNotificationManager updateManager) {
        this.config = config;
        this.updateManager = updateManager;
    }

    public void register(final Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        if (!config.isUpdateCheckEnabled() || !config.isUpdateNotifyOpsOnJoin()) {
            return;
        }
        final Player player = event.getPlayer();
        if (!player.isOp()) {
            return;
        }
        updateManager.latest()
            .map(ChainedUpdateResult::getResult)
            .filter(result -> !result.hasError() && result.isUpdateAvailable())
            .ifPresent(result -> sendNotice(player, result));
    }

    private void sendNotice(final Player player, final UpdateResult result) {
        final String latest = result.getLatestVersion().orElse("unknown");
        final String source = updateManager.latest().flatMap(ChainedUpdateResult::getSourceUsed).orElse("unknown");
        MsgUtil.sendKey(
            player,
            "update.available",
            "<yellow>Update available for Factions: <white>{current}</white> -> <green>{latest}</green> <gray>({source})",
            "current", result.getCurrentVersion(),
            "latest", latest,
            "source", source
        );
        result.getReleaseUrl().ifPresent(url -> MsgUtil.sendKey(
            player,
            "update.url",
            "<gray>Download: <aqua><click:open_url:'{url}'>{url}</click>",
            "url", url
        ));
    }
}

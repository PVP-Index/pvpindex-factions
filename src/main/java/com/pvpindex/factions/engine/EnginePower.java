package com.pvpindex.factions.engine;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.scheduler.CancelableTask;
import com.pvpindex.factions.scheduler.TaskScheduler;
import com.pvpindex.factions.util.MsgUtil;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Periodically ticks player power and handles power updates on login/logout.
 */
public final class EnginePower implements Runnable, Listener {

    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;
    private final TaskScheduler taskScheduler;
    private CancelableTask timerTask;
    private final long startedAt = System.currentTimeMillis();

    public EnginePower(
            final Repositories repos,
            final FactionsConfig config,
            final Logger logger,
            final TaskScheduler taskScheduler) {
        this.repos = repos;
        this.config = config;
        this.logger = logger;
        this.taskScheduler = taskScheduler;
    }

    /**
     * Schedule this engine to run every {@code powerTickIntervalSeconds} seconds
     * and register Bukkit event listeners.
     *
     * @param plugin owning plugin (used only for listener registration)
     */
    public void start(final Plugin plugin) {
        final int intervalSeconds = Math.max(1, config.getPowerTickIntervalSeconds());
        final long intervalTicks = intervalSeconds * 20L;
        timerTask = taskScheduler.scheduleAsyncTimer(this, intervalTicks, intervalTicks);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Cancel the periodic power tick. Call from engine shutdown.
     */
    public void stop() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    @Override
    public void run() {
        try {
            final List<PlayerModel> players = repos.players().findAll();
            for (final PlayerModel pm : players) {
                tickPower(pm);
            }
            if (config.isRaidableBroadcastEnabled()) {
                checkRaidableTransitions();
            }
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to tick power for players", e);
        }
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        taskScheduler.runAsync(
            () -> applyPowerOnQuit(event.getPlayer().getUniqueId().toString()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(final PlayerDeathEvent event) {
        final Player dead = event.getPlayer();
        final Player killer = dead.getKiller();
        final String deadId = dead.getUniqueId().toString();
        final String killerId = killer != null ? killer.getUniqueId().toString() : null;
        final String worldName = dead.getWorld().getName();
        final int chunkX = dead.getLocation().getChunk().getX();
        final int chunkZ = dead.getLocation().getChunk().getZ();
        taskScheduler.runAsync(
            () -> applyDeathPower(deadId, killerId, worldName, chunkX, chunkZ));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void tickPower(final PlayerModel pm) throws StorageException {
        final double current = pm.getPower();
        final double max = config.getMaxPower();
        final Player online = Bukkit.getPlayer(java.util.UUID.fromString(pm.getId()));

        double regen;
        if (online != null) {
            regen = config.getPowerRegenOnline();
        } else {
            regen = config.getPowerRegenOffline();
        }

        if (current < max) {
            pm.setPower(Math.min(max, current + regen));
            repos.players().save(pm);
        }
    }

    private void applyPowerOnQuit(final String playerId) {
        // Power loss on death is handled by onDeath; nothing extra here.
    }

    private void applyDeathPower(
            final String deadId,
            final String killerId,
            final String worldName,
            final int chunkX,
            final int chunkZ) {
        try {
            // Respect server start grace period — no power loss during grace window.
            if (System.currentTimeMillis() - startedAt < config.getPowerGracePeriodSeconds() * 1000L) {
                return;
            }

            // Skip power changes in safezone territory when safe zones are active.
            final Optional<BoardEntry> claim = repos.board().findByChunk(worldName, chunkX, chunkZ);
            if (config.isSafeZoneEnabled()
                    && claim.isPresent()
                    && FactionModel.SAFEZONE_ID.equals(claim.get().getFactionId())) {
                return;
            }

            // Apply power loss to dead player.
            final Optional<PlayerModel> deadOpt = repos.players().find(deadId);
            if (deadOpt.isPresent()) {
                final PlayerModel deadModel = deadOpt.get();

                // F3: capture victim power before modifying it (used for kill scaling below)
                final double victimPowerBefore = deadModel.getPower();

                // F2: Death streak — escalate loss on repeated deaths within the window
                double loss = config.getPowerLossOnDeath();
                int streak = 0;
                if (config.isDeathStreakEnabled()) {
                    final long now = System.currentTimeMillis();
                    final long windowMs = config.getDeathStreakWindowSeconds() * 1000L;
                    final long lastDeath = deadModel.getLastDeathAt();
                    if (lastDeath > 0 && now - lastDeath <= windowMs) {
                        streak = deadModel.getDeathStreak() + 1;
                    }
                    // streak == 0 → first death in window, no multiplier yet
                    if (streak > 0) {
                        loss = loss * Math.pow(config.getDeathStreakMultiplier(), streak);
                    }
                    deadModel.setLastDeathAt(now);
                    deadModel.setDeathStreak(streak);
                }

                final double newPower = Math.max(0.0, deadModel.getPower() - loss);
                deadModel.setPower(newPower);
                repos.players().save(deadModel);
                repos.powerHistory().record(deadId, -loss, "DEATH", newPower);

                final double finalLoss = loss;
                final int finalStreak = streak;
                taskScheduler.runSync(() -> {
                    final Player deadPlayer = Bukkit.getPlayer(UUID.fromString(deadId));
                    if (deadPlayer != null) {
                        if (config.isDeathStreakEnabled() && finalStreak > 0) {
                            final String streakMsg = MsgUtil.replace(
                                MsgUtil.replace(
                                    MsgUtil.message("power.death-streak-penalty",
                                        "<red>Death streak \u00d7{streak}! You lost"
                                        + " <yellow>{amount}<red> power."),
                                    "streak", String.valueOf(finalStreak + 1)),
                                "amount", String.format(java.util.Locale.ROOT, "%.1f", finalLoss));
                            MsgUtil.send(deadPlayer, streakMsg);
                        } else {
                            final String lossMsg = MsgUtil.replace(
                                MsgUtil.message("power.lost-on-death",
                                    "<red>You lost <yellow>{amount}<red> power on death."),
                                "amount", String.format(java.util.Locale.ROOT, "%.1f", finalLoss));
                            MsgUtil.send(deadPlayer, lossMsg);
                        }
                    }
                });

                // Apply power gain to the killer if feature is enabled.
                if (killerId != null && config.isPowerGainOnKillEnabled()) {
                    final Optional<PlayerModel> killerOpt = repos.players().find(killerId);
                    if (killerOpt.isPresent()) {
                        final PlayerModel killerModel = killerOpt.get();

                        // F3: Scale the kill reward by victim-power / killer-power ratio
                        double gain = config.getPowerGainOnKill();
                        if (config.isKillScaleEnabled()) {
                            final double killerPower = killerModel.getPower();
                            if (killerPower > 0) {
                                final double ratio = victimPowerBefore / killerPower;
                                final double factor = Math.max(config.getKillScaleMinFactor(),
                                    Math.min(config.getKillScaleMaxFactor(), ratio));
                                gain = gain * factor;
                            } else {
                                gain = gain * config.getKillScaleMinFactor();
                            }
                        }

                        final double maxPower = config.getMaxPower();
                        final double newKillerPower = Math.min(maxPower, killerModel.getPower() + gain);
                        killerModel.setPower(newKillerPower);
                        repos.players().save(killerModel);
                        repos.powerHistory().record(killerId, gain, "KILL", newKillerPower);
                        final double finalGain = gain;
                        final String gainMsg = MsgUtil.replace(
                            MsgUtil.message("power.kill-gained",
                                "<green>You gained <yellow>{amount}<green> power from your kill."),
                            "amount", String.format(java.util.Locale.ROOT, "%.1f", finalGain));
                        taskScheduler.runSync(() -> {
                            final Player killerPlayer = Bukkit.getPlayer(UUID.fromString(killerId));
                            if (killerPlayer != null) {
                                MsgUtil.send(killerPlayer, gainMsg);
                            }
                        });
                    }
                }
            }
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to apply death power changes", e);
        }
    }

    /**
     * Compute the total power for a faction from the DB (sum of member power + power boost).
     *
     * @param factionId faction ID
     * @return total power
     */
    public double computeTotalPower(final String factionId) throws StorageException {
        final Optional<FactionModel> faction = repos.factions().find(factionId);
        double total = faction.map(FactionModel::getPowerBoost).orElse(0.0);
        for (final PlayerModel pm : repos.players().findByFactionId(factionId)) {
            total += effectivePower(pm);
        }
        return total;
    }

    // -------------------------------------------------------------------------
    // F4: Raidable state broadcast
    // -------------------------------------------------------------------------

    /**
     * After each power tick, iterate all normal factions and detect transitions between
     * the safe (enough power) and raidable (land exceeds max land) states. Broadcasts
     * a notification to faction members and optionally server-wide.
     */
    private void checkRaidableTransitions() throws StorageException {
        final double landPerPower = config.getLandPerPower();
        for (final FactionModel faction : repos.factions().findAll()) {
            if (!faction.isNormal()) {
                continue;
            }
            double totalPower = faction.getPowerBoost();
            for (final PlayerModel pm : repos.players().findByFactionId(faction.getId())) {
                totalPower += effectivePower(pm);
            }
            final int maxLand = landPerPower <= 0
                    ? config.getMaxLand()
                    : Math.min(config.getMaxLand(), (int) (totalPower / landPerPower));
            final int currentLand = repos.board().countByFactionId(faction.getId());
            final boolean nowRaidable = currentLand > maxLand;
            if (nowRaidable == faction.isRaidable()) {
                continue;
            }
            faction.setRaidable(nowRaidable);
            repos.factions().save(faction);

            final List<PlayerModel> members = repos.players().findByFactionId(faction.getId());
            final String memberMsgKey;
            final String memberDefault;
            if (nowRaidable) {
                memberMsgKey = "raidable.became-raidable";
                memberDefault = "<red>\u26a0 Your faction is now raidable!"
                        + " Enemies can overclaim your land.";
            } else {
                memberMsgKey = "raidable.no-longer-raidable";
                memberDefault = "<green>\u2714 Your faction is no longer raidable.";
            }
            final String memberMsg = MsgUtil.message(memberMsgKey, memberDefault);
            final String factionName = faction.getName();
            final boolean serverWide = config.isRaidableBroadcastServerWide();
            taskScheduler.runSync(() -> {
                for (final PlayerModel pm : members) {
                    try {
                        final Player p = Bukkit.getPlayer(UUID.fromString(pm.getId()));
                        if (p != null) {
                            MsgUtil.send(p, memberMsg);
                        }
                    } catch (IllegalArgumentException ignored) { }
                }
                if (serverWide) {
                    final String broadcastMsg = nowRaidable
                            ? MsgUtil.replace(
                                MsgUtil.message("raidable.server-announce",
                                    "<red>\u26a0 <yellow>{faction}</yellow> is now raidable!"),
                                "faction", factionName)
                            : MsgUtil.replace(
                                MsgUtil.message("raidable.server-announce-recovered",
                                    "<green>\u2714 <yellow>{faction}</yellow>"
                                    + " is no longer raidable."),
                                "faction", factionName);
                    for (final Player online : Bukkit.getOnlinePlayers()) {
                        MsgUtil.send(online, broadcastMsg);
                    }
                }
            });
        }
    }

    /**
     * Returns the power contribution of a player, respecting the inactive-exclusion
     * setting (F1). Members inactive longer than the configured threshold contribute 0.
     */
    private double effectivePower(final PlayerModel pm) {
        if (!config.isPowerInactiveExclusionEnabled()) {
            return pm.getPower();
        }
        final long inactiveMs = config.getPowerInactiveDays() * 24L * 3600L * 1000L;
        final long last = pm.getLastActivity();
        if (last > 0 && System.currentTimeMillis() - last > inactiveMs) {
            return 0.0;
        }
        return pm.getPower();
    }
}

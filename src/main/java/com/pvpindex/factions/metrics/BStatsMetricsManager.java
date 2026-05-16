package com.pvpindex.factions.metrics;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.Relation;
import com.pvpindex.factions.config.DatabaseConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.event.FactionCreateEvent;
import com.pvpindex.factions.event.FactionDisbandEvent;
import com.pvpindex.factions.scheduler.TaskScheduler;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * bStats bootstrap and chart providers.
 *
 * <p>Performance strategy for big datasets:
 * - faction counters are in-memory atomics
 * - relation distribution is cached and refreshed asynchronously
 * - no synchronous DB scans from chart callbacks
 */
public final class BStatsMetricsManager implements Listener {

    private static final long RELATION_REFRESH_TICKS = 20L * 60L * 15L; // 15 min

    private final Plugin plugin;
    private final Repositories repos;
    private final DatabaseConfig databaseConfig;
    private final Logger logger;
    private final TaskScheduler taskScheduler;
    private final String pluginVersion;

    private final AtomicInteger createdFactionsSinceStartup = new AtomicInteger(0);
    private final AtomicInteger totalFactions = new AtomicInteger(0);
    private volatile Map<String, Integer> relationCounts = defaultRelationCounts();

    public BStatsMetricsManager(
        final Plugin plugin,
        final Repositories repos,
        final DatabaseConfig databaseConfig,
        final Logger logger
    ) {
        this(plugin, repos, databaseConfig, null, logger);
    }

    public BStatsMetricsManager(
        final Plugin plugin,
        final Repositories repos,
        final DatabaseConfig databaseConfig,
        final TaskScheduler taskScheduler,
        final Logger logger
    ) {
        this.plugin = plugin;
        this.repos = repos;
        this.databaseConfig = databaseConfig;
        this.taskScheduler = taskScheduler;
        this.logger = logger;
        this.pluginVersion = plugin.getDescription().getVersion();
    }

    public void start(final int pluginId) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        warmCacheAsync();
        scheduleRelationRefresh();

        final Metrics metrics = new Metrics(plugin, pluginId);
        metrics.addCustomChart(new SingleLineChart("created_factions", createdFactionsSinceStartup::get));
        metrics.addCustomChart(new SingleLineChart("total_factions", totalFactions::get));

        metrics.addCustomChart(new DrilldownPie("relationship_type", () -> {
            final Map<String, Map<String, Integer>> out = new HashMap<>();
            final Map<String, Integer> snapshot = relationCounts;
            for (final Map.Entry<String, Integer> entry : snapshot.entrySet()) {
                out.put(entry.getKey(), Map.of(pluginVersion, entry.getValue()));
            }
            return out;
        }));

        metrics.addCustomChart(new DrilldownPie("database_type", () -> {
            final String dbType = normalizeDatabaseType(databaseConfig.getType());
            return Map.of(dbType, Map.of(pluginVersion, 1));
        }));
    }

    @EventHandler
    public void onFactionCreate(final FactionCreateEvent event) {
        createdFactionsSinceStartup.incrementAndGet();
        totalFactions.incrementAndGet();
    }

    @EventHandler
    public void onFactionDisband(final FactionDisbandEvent event) {
        totalFactions.updateAndGet(current -> Math.max(0, current - 1));
    }

    private void warmCacheAsync() {
        if (taskScheduler != null) {
            taskScheduler.runAsync(() -> {
                try {
                    totalFactions.set(repos.factions().countAll());
                    refreshRelationCache();
                } catch (StorageException e) {
                    logger.log(Level.WARNING, "Failed to warm bStats faction metrics cache", e);
                }
            });
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    totalFactions.set(repos.factions().countAll());
                    refreshRelationCache();
                } catch (StorageException e) {
                    logger.log(Level.WARNING, "Failed to warm bStats faction metrics cache", e);
                }
            });
        }
    }

    private void scheduleRelationRefresh() {
        if (taskScheduler != null) {
            taskScheduler.scheduleAsyncTimer(() -> {
                try {
                    refreshRelationCache();
                } catch (StorageException e) {
                    logger.log(Level.FINE, "Failed to refresh bStats relation cache", e);
                }
            }, RELATION_REFRESH_TICKS, RELATION_REFRESH_TICKS);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                try {
                    refreshRelationCache();
                } catch (StorageException e) {
                    logger.log(Level.FINE, "Failed to refresh bStats relation cache", e);
                }
            }, RELATION_REFRESH_TICKS, RELATION_REFRESH_TICKS);
        }
    }

    private void refreshRelationCache() throws StorageException {
        final List<FactionModel> factions = repos.factions().findAll();
        final Map<String, Integer> counts = defaultRelationCounts();
        for (final FactionModel faction : factions) {
            final Map<String, Relation> relations = parseRelations(faction.getRelationsJson());
            for (final Relation relation : relations.values()) {
                final String key = relation.name().toLowerCase();
                counts.computeIfPresent(key, (k, v) -> v + 1);
            }
        }
        relationCounts = Collections.unmodifiableMap(counts);
    }

    private Map<String, Integer> defaultRelationCounts() {
        final Map<String, Integer> out = new HashMap<>();
        for (final Relation relation : Relation.values()) {
            out.put(relation.name().toLowerCase(), 0);
        }
        return out;
    }

    private Map<String, Relation> parseRelations(final String json) {
        final Map<String, Relation> out = new HashMap<>();
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return out;
        }
        final String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return out;
        }
        final String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return out;
        }
        final String[] entries = body.split(",");
        for (final String rawEntry : entries) {
            final String[] kv = rawEntry.split(":", 2);
            if (kv.length != 2) {
                continue;
            }
            final String key = stripQuotes(kv[0].trim());
            final String value = stripQuotes(kv[1].trim());
            try {
                out.put(key, Relation.valueOf(value));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid serialized relation values.
            }
        }
        return out;
    }

    private String stripQuotes(final String value) {
        String out = value;
        if (out.startsWith("\"")) {
            out = out.substring(1);
        }
        if (out.endsWith("\"")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    private String normalizeDatabaseType(final String raw) {
        if (raw == null || raw.isBlank()) {
            return "unknown";
        }
        return raw.trim().toLowerCase();
    }
}

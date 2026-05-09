package com.pvpindex.factions.integration.lwc;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.Relation;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.event.FactionChunkClaimEvent;
import com.pvpindex.factions.util.MsgUtil;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

/**
 * Reflection-based LWCX interop.
 *
 * <p>All LWC/LWCX classes are resolved dynamically so startup remains safe
 * when the provider plugin is absent or changes minor API signatures.
 */
public final class LwcxInterop implements LwcInterop, Listener {

    private static final String EVENT_REGISTER = "com.griefcraft.scripting.event.LWCProtectionRegisterEvent";
    private static final String EVENT_INTERACT = "com.griefcraft.scripting.event.LWCProtectionInteractEvent";

    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;
    private final ConcurrentHashMap<String, Object> chunkLocks = new ConcurrentHashMap<>();

    private Plugin plugin;
    private boolean registered;
    private Class<?> registerEventClass;
    private Class<?> interactEventClass;

    public LwcxInterop(final Repositories repos, final FactionsConfig config, final Logger logger) {
        this.repos = repos;
        this.config = config;
        this.logger = logger;
    }

    @Override
    public void register(final Plugin plugin) {
        this.plugin = plugin;
        if (registered) {
            return;
        }

        try {
            registerEventClass = Class.forName(EVENT_REGISTER);
            interactEventClass = Class.forName(EVENT_INTERACT);
            final EventExecutor executor = (listener, event) -> onLwcEvent(event);
            plugin.getServer().getPluginManager().registerEvent(
                castEventClass(registerEventClass), this, EventPriority.HIGH, executor, plugin, true);
            plugin.getServer().getPluginManager().registerEvent(
                castEventClass(interactEventClass), this, EventPriority.HIGH, executor, plugin, true);
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            registered = true;
            logger.info("LWC integration listeners registered.");
        } catch (ClassNotFoundException e) {
            logger.info("LWC events not found - integration listeners not registered.");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to register LWC integration listeners", e);
        }
    }

    @Override
    public void unregister() {
        if (!registered) {
            return;
        }
        try {
            org.bukkit.event.HandlerList.unregisterAll(this);
        } catch (Exception ignored) {
            // Best effort; plugin shutdown continues.
        }
        registered = false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkClaim(final FactionChunkClaimEvent event) {
        if (!config.isLwcRemoveOnClaimChange()) {
            return;
        }
        cleanupChunkAsync(event.getFaction().getId(), event.getWorldName(), event.getChunkX(), event.getChunkZ());
    }

    private void onLwcEvent(final Event event) {
        try {
            final Class<?> eventClass = event.getClass();
            if (registerEventClass != null && registerEventClass.isAssignableFrom(eventClass)) {
                onProtectionRegister(event);
                return;
            }
            if (interactEventClass != null && interactEventClass.isAssignableFrom(eventClass)) {
                onProtectionInteract(event);
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "LWC event handling failed", e);
        }
    }

    private void onProtectionRegister(final Object event) {
        if (!config.isLwcRequireBuildRightsToCreate()) {
            return;
        }
        final Player player = invoke(event, "getPlayer", Player.class);
        final Block block = invoke(event, "getBlock", Block.class);
        if (player == null || block == null) {
            return;
        }
        if (canModify(player.getUniqueId(), block.getChunk())) {
            return;
        }
        invokeVoid(event, "setCancelled", boolean.class, true);
    }

    private void onProtectionInteract(final Object event) {
        if (!config.isLwcRemoveIfNoBuildRights()) {
            return;
        }
        final Object protection = invoke(event, "getProtection", Object.class);
        if (protection == null) {
            return;
        }
        final Block block = invoke(protection, "getBlock", Block.class);
        if (block == null) {
            return;
        }
        final String ownerName = invoke(protection, "getOwner", String.class);
        final UUID ownerUuid = resolveUuid(ownerName);
        if (ownerUuid == null) {
            return;
        }
        if (canModify(ownerUuid, block.getChunk())) {
            return;
        }

        invokeVoid(protection, "remove");
        final Object cancelResult = resolveCancelResult(event.getClass());
        if (cancelResult != null) {
            invokeVoid(event, "setResult", cancelResult.getClass(), cancelResult);
        }

        final Player actor = invoke(event, "getPlayer", Player.class);
        if (actor != null) {
            MsgUtil.send(actor, "<yellow>Removed stale LWC protection because the owner no longer has build rights.");
        }
    }

    private void cleanupChunkAsync(final String factionId, final String worldName, final int chunkX, final int chunkZ) {
        if (plugin == null || factionId == null) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin,
            () -> Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> cleanupChunkSync(factionId, worldName, chunkX, chunkZ)), 1L);
    }

    private void cleanupChunkSync(final String factionId, final String worldName, final int chunkX, final int chunkZ) {
        final String key = worldName + ":" + chunkX + ":" + chunkZ;
        final Object lock = chunkLocks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            try {
                final List<?> protections = getProtectionsInChunk(worldName, chunkX, chunkZ);
                for (final Object protection : protections) {
                    final String ownerName = invoke(protection, "getOwner", String.class);
                    final UUID ownerUuid = resolveUuid(ownerName);
                    if (ownerUuid == null) {
                        continue;
                    }
                    final Optional<PlayerModel> pm = repos.players().find(ownerUuid.toString());
                    if (pm.isPresent() && factionId.equals(pm.get().getFactionId())) {
                        continue;
                    }
                    invokeVoid(protection, "remove");
                }
            } catch (Exception e) {
                logger.log(Level.FINE, "Failed LWC cleanup for " + key, e);
            } finally {
                chunkLocks.remove(key);
            }
        }
    }

    private List<?> getProtectionsInChunk(final String world, final int chunkX, final int chunkZ) throws Exception {
        final Class<?> lwcClass = Class.forName("com.griefcraft.lwc.LWC");
        final Object lwc = lwcClass.getMethod("getInstance").invoke(null);
        final Object db = lwc.getClass().getMethod("getPhysicalDatabase").invoke(lwc);
        final int xmin = chunkX * 16;
        final int xmax = xmin + 15;
        final int zmin = chunkZ * 16;
        final int zmax = zmin + 15;
        final Object result = db.getClass().getMethod(
            "loadProtections", String.class, int.class, int.class, int.class, int.class, int.class, int.class)
            .invoke(db, world, xmin, xmax, 0, 255, zmin, zmax);
        if (result instanceof List<?> list) {
            return list;
        }
        return List.of();
    }

    private UUID resolveUuid(final String ownerName) {
        if (ownerName == null || ownerName.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(ownerName);
        } catch (IllegalArgumentException ignored) {
            // Not a UUID, resolve by name.
        }
        final Player online = Bukkit.getPlayerExact(ownerName);
        if (online != null) {
            return online.getUniqueId();
        }
        final OfflinePlayer offline = Bukkit.getOfflinePlayer(ownerName);
        if (offline.getUniqueId() == null) {
            return null;
        }
        return offline.getUniqueId();
    }

    private boolean canModify(final UUID playerUuid, final Chunk chunk) {
        try {
            final Optional<PlayerModel> pm = repos.players().find(playerUuid.toString());
            final Optional<BoardEntry> entry = repos.board().findByChunk(
                chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            if (entry.isEmpty()) {
                return true;
            }
            final String factionId = entry.get().getFactionId();
            if (FactionModel.SAFEZONE_ID.equals(factionId) || FactionModel.WARZONE_ID.equals(factionId)) {
                return false;
            }
            if (pm.isEmpty() || !pm.get().isInFaction()) {
                return false;
            }
            if (factionId.equals(pm.get().getFactionId())) {
                return true;
            }
            final Optional<FactionModel> claimOwner = repos.factions().find(factionId);
            final Optional<FactionModel> playerFaction = repos.factions().find(pm.get().getFactionId());
            if (claimOwner.isEmpty() || playerFaction.isEmpty()) {
                return false;
            }
            return getRelation(playerFaction.get(), factionId) == Relation.ALLY;
        } catch (StorageException e) {
            return false;
        }
    }

    private Relation getRelation(final FactionModel faction, final String otherFactionId) {
        final String relationJson = faction.getRelationsJson();
        if (relationJson == null) {
            return Relation.NEUTRAL;
        }
        final String token = "\"" + otherFactionId + "\":\"";
        final int start = relationJson.indexOf(token);
        if (start < 0) {
            return Relation.NEUTRAL;
        }
        final int valueStart = start + token.length();
        final int valueEnd = relationJson.indexOf('"', valueStart);
        if (valueEnd < 0) {
            return Relation.NEUTRAL;
        }
        try {
            return Relation.valueOf(relationJson.substring(valueStart, valueEnd));
        } catch (IllegalArgumentException e) {
            return Relation.NEUTRAL;
        }
    }

    private Object resolveCancelResult(final Class<?> eventClass) {
        for (final Class<?> nested : eventClass.getDeclaredClasses()) {
            if ("Result".equals(nested.getSimpleName()) && nested.isEnum()) {
                try {
                    @SuppressWarnings("unchecked")
                    final Class<? extends Enum> enumClass = (Class<? extends Enum>) nested;
                    return Enum.valueOf(enumClass, "CANCEL");
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Event> castEventClass(final Class<?> clazz) {
        return (Class<? extends Event>) clazz;
    }

    private <T> T invoke(final Object target, final String method, final Class<T> expectedType) {
        try {
            final Method m = target.getClass().getMethod(method);
            final Object value = m.invoke(target);
            if (value == null) {
                return null;
            }
            if (!expectedType.isAssignableFrom(value.getClass())) {
                return null;
            }
            return expectedType.cast(value);
        } catch (Exception e) {
            return null;
        }
    }

    private void invokeVoid(final Object target, final String method) {
        try {
            final Method m = target.getClass().getMethod(method);
            m.invoke(target);
        } catch (Exception ignored) {
            // Best effort for optional interop.
        }
    }

    private void invokeVoid(final Object target, final String method, final Class<?> argType, final Object arg) {
        try {
            final Method m = target.getClass().getMethod(method, argType);
            m.invoke(target, arg);
        } catch (Exception ignored) {
            // Best effort for optional interop.
        }
    }
}

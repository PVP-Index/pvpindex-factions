package com.gyvex.pvpindex.factions.integration.dynmap;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.gyvex.pvpindex.factions.data.Repositories;
import com.gyvex.pvpindex.factions.data.model.BoardEntry;
import com.gyvex.pvpindex.factions.data.model.FactionModel;
import com.gyvex.pvpindex.factions.event.FactionChunkClaimEvent;
import com.gyvex.pvpindex.factions.event.FactionChunkUnclaimEvent;
import com.gyvex.pvpindex.factions.event.FactionDisbandEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

/**
 * Renders faction territory as coloured area regions on the dynmap live map.
 *
 * <p>This class is only instantiated when the {@code dynmap} plugin is present.
 * It must never be referenced by code paths that execute without dynmap on the
 * classpath — doing so would cause a {@link NoClassDefFoundError}.
 *
 * <p>Claims are loaded from the database one tick after startup so that dynmap
 * has time to finish its own initialisation. Subsequent changes are reflected
 * in real-time via {@link FactionChunkClaimEvent}, {@link FactionChunkUnclaimEvent},
 * and {@link FactionDisbandEvent}.
 */
public final class DynmapLayer implements Listener {

    private static final String LAYER_ID = "pvpindex_factions";
    private static final String LAYER_LABEL = "Factions";

    /**
     * Colour palette — 24-bit RGB integers (0xRRGGBB).
     * Faction colours are derived by hashing the faction ID into this array.
     */
    private static final int[] PALETTE = {
        0x3399ff, 0xff6633, 0x33cc33, 0xff3399,
        0x9966ff, 0xffcc00, 0x00cccc, 0xff6600,
    };

    private final Repositories repos;
    private final Logger logger;
    private MarkerSet markerSet;

    public DynmapLayer(final Repositories repos, final Logger logger) {
        this.repos = repos;
        this.logger = logger;
    }

    /**
     * Hook into dynmap and register the faction territory layer.
     *
     * @param plugin the owning plugin instance
     * @return {@code true} if the layer was registered successfully
     */
    public boolean start(final Plugin plugin) {
        final DynmapAPI dynmapApi =
            (DynmapAPI) plugin.getServer().getPluginManager().getPlugin("dynmap");
        if (dynmapApi == null) {
            return false;
        }
        final MarkerAPI markerApi = dynmapApi.getMarkerAPI();
        if (markerApi == null) {
            logger.warning("dynmap MarkerAPI not ready — faction territory layer skipped.");
            return false;
        }
        // Remove a stale layer left over from a previous load (e.g. /fa reload)
        final MarkerSet existing = markerApi.getMarkerSet(LAYER_ID);
        if (existing != null) {
            existing.deleteMarkerSet();
        }
        markerSet = markerApi.createMarkerSet(LAYER_ID, LAYER_LABEL, null, false);
        if (markerSet == null) {
            logger.warning("Failed to create dynmap faction marker set.");
            return false;
        }
        markerSet.setHideByDefault(false);
        markerSet.setLayerPriority(5);
        // Load all existing claims one tick later so dynmap finishes its own startup
        plugin.getServer().getScheduler().runTask(plugin, this::loadAllClaims);
        org.bukkit.Bukkit.getPluginManager().registerEvents(this, plugin);
        return true;
    }

    // -------------------------------------------------------------------------
    // Bukkit event listeners — MONITOR priority so we act only on confirmed events
    // -------------------------------------------------------------------------

    /** Add a marker when a chunk is successfully claimed. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClaim(final FactionChunkClaimEvent event) {
        if (markerSet == null) {
            return;
        }
        final FactionModel faction = event.getFaction();
        addChunkMarker(faction.getId(), faction.getName(),
            event.getWorldName(), event.getChunkX(), event.getChunkZ());
    }

    /** Remove a marker when a chunk is successfully unclaimed. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onUnclaim(final FactionChunkUnclaimEvent event) {
        if (markerSet == null) {
            return;
        }
        removeChunkMarker(event.getWorldName(), event.getChunkX(), event.getChunkZ());
    }

    /** Remove all territory markers when a faction is disbanded. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDisband(final FactionDisbandEvent event) {
        if (markerSet == null) {
            return;
        }
        final String prefix = event.getFaction().getId() + "~";
        for (final AreaMarker marker : List.copyOf(markerSet.getAreaMarkers())) {
            if (marker.getMarkerID().startsWith(prefix)) {
                marker.deleteMarker();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Marker management
    // -------------------------------------------------------------------------

    private void loadAllClaims() {
        if (markerSet == null) {
            return;
        }
        // Clear stale markers before rebuilding
        for (final AreaMarker marker : List.copyOf(markerSet.getAreaMarkers())) {
            marker.deleteMarker();
        }
        try {
            for (final FactionModel faction : repos.factions().findAll()) {
                final List<BoardEntry> claims = repos.board().findByFactionId(faction.getId());
                for (final BoardEntry entry : claims) {
                    addChunkMarker(faction.getId(), faction.getName(),
                        entry.getWorldName(), entry.getChunkX(), entry.getChunkZ());
                }
            }
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Failed to load faction claims for dynmap layer", e);
        }
    }

    private void addChunkMarker(
            final String factionId, final String factionName,
            final String worldName, final int chunkX, final int chunkZ) {
        if (markerSet == null) {
            return;
        }
        final String markerId = markerId(factionId, worldName, chunkX, chunkZ);
        // Replace any existing marker for this chunk (faction ownership change)
        final AreaMarker old = markerSet.findAreaMarker(markerId);
        if (old != null) {
            old.deleteMarker();
        }
        final double x0 = chunkX * 16.0;
        final double z0 = chunkZ * 16.0;
        final double[] xCorners = {x0, x0, x0 + 16.0, x0 + 16.0};
        final double[] zCorners = {z0, z0 + 16.0, z0 + 16.0, z0};
        final AreaMarker marker = markerSet.createAreaMarker(
            markerId, factionName, false, worldName, xCorners, zCorners, false);
        if (marker != null) {
            final int color = factionColor(factionId);
            marker.setFillStyle(0.35, color);
            marker.setLineStyle(1, 1.0, color);
        }
    }

    /**
     * Remove the marker for a specific chunk.
     *
     * <p>Since the faction ID is not available on unclaim events, we locate
     * the marker by scanning for an ID that ends with the chunk's suffix.
     */
    private void removeChunkMarker(final String worldName, final int chunkX, final int chunkZ) {
        if (markerSet == null) {
            return;
        }
        final String suffix = "~" + worldName + "~" + chunkX + "~" + chunkZ;
        for (final AreaMarker marker : List.copyOf(markerSet.getAreaMarkers())) {
            if (marker.getMarkerID().endsWith(suffix)) {
                marker.deleteMarker();
                break;
            }
        }
    }

    /**
     * Marker ID format: {@code factionId~worldName~chunkX~chunkZ}.
     *
     * <p>Using {@code ~} as the separator keeps it disjoint from UUID hyphens,
     * world name characters, and integer digits.
     */
    private static String markerId(
            final String factionId, final String worldName, final int chunkX, final int chunkZ) {
        return factionId + "~" + worldName + "~" + chunkX + "~" + chunkZ;
    }

    /** Deterministic colour derived from the faction ID hash. */
    private static int factionColor(final String factionId) {
        return PALETTE[Math.abs(factionId.hashCode()) % PALETTE.length];
    }
}

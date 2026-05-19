package com.pvpindex.factions.api;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.PlayerModel;
import com.skyblockexp.teamsapi.api.TeamsNotificationService;
import com.skyblockexp.teamsapi.model.TeamNotificationType;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Adapts the factions notification preferences to the TeamsAPI
 * {@link TeamsNotificationService} interface.
 *
 * <h2>Delivery</h2>
 * <p>Notifications are delivered immediately to online players via
 * {@link Player#sendMessage}. Offline delivery is not attempted; callers that
 * need reliable offline delivery should implement their own queuing on top.</p>
 *
 * <h2>Per-player preferences</h2>
 * <p>The following built-in {@link TeamNotificationType} values are backed by a
 * persisted preference in {@link PlayerModel}:</p>
 * <ul>
 *   <li>{@link TeamNotificationType#TEAM_INVITE} → {@code notify_invites}</li>
 * </ul>
 * <p>All other built-in types and all custom string types default to
 * {@code enabled = true} and calls to {@link #setNotificationEnabled} for
 * those types return {@code false} (unsupported).</p>
 *
 * <p>This class is only instantiated when TeamsAPI 1.7+ is present on the
 * server.</p>
 */
public final class FactionsTeamsNotificationService implements TeamsNotificationService {

    private final Repositories repos;
    private final Logger logger;

    /**
     * Creates a new {@link FactionsTeamsNotificationService}.
     *
     * @param repos  the factions repositories; must not be {@code null}
     * @param logger the plugin logger; must not be {@code null}
     */
    public FactionsTeamsNotificationService(final Repositories repos, final Logger logger) {
        this.repos = repos;
        this.logger = logger;
    }

    // -------------------------------------------------------------------------
    // TeamsNotificationService — sendNotification overloads
    // -------------------------------------------------------------------------

    @Override
    public boolean sendNotification(
            final Plugin senderPlugin,
            final UUID recipientUUID,
            final TeamNotificationType type,
            final String message) {
        if (!isNotificationEnabled(recipientUUID, type)) {
            return false;
        }
        return deliverToOnlinePlayer(recipientUUID, message);
    }

    @Override
    public boolean sendNotification(
            final Plugin senderPlugin,
            final UUID recipientUUID,
            final String notificationType,
            final String message) {
        if (notificationType == null || notificationType.isBlank()) {
            return false;
        }
        // No per-player preferences for custom types; always attempt delivery.
        return deliverToOnlinePlayer(recipientUUID, message);
    }

    // -------------------------------------------------------------------------
    // TeamsNotificationService — isNotificationEnabled overloads
    // -------------------------------------------------------------------------

    @Override
    public boolean isNotificationEnabled(
            final UUID playerUUID, final TeamNotificationType type) {
        if (type == TeamNotificationType.TEAM_INVITE) {
            return loadPlayerPref(playerUUID, true, PlayerModel::hasInviteNotifications);
        }
        // All other built-in types are enabled by default.
        return true;
    }

    @Override
    public boolean isNotificationEnabled(
            final UUID playerUUID, final String notificationType) {
        if (notificationType == null || notificationType.isBlank()) {
            return false;
        }
        // No per-player preferences stored for custom types — treat as enabled.
        return true;
    }

    // -------------------------------------------------------------------------
    // TeamsNotificationService — setNotificationEnabled overloads
    // -------------------------------------------------------------------------

    @Override
    public boolean setNotificationEnabled(
            final UUID playerUUID, final TeamNotificationType type, final boolean enabled) {
        if (type == TeamNotificationType.TEAM_INVITE) {
            return updatePlayerPref(playerUUID, pm -> pm.setInviteNotifications(enabled));
        }
        // Other built-in types cannot be persisted — report unsupported.
        return false;
    }

    @Override
    public boolean setNotificationEnabled(
            final UUID playerUUID, final String notificationType, final boolean enabled) {
        // Custom notification type preferences are not persisted.
        return false;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Delivers {@code message} to the player if they are currently online. */
    private boolean deliverToOnlinePlayer(final UUID recipientUUID, final String message) {
        final Player player = Bukkit.getPlayer(recipientUUID);
        if (player == null || !player.isOnline()) {
            return false;
        }
        player.sendMessage(message);
        return true;
    }

    @FunctionalInterface
    private interface PlayerReader<T> {
        T read(PlayerModel model);
    }

    @FunctionalInterface
    private interface PlayerWriter {
        void write(PlayerModel model);
    }

    private <T> T loadPlayerPref(
            final UUID playerUUID, final T defaultValue, final PlayerReader<T> reader) {
        try {
            final Optional<PlayerModel> pm = repos.players().find(playerUUID.toString());
            return pm.map(reader::read).orElse(defaultValue);
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Could not load notification pref for " + playerUUID, e);
            return defaultValue;
        }
    }

    private boolean updatePlayerPref(final UUID playerUUID, final PlayerWriter writer) {
        try {
            final Optional<PlayerModel> pm = repos.players().find(playerUUID.toString());
            if (pm.isEmpty()) {
                return false;
            }
            writer.write(pm.get());
            repos.players().save(pm.get());
            return true;
        } catch (StorageException e) {
            logger.log(Level.WARNING, "Could not update notification pref for " + playerUUID, e);
            return false;
        }
    }
}

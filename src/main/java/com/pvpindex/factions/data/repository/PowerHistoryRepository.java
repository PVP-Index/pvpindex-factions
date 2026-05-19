package com.pvpindex.factions.data.repository;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.model.TableRegistry;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.github.ezframework.javaquerybuilder.query.builder.QueryBuilder;
import com.pvpindex.factions.data.model.PowerHistoryModel;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Repository for per-player power change history records. */
public class PowerHistoryRepository extends ModelRepository<PowerHistoryModel> {

    public PowerHistoryRepository(final DataSourceJdbcStore store) {
        super(store, PowerHistoryModel.PREFIX, (id, data) -> new PowerHistoryModel(id));
        TableRegistry.register(
            PowerHistoryModel.PREFIX, "power_history", PowerHistoryModel.COLUMNS);
    }

    /**
     * Return the most recent power history entries for the given player,
     * sorted newest-first with pagination.
     */
    public List<PowerHistoryModel> findRecentByPlayerUuid(
            final String playerUuid, final int limit, final int offset) throws StorageException {
        final List<PowerHistoryModel> rows = query(
            new QueryBuilder().whereEquals("player_uuid", playerUuid).build()
        );
        rows.sort(Comparator.comparingLong(PowerHistoryModel::getCreatedAt).reversed());
        if (offset >= rows.size()) {
            return List.of();
        }
        final int to = Math.min(rows.size(), offset + limit);
        return rows.subList(offset, to);
    }

    /**
     * Insert a new power history entry.
     *
     * @param playerUuid UUID string of the player
     * @param delta      amount gained (positive) or lost (negative)
     * @param reason     short label: {@code DEATH}, {@code KILL}, or {@code BUY}
     * @param powerAfter player's power value after the change
     */
    public void record(
            final String playerUuid,
            final double delta,
            final String reason,
            final double powerAfter) throws StorageException {
        final PowerHistoryModel entry = new PowerHistoryModel(UUID.randomUUID().toString());
        entry.setPlayerUuid(playerUuid);
        entry.setDelta(delta);
        entry.setReason(reason);
        entry.setPowerAfter(powerAfter);
        entry.setCreatedAt(System.currentTimeMillis());
        save(entry);
    }

    /**
     * Insert a power history entry with an explicit ID and timestamp.
     *
     * <p>Used by the TeamsAPI integration to honour external entry identities.</p>
     *
     * @param id           the entry UUID
     * @param playerUuid   UUID string of the player
     * @param delta        signed power change amount
     * @param reason       provider reason key
     * @param occurredAtMs epoch-millisecond timestamp
     * @return {@code true} if the entry was inserted, {@code false} if the ID
     *         already exists
     */
    public boolean insert(
            final UUID id,
            final String playerUuid,
            final double delta,
            final String reason,
            final long occurredAtMs) throws StorageException {
        if (find(id.toString()).isPresent()) {
            return false;
        }
        final PowerHistoryModel entry = new PowerHistoryModel(id.toString());
        entry.setPlayerUuid(playerUuid);
        entry.setDelta(delta);
        entry.setReason(reason);
        entry.setPowerAfter(0.0);
        entry.setCreatedAt(occurredAtMs);
        save(entry);
        return true;
    }

    /**
     * Return all power history entries for the given player, sorted newest-first.
     *
     * @param playerUuid UUID string of the player
     * @return mutable list sorted by {@code created_at} descending
     */
    public List<PowerHistoryModel> findAllByPlayerUuid(final String playerUuid)
            throws StorageException {
        final List<PowerHistoryModel> rows = query(
            new QueryBuilder().whereEquals("player_uuid", playerUuid).build()
        );
        rows.sort(Comparator.comparingLong(PowerHistoryModel::getCreatedAt).reversed());
        return rows;
    }

    /**
     * Delete a single power history entry.
     *
     * @param id entry UUID string
     * @return {@code true} if the entry existed and was removed
     */
    public boolean deleteById(final String id) throws StorageException {
        if (find(id).isEmpty()) {
            return false;
        }
        delete(id);
        return true;
    }
}

package com.gyvex.pvpindex.factions.data.repository;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.model.TableRegistry;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.github.ezframework.javaquerybuilder.query.builder.QueryBuilder;
import com.gyvex.pvpindex.factions.data.model.AutoTerritoryMode;
import com.gyvex.pvpindex.factions.data.model.PlayerModel;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link PlayerModel} instances.
 */
public class PlayerRepository extends ModelRepository<PlayerModel> {

    public PlayerRepository(final DataSourceJdbcStore store) {
        super(store, PlayerModel.PREFIX, (id, data) -> new PlayerModel(id));
        TableRegistry.register(PlayerModel.PREFIX, "players", PlayerModel.COLUMNS);
    }

    /**
     * Find all players that belong to a given faction.
     *
     * @param factionId faction UUID string
     * @return list of member PlayerModels
     * @throws StorageException on database error
     */
    public List<PlayerModel> findByFactionId(final String factionId) throws StorageException {
        return query(new QueryBuilder().whereEquals("faction_id", factionId).build());
    }

    /**
     * Find all players in the database.
     *
     * @return list of all PlayerModels
     * @throws StorageException on database error
     */
    public List<PlayerModel> findAll() throws StorageException {
        return query(new QueryBuilder().build());
    }

    /**
     * Find the PlayerModel for the given player UUID, or insert a fresh one.
     *
     * @param playerUuid player UUID string
     * @return existing or new PlayerModel (not yet saved for new ones)
     * @throws StorageException on database error
     */
    public PlayerModel findOrCreate(final String playerUuid) throws StorageException {
        final Optional<PlayerModel> existing = find(playerUuid);
        if (existing.isPresent()) {
            return ensureWriteSafeDefaults(existing.get());
        }
        return ensureWriteSafeDefaults(new PlayerModel(playerUuid));
    }

    /**
     * Remove all player membership records that belong to the given faction.
     * This does NOT delete the PlayerModel row — it clears the faction_id field.
     *
     * @param factionId faction UUID string
     * @throws StorageException on database error
     */
    public void clearFactionMembers(final String factionId) throws StorageException {
        final List<PlayerModel> members = findByFactionId(factionId);
        for (final PlayerModel member : members) {
            member.setFactionId(null);
            member.setRankId(null);
            save(member);
        }
    }

    private PlayerModel ensureWriteSafeDefaults(final PlayerModel model) {
        if (model.getTitle() == null) {
            model.setTitle("");
        }
        model.setTerritoryTitles(model.hasTerritoryTitles());
        model.setOverriding(model.isOverriding());
        model.setInviteNotifications(model.hasInviteNotifications());
        model.setBankTaxNotifications(model.hasBankTaxNotifications());
        model.setAutoTerritoryMode(model.getAutoTerritoryMode());
        if (model.getAutoTerritoryMode() == null) {
            model.setAutoTerritoryMode(AutoTerritoryMode.OFF);
        }
        return model;
    }
}

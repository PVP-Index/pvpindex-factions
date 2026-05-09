package com.pvpindex.factions.data.repository;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.github.ezframework.jaloquent.model.ModelRepository;
import com.github.ezframework.jaloquent.model.TableRegistry;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.github.ezframework.javaquerybuilder.query.builder.QueryBuilder;
import com.pvpindex.factions.data.model.BoardEntry;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link BoardEntry} (chunk claim) instances.
 */
public class BoardRepository extends ModelRepository<BoardEntry> {

    public BoardRepository(final DataSourceJdbcStore store) {
        super(store, BoardEntry.PREFIX, (id, data) -> new BoardEntry(id));
        TableRegistry.register(BoardEntry.PREFIX, "board", BoardEntry.COLUMNS);
    }

    /**
     * Find the BoardEntry for the given chunk, if claimed.
     *
     * @param worldName Bukkit world name
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return the entry, or empty if unclaimed
     * @throws StorageException on database error
     */
    public Optional<BoardEntry> findByChunk(
            final String worldName, final int chunkX, final int chunkZ)
            throws StorageException {
        return find(BoardEntry.buildId(worldName, chunkX, chunkZ));
    }

    /**
     * Return all chunks claimed by a given faction.
     *
     * @param factionId faction UUID string
     * @return list of claimed BoardEntries
     * @throws StorageException on database error
     */
    public List<BoardEntry> findByFactionId(final String factionId) throws StorageException {
        return query(new QueryBuilder().whereEquals("faction_id", factionId).build());
    }

    /**
     * Count the number of chunks claimed by a faction without loading all entries.
     *
     * @param factionId faction UUID string
     * @return land count
     * @throws StorageException on database error
     */
    public int countByFactionId(final String factionId) throws StorageException {
        return findByFactionId(factionId).size();
    }

    /**
     * Delete all claims belonging to a faction (e.g. when faction is disbanded).
     *
     * @param factionId faction UUID string
     * @throws StorageException on database error
     */
    public void deleteByFactionId(final String factionId) throws StorageException {
        deleteWhere("faction_id", factionId);
    }

    /**
     * Convenience: claim a chunk for a faction, creating or overwriting the entry.
     *
     * @param worldName Bukkit world name
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param factionId faction UUID string
     * @throws StorageException on database error
     */
    public void claimChunk(
            final String worldName, final int chunkX, final int chunkZ,
            final String factionId) throws StorageException {
        final BoardEntry entry = new BoardEntry(BoardEntry.buildId(worldName, chunkX, chunkZ));
        entry.setFactionId(factionId);
        save(entry);
    }

    /**
     * Convenience: unclaim a chunk.
     *
     * @param worldName Bukkit world name
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @throws StorageException on database error
     */
    public void unclaimChunk(
            final String worldName, final int chunkX, final int chunkZ)
            throws StorageException {
        delete(BoardEntry.buildId(worldName, chunkX, chunkZ));
    }
}

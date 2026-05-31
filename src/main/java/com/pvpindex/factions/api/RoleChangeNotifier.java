package com.pvpindex.factions.api;

import com.pvpindex.factions.data.model.RankModel;

/**
 * Notifier interface for rank (role) lifecycle changes.
 * Implementations may update external integrations (e.g., TeamsAPI).
 */
public interface RoleChangeNotifier {

    default void roleCreated(RankModel rank) { }

    default void roleUpdated(RankModel rank) { }

    default void roleRenamed(RankModel rank, String oldName) { }

    default void roleDeleted(RankModel rank) { }
}

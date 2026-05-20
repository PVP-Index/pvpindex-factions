package com.pvpindex.factions.service;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.data.model.PlayerModel;
import java.util.Optional;

/** Shared power mutation service used by commands and engines. */
public interface PowerService {

    enum Source {
        REGEN_ONLINE,
        REGEN_OFFLINE,
        DEATH,
        KILL,
        BUY,
        ADMIN_SET,
        ADMIN_ADD,
        ADMIN_REMOVE,
        ADMIN_RESET
    }

    enum ZoneContext {
        SAFEZONE,
        WARZONE,
        OWN_CLAIMED,
        ENEMY_CLAIMED,
        WILDERNESS
    }

    record Request(
        String playerId,
        Source source,
        double baseDelta,
        String actorName,
        String reason,
        String world,
        ZoneContext zone,
        boolean bypassFreeze
    ) { }

    record Result(
        boolean changed,
        boolean blockedByFreeze,
        double before,
        double requestedDelta,
        double effectiveDelta,
        double after,
        String reasonCode
    ) { }

    Optional<PlayerModel> findPlayerByNameOrUuid(String input) throws StorageException;

    Result apply(Request request) throws StorageException;

    double getFactionPower(String factionId) throws StorageException;
}


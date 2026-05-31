package com.pvpindex.factions.service;

/** Result of a {@link FactionService#createRole} operation. */
public enum CreateRoleResult {

    /** Role was created successfully. */
    SUCCESS,

    /** Custom roles or per-faction overrides are disabled in server configuration. */
    FEATURE_DISABLED,

    /** Actor is not in a faction or has no rank record. */
    NOT_IN_FACTION,

    /** The supplied role name was blank. */
    INVALID_NAME,

    /** A role with that name already exists in the faction. */
    NAME_TAKEN,

    /** The requested priority is outside the configured [min, max] range. */
    PRIORITY_OUT_OF_RANGE,

    /** The actor's own rank priority is not greater than the requested priority. */
    ACTOR_RANK_INSUFFICIENT,

    /** The faction has already reached the maximum number of custom roles. */
    ROLE_LIMIT_REACHED,

    /** A storage exception occurred while saving the new role. */
    STORAGE_ERROR
}

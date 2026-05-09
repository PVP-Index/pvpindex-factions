package com.gyvex.pvpindex.factions.data.model;

/**
 * Auto-territory mode for chunk-crossing behavior.
 */
public enum AutoTerritoryMode {
    OFF(0),
    CLAIM(1),
    UNCLAIM(2);

    private final int dbValue;

    AutoTerritoryMode(final int dbValue) {
        this.dbValue = dbValue;
    }

    public int getDbValue() {
        return dbValue;
    }

    public static AutoTerritoryMode fromDbValue(final int value) {
        return switch (value) {
            case 1 -> CLAIM;
            case 2 -> UNCLAIM;
            default -> OFF;
        };
    }
}

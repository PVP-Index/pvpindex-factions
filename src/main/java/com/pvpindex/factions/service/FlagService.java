package com.pvpindex.factions.service;

import com.pvpindex.factions.FactionFlag;
import com.pvpindex.factions.data.model.FactionModel;
import java.util.Map;

/**
 * Manages per-faction boolean flags.
 *
 * <p>Flag values are stored sparsely in {@link FactionModel#getFlagsJson()}.
 * Absent entries fall back to the server-wide default from
 * {@link com.pvpindex.factions.config.FactionsConfig}.
 */
public interface FlagService {

    /**
     * Get the effective flag value for a faction.
     *
     * <p>Returns the per-faction override when explicitly set, otherwise the
     * server-wide config default.
     */
    boolean getFlag(FactionModel faction, FactionFlag flag);

    /**
     * Set a per-faction flag value and persist the change immediately.
     */
    void setFlag(FactionModel faction, FactionFlag flag, boolean value);

    /**
     * Return all flag values for the given faction, including defaults for any
     * flags the faction has not explicitly set.
     */
    Map<FactionFlag, Boolean> getAllFlags(FactionModel faction);

    /**
     * Whether a flag may be toggled by faction officers via {@code /f flag set}.
     *
     * <p>Admins may always override regardless of this setting.
     */
    boolean isFlagEditable(FactionFlag flag);
}

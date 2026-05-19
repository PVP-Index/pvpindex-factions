package com.pvpindex.factions.service;

import com.github.ezframework.jaloquent.exception.StorageException;
import com.pvpindex.factions.FactionFlag;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of {@link FlagService}.
 *
 * <p>Flags are stored sparsely in {@link FactionModel#getFlagsJson()} using a
 * simple JSON map of {@code {"flag-id": true/false}} entries. Absent keys fall
 * back to the server-configured default from {@link FactionsConfig}.
 *
 * <p>JSON is encoded/decoded without an external library, following the same
 * lightweight string-scan pattern used by {@code FactionServiceImpl} for
 * relations.
 */
public final class FlagServiceImpl implements FlagService {

    private final Repositories repos;
    private final FactionsConfig config;
    private final Logger logger;

    public FlagServiceImpl(
            final Repositories repos, final FactionsConfig config, final Logger logger) {
        this.repos = repos;
        this.config = config;
        this.logger = logger;
    }

    // -------------------------------------------------------------------------
    // FlagService
    // -------------------------------------------------------------------------

    @Override
    public boolean getFlag(final FactionModel faction, final FactionFlag flag) {
        final Map<String, Boolean> parsed = parseFlags(faction.getFlagsJson());
        if (parsed.containsKey(flag.getId())) {
            return parsed.get(flag.getId());
        }
        return config.getFlagDefault(flag);
    }

    @Override
    public void setFlag(final FactionModel faction, final FactionFlag flag, final boolean value) {
        final Map<String, Boolean> parsed = parseFlags(faction.getFlagsJson());
        parsed.put(flag.getId(), value);
        faction.setFlagsJson(serializeFlags(parsed));
        try {
            repos.factions().save(faction);
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Failed to save flag " + flag.getId()
                    + " for faction " + faction.getId(), e);
        }
    }

    @Override
    public Map<FactionFlag, Boolean> getAllFlags(final FactionModel faction) {
        final Map<String, Boolean> raw = parseFlags(faction.getFlagsJson());
        final Map<FactionFlag, Boolean> out = new EnumMap<>(FactionFlag.class);
        for (final FactionFlag flag : FactionFlag.values()) {
            out.put(flag, raw.containsKey(flag.getId())
                    ? raw.get(flag.getId())
                    : config.getFlagDefault(flag));
        }
        return out;
    }

    @Override
    public boolean isFlagEditable(final FactionFlag flag) {
        return config.isFlagPlayerEditable(flag);
    }

    // -------------------------------------------------------------------------
    // JSON helpers
    // -------------------------------------------------------------------------

    /**
     * Parse a flags JSON string like {@code {"pvp":true,"explosions":false}}
     * into a {@code Map<flagId, boolean>}.
     */
    static Map<String, Boolean> parseFlags(final String json) {
        final Map<String, Boolean> out = new HashMap<>();
        if (json == null || json.isBlank() || "{}".equals(json.trim())) {
            return out;
        }
        final String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return out;
        }
        final String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return out;
        }
        for (final String rawEntry : body.split(",")) {
            final String[] kv = rawEntry.split(":", 2);
            if (kv.length != 2) {
                continue;
            }
            final String key = stripQuotes(kv[0].trim());
            final String value = kv[1].trim();
            if ("true".equalsIgnoreCase(value)) {
                out.put(key, Boolean.TRUE);
            } else if ("false".equalsIgnoreCase(value)) {
                out.put(key, Boolean.FALSE);
            }
        }
        return out;
    }

    /** Serialize a {@code Map<flagId, boolean>} to a compact JSON string. */
    static String serializeFlags(final Map<String, Boolean> map) {
        final StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (final Map.Entry<String, Boolean> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(entry.getKey()).append('"')
              .append(':')
              .append(entry.getValue());
        }
        sb.append('}');
        return sb.toString();
    }

    private static String stripQuotes(final String value) {
        String out = value;
        if (out.startsWith("\"")) {
            out = out.substring(1);
        }
        if (out.endsWith("\"")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }
}

package com.pvpindex.factions.config;

import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.PlayerModel;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Locale-aware typed access wrapper for messages bundles.
 */
public class MessagesConfig {

    private static final String ENGLISH_LOCALE = "en";

    private final Map<String, FileConfiguration> bundles;
    private final String defaultLocale;
    private Repositories repositories;

    public MessagesConfig(final Map<String, FileConfiguration> bundles, final String defaultLocale) {
        this.bundles = new LinkedHashMap<>();
        for (Map.Entry<String, FileConfiguration> entry : bundles.entrySet()) {
            this.bundles.put(normalizeLocale(entry.getKey()), entry.getValue());
        }
        this.defaultLocale = normalizeLocale(defaultLocale);
    }

    public void setRepositories(final Repositories repositories) {
        this.repositories = repositories;
    }

    public Set<String> getAvailableLocales() {
        return Collections.unmodifiableSet(bundles.keySet());
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public boolean isSupportedLocale(final String locale) {
        return bundles.containsKey(normalizeLocale(locale));
    }

    public static String normalizeLocale(final String locale) {
        if (locale == null || locale.isBlank()) {
            return ENGLISH_LOCALE;
        }
        String normalized = locale.trim().replace('_', '-');
        if (normalized.equalsIgnoreCase("pt-br")) {
            return "pt-BR";
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        return normalized;
    }

    public String resolveSenderLocale(final CommandSender sender) {
        if (sender instanceof Player player && repositories != null) {
            try {
                final Optional<PlayerModel> pmOpt = repositories.players().find(player.getUniqueId().toString());
                if (pmOpt.isPresent()) {
                    final String stored = pmOpt.get().getLocale();
                    if (isSupportedLocale(stored)) {
                        return normalizeLocale(stored);
                    }
                }
            } catch (Exception ignored) {
                // Fallback to default locale.
            }
        }
        return defaultLocale;
    }

    public String get(final String path, final String fallback) {
        return get(path, fallback, defaultLocale);
    }

    public String get(final String path, final String fallback, final String preferredLocale) {
        final String preferred = normalizeLocale(preferredLocale);
        final String fromPreferred = read(preferred, path);
        if (fromPreferred != null) {
            return fromPreferred;
        }
        final String fromDefault = read(defaultLocale, path);
        if (fromDefault != null) {
            return fromDefault;
        }
        final String fromEnglish = read(ENGLISH_LOCALE, path);
        if (fromEnglish != null) {
            return fromEnglish;
        }
        return fallback;
    }

    public String getForSender(final CommandSender sender, final String path, final String fallback) {
        return get(path, fallback, resolveSenderLocale(sender));
    }

    public List<String> getStringList(
            final String path,
            final List<String> fallback,
            final String preferredLocale) {
        final String preferred = normalizeLocale(preferredLocale);
        final List<String> fromPreferred = readList(preferred, path);
        if (fromPreferred != null) {
            return fromPreferred;
        }
        final List<String> fromDefault = readList(defaultLocale, path);
        if (fromDefault != null) {
            return fromDefault;
        }
        final List<String> fromEnglish = readList(ENGLISH_LOCALE, path);
        if (fromEnglish != null) {
            return fromEnglish;
        }
        return fallback;
    }

    public List<String> getStringListForSender(
            final CommandSender sender,
            final String path,
            final List<String> fallback) {
        return getStringList(path, fallback, resolveSenderLocale(sender));
    }

    private String read(final String locale, final String path) {
        final FileConfiguration cfg = bundles.get(normalizeLocale(locale));
        return cfg == null ? null : cfg.getString(path);
    }

    private List<String> readList(final String locale, final String path) {
        final FileConfiguration cfg = bundles.get(normalizeLocale(locale));
        if (cfg == null || !cfg.isList(path)) {
            return null;
        }
        final List<String> result = cfg.getStringList(path);
        return result.isEmpty() ? null : result;
    }
}

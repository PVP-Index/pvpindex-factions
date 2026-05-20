package com.pvpindex.factions.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MessagesConfig locale fallback")
class MessagesConfigTest {

    @Test
    @DisplayName("uses preferred locale when key exists")
    void usesPreferredLocale() {
        final YamlConfiguration en = new YamlConfiguration();
        en.set("general.no-permission", "EN");
        final YamlConfiguration de = new YamlConfiguration();
        de.set("general.no-permission", "DE");
        final MessagesConfig cfg = new MessagesConfig(Map.of("en", en, "de", de), "en");

        assertEquals("DE", cfg.get("general.no-permission", "fallback", "de"));
    }

    @Test
    @DisplayName("falls back to server default when preferred missing")
    void fallsBackToDefaultLocale() {
        final YamlConfiguration en = new YamlConfiguration();
        en.set("general.no-permission", "EN");
        final YamlConfiguration fr = new YamlConfiguration();
        fr.set("general.no-permission", "FR");
        final MessagesConfig cfg = new MessagesConfig(Map.of("en", en, "fr", fr), "fr");

        assertEquals("FR", cfg.get("general.no-permission", "fallback", "de"));
    }

    @Test
    @DisplayName("falls back to english when default locale missing key")
    void fallsBackToEnglish() {
        final YamlConfiguration en = new YamlConfiguration();
        en.set("general.no-permission", "EN");
        final YamlConfiguration es = new YamlConfiguration();
        final MessagesConfig cfg = new MessagesConfig(Map.of("en", en, "es", es), "es");

        assertEquals("EN", cfg.get("general.no-permission", "fallback", "de"));
    }
}


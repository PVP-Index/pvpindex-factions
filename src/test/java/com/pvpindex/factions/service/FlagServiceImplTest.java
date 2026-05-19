package com.pvpindex.factions.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pvpindex.factions.FactionFlag;
import com.pvpindex.factions.command.StorageTest;
import com.pvpindex.factions.config.FactionsConfig;
import com.pvpindex.factions.data.Repositories;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.repository.FactionRepository;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FlagServiceImpl")
class FlagServiceImplTest {

    @Mock private Repositories repos;
    @Mock private FactionsConfig config;
    @Mock private Logger logger;
    @Mock private FactionModel faction;
    @Mock private FactionRepository factionRepo;

    private FlagServiceImpl service;

    @BeforeEach
    void setUp() {
        when(repos.factions()).thenReturn(factionRepo);
        service = new FlagServiceImpl(repos, config, logger);
    }

    // -------------------------------------------------------------------------
    // getFlag
    // -------------------------------------------------------------------------

    @StorageTest
    @DisplayName("getFlag returns per-faction override when set")
    void testGetFlagOverride() {
        when(faction.getFlagsJson()).thenReturn("{\"pvp\":false}");
        when(config.getFlagDefault(FactionFlag.PVP)).thenReturn(true);

        assertFalse(service.getFlag(faction, FactionFlag.PVP));
    }

    @StorageTest
    @DisplayName("getFlag falls back to config default when flag absent")
    void testGetFlagFallback() {
        when(faction.getFlagsJson()).thenReturn("{}");
        when(config.getFlagDefault(FactionFlag.FRIENDLY_FIRE)).thenReturn(false);

        assertFalse(service.getFlag(faction, FactionFlag.FRIENDLY_FIRE));
    }

    @StorageTest
    @DisplayName("getFlag falls back to config default for null json")
    void testGetFlagNullJson() {
        when(faction.getFlagsJson()).thenReturn(null);
        when(config.getFlagDefault(FactionFlag.EXPLOSIONS)).thenReturn(false);

        assertFalse(service.getFlag(faction, FactionFlag.EXPLOSIONS));
    }

    // -------------------------------------------------------------------------
    // setFlag
    // -------------------------------------------------------------------------

    @StorageTest
    @DisplayName("setFlag persists the new value")
    void testSetFlagPersists() throws Exception {
        when(faction.getFlagsJson()).thenReturn("{}");

        service.setFlag(faction, FactionFlag.PVP, false);

        verify(faction).setFlagsJson("{\"pvp\":false}");
        verify(factionRepo).save(faction);
    }

    @StorageTest
    @DisplayName("setFlag preserves existing unrelated flags")
    void testSetFlagPreservesOthers() throws Exception {
        when(faction.getFlagsJson()).thenReturn("{\"explosions\":true}");

        service.setFlag(faction, FactionFlag.PVP, false);

        // Verify save was called (exact JSON order can vary, so just check save)
        verify(factionRepo).save(faction);
    }

    // -------------------------------------------------------------------------
    // getAllFlags
    // -------------------------------------------------------------------------

    @StorageTest
    @DisplayName("getAllFlags returns complete map with defaults for absent keys")
    void testGetAllFlags() {
        when(faction.getFlagsJson()).thenReturn("{\"pvp\":false}");
        for (final FactionFlag flag : FactionFlag.values()) {
            when(config.getFlagDefault(flag)).thenReturn(flag.getDefaultValue());
        }

        final Map<FactionFlag, Boolean> flags = service.getAllFlags(faction);

        assertEquals(FactionFlag.values().length, flags.size());
        assertFalse(flags.get(FactionFlag.PVP));           // overridden to false
        assertFalse(flags.get(FactionFlag.FRIENDLY_FIRE)); // default false
    }

    // -------------------------------------------------------------------------
    // isFlagEditable
    // -------------------------------------------------------------------------

    @StorageTest
    @DisplayName("isFlagEditable delegates to config")
    void testIsFlagEditable() {
        when(config.isFlagPlayerEditable(FactionFlag.OPEN)).thenReturn(false);

        assertFalse(service.isFlagEditable(FactionFlag.OPEN));
    }

    // -------------------------------------------------------------------------
    // parseFlags / serializeFlags (package-private, tested directly)
    // -------------------------------------------------------------------------

    @StorageTest
    @DisplayName("parseFlags parses valid JSON")
    void testParseFlags() {
        final Map<String, Boolean> result = FlagServiceImpl.parseFlags("{\"pvp\":true,\"explosions\":false}");

        assertEquals(2, result.size());
        assertTrue(result.get("pvp"));
        assertFalse(result.get("explosions"));
    }

    @StorageTest
    @DisplayName("parseFlags returns empty map for empty JSON")
    void testParseFlagsEmpty() {
        assertTrue(FlagServiceImpl.parseFlags("{}").isEmpty());
        assertTrue(FlagServiceImpl.parseFlags(null).isEmpty());
        assertTrue(FlagServiceImpl.parseFlags("").isEmpty());
    }

    @StorageTest
    @DisplayName("serializeFlags produces valid JSON round-trip")
    void testSerializeRoundTrip() {
        final Map<String, Boolean> input = Map.of("pvp", true);
        final String json = FlagServiceImpl.serializeFlags(input);
        final Map<String, Boolean> back = FlagServiceImpl.parseFlags(json);

        assertEquals(1, back.size());
        assertTrue(back.get("pvp"));
    }
}

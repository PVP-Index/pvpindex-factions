package com.pvpindex.factions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pvpindex.factions.data.model.AutoTerritoryMode;
import com.pvpindex.factions.data.model.PlayerModel;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerModelTest {

    @Test
    void testAutoTerritoryModeDefaultsToOff() {
        final PlayerModel model = new PlayerModel(UUID.randomUUID().toString());
        assertEquals(AutoTerritoryMode.OFF, model.getAutoTerritoryMode());
    }

    @Test
    void testAutoTerritoryModeRoundTrip() {
        final PlayerModel model = new PlayerModel(UUID.randomUUID().toString());
        model.setAutoTerritoryMode(AutoTerritoryMode.CLAIM);
        assertEquals(AutoTerritoryMode.CLAIM, model.getAutoTerritoryMode());
        model.setAutoTerritoryMode(AutoTerritoryMode.UNCLAIM);
        assertEquals(AutoTerritoryMode.UNCLAIM, model.getAutoTerritoryMode());
    }

    @Test
    void testNotNullBackedDefaultsAreInitialized() {
        final PlayerModel model = new PlayerModel(UUID.randomUUID().toString());
        assertEquals(0.0, model.getPowerBoost());
        assertEquals(0.0, model.getPower());
        assertEquals(0L, model.getJoinedAt());
        assertEquals(0L, model.getLastActivity());
        assertFalse(model.isOverriding());
        assertTrue(model.hasTerritoryTitles());
        assertTrue(model.hasInviteNotifications());
        assertTrue(model.hasBankTaxNotifications());
        assertEquals(AutoTerritoryMode.OFF, model.getAutoTerritoryMode());
    }
}

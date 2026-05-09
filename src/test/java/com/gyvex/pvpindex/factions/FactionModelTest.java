package com.gyvex.pvpindex.factions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gyvex.pvpindex.factions.data.model.FactionModel;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FactionModelTest {

    @Test
    void testDefaultValues() {
        final FactionModel model = new FactionModel(UUID.randomUUID().toString());
        assertEquals(0.0, model.getBank());
        assertEquals(0.0, model.getPowerBoost());
        // Jaloquent returns empty string (not null) for unset string fields
        assertTrue(model.getOwnerId() == null || model.getOwnerId().isEmpty());
        assertTrue(model.getName() == null || model.getName().isEmpty());
    }

    @Test
    void testSettersAndGetters() {
        final String id = UUID.randomUUID().toString();
        final FactionModel model = new FactionModel(id);
        model.setName("TestFaction");
        model.setOwnerId(UUID.randomUUID().toString());
        model.setBank(100.0);
        model.setPowerBoost(5.0);
        model.setCreatedAt(System.currentTimeMillis());

        assertEquals(id, model.getId());
        assertEquals("TestFaction", model.getName());
        assertEquals(100.0, model.getBank());
        assertEquals(5.0, model.getPowerBoost());
        assertTrue(model.isNormal());
    }

    @Test
    void testSentinelIdChecks() {
        final FactionModel safezone = new FactionModel(FactionModel.SAFEZONE_ID);
        final FactionModel warzone = new FactionModel(FactionModel.WARZONE_ID);
        final FactionModel wilderness = new FactionModel(FactionModel.WILDERNESS_ID);

        assertFalse(safezone.isNormal());
        assertFalse(warzone.isNormal());
        assertFalse(wilderness.isNormal());
    }

    @Test
    void testHasHomeReturnsFalseByDefault() {
        final FactionModel model = new FactionModel(UUID.randomUUID().toString());
        assertFalse(model.hasHome());
    }

    @Test
    void testHasHomeReturnsTrueWhenSet() {
        final FactionModel model = new FactionModel(UUID.randomUUID().toString());
        model.setHomeWorld("world");
        model.setHomeX(0.0);
        model.setHomeY(64.0);
        model.setHomeZ(0.0);
        assertTrue(model.hasHome());
    }
}

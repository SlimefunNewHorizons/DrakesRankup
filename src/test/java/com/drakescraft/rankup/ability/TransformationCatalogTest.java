package com.drakescraft.rankup.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TransformationCatalogTest {

    @Test
    void nothingUnlockedBelowTheFirstTier() {
        assertTrue(TransformationCatalog.unlocked(1).isEmpty());
        assertNull(TransformationCatalog.next(null, 1));
    }

    @Test
    void cycleVisitsEveryUnlockedOnceThenReturnsToNone() {
        int tier = 100;
        List<String> expected = TransformationCatalog.unlocked(tier);
        String current = null;
        for (String key : expected) {
            current = TransformationCatalog.next(current, tier);
            assertEquals(key, current);
        }
        assertNull(TransformationCatalog.next(current, tier));
    }

    @Test
    void lowerTierOnlyRotatesThroughWhatItOwns() {
        List<String> at30 = TransformationCatalog.unlocked(30);
        assertTrue(at30.contains("GOMU_GOMU"));
        assertTrue(at30.contains("HAKI_CONQUEROR"));
        assertTrue(at30.stream().noneMatch(k -> TransformationCatalog.requiredTier(k) > 30));
    }

    @Test
    void unknownCurrentRestartsFromTheFirst() {
        assertEquals(TransformationCatalog.unlocked(100).get(0), TransformationCatalog.next("NO_EXISTE", 100));
    }

    @Test
    void requiredTierIsCaseInsensitiveAndUnknownIsUnreachable() {
        assertEquals(30, TransformationCatalog.requiredTier("gomu_gomu"));
        assertEquals(Integer.MAX_VALUE, TransformationCatalog.requiredTier("NADA"));
    }
}

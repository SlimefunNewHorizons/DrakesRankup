package com.drakescraft.rankup.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.drakescraft.rankup.model.AbilityType;
import org.junit.jupiter.api.Test;

class TechniqueGateTest {

    @Test
    void equippedTransformationBlocksEveryOtherTechnique() {
        // Tier 100 con el Gear 4 equipado: solo el Gear 4 dispara, no el Golpe Serio ni el Hakai.
        assertTrue(TechniqueGate.permits("GOMU_GOMU", null, TechniqueGate.GOMU));
        assertFalse(TechniqueGate.permits("GOMU_GOMU", null, TechniqueGate.SERIOUS_PUNCH));
        assertFalse(TechniqueGate.permits("GOMU_GOMU", null, TechniqueGate.HAKAI));
        assertFalse(TechniqueGate.permits("GOMU_GOMU", null, TechniqueGate.ZENO_ERASE));
        assertFalse(TechniqueGate.permits("GOMU_GOMU", null, TechniqueGate.MAGNET));
    }

    @Test
    void withoutEquippedOnlyThePrimaryAbilityFires() {
        String primary = TechniqueGate.primaryKey(AbilityType.SERIOUS_PUNCH);
        assertTrue(TechniqueGate.permits(null, primary, TechniqueGate.SERIOUS_PUNCH));
        assertFalse(TechniqueGate.permits(null, primary, TechniqueGate.GOMU));
        assertFalse(TechniqueGate.permits("", primary, TechniqueGate.HAKAI));
        assertFalse(TechniqueGate.permits("  ", primary, TechniqueGate.HAKAI));
    }

    @Test
    void rankWithNoTechniqueAbilityFiresNothing() {
        assertNull(TechniqueGate.primaryKey(AbilityType.SSJ_BLUE));
        assertNull(TechniqueGate.primaryKey(null));
        assertFalse(TechniqueGate.permits((String) null, (String) null, TechniqueGate.GOMU));
        assertFalse(TechniqueGate.permits((String) null, (String) null, TechniqueGate.SERIOUS_PUNCH));
    }

    @Test
    void legacyAliasesStillMatch() {
        assertTrue(TechniqueGate.permits("gear_4", null, TechniqueGate.GOMU));
        assertTrue(TechniqueGate.permits("SAITAMA", null, TechniqueGate.SERIOUS_PUNCH));
        assertTrue(TechniqueGate.permits("ULTRA_EGO", null, TechniqueGate.HAKAI));
        assertTrue(TechniqueGate.permits("GEAR_FIVE", null, TechniqueGate.CONQUEROR));
    }

    @Test
    void exactlyOneTechniqueFiresForEveryEquippedKey() {
        String[][] all = {TechniqueGate.GOMU, TechniqueGate.SERIOUS_PUNCH, TechniqueGate.HAKAI,
                TechniqueGate.ZENO_ERASE, TechniqueGate.MAGNET};
        for (String equipped : new String[]{"GOMU_GOMU", "SERIOUS_PUNCH", "HAKAI", "ZENO_ERASE", "JIKI_MAGNET"}) {
            int fired = 0;
            for (String[] keys : all) {
                if (TechniqueGate.permits(equipped, null, keys)) {
                    fired++;
                }
            }
            assertEquals(1, fired, equipped);
        }
    }

    @Test
    void primaryKeysMapEachTechniqueAbility() {
        assertEquals("GOMU_GOMU", TechniqueGate.primaryKey(AbilityType.DEVIL_FRUIT_GOMU));
        assertEquals("HAKAI", TechniqueGate.primaryKey(AbilityType.HAKAI_AURA));
        assertEquals("HAKAI", TechniqueGate.primaryKey(AbilityType.ULTRA_EGO));
        assertEquals("ZENO_ERASE", TechniqueGate.primaryKey(AbilityType.KAMI_DIVINE));
        assertEquals("JIKI_MAGNET", TechniqueGate.primaryKey(AbilityType.MAGNETIC_ATTRACTOR));
        assertEquals("HAKI_CONQUEROR", TechniqueGate.primaryKey(AbilityType.HAKI_CONQUEROR));
    }
}

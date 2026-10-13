package com.drakescraft.rankup.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JobsBoostTest {

    @Test
    void tierAndRebirthsAddUp() {
        // tier 100 * 0.5% = +50%, 10 renacimientos * 1% = +10%
        assertEquals(1.6, JobsBoost.multiplier(100, 10, 0.005, 0.01, 1.0, 3.0), 1e-9);
    }

    @Test
    void vipMultipliesTheRankBonus() {
        assertEquals(1.5 * 2.0, JobsBoost.multiplier(100, 0, 0.005, 0.01, 2.0, 10.0), 1e-9);
    }

    @Test
    void capLimitsTheTotal() {
        assertEquals(3.0, JobsBoost.multiplier(100, 50, 0.005, 0.01, 4.0, 3.0), 1e-9);
    }

    @Test
    void noTierMeansNoBonus() {
        assertEquals(1.0, JobsBoost.multiplier(0, 0, 0.005, 0.01, 1.0, 3.0), 1e-9);
    }

    @Test
    void invalidInputsNeverReduceBelowOne() {
        assertEquals(1.0, JobsBoost.multiplier(-5, -3, -1.0, -1.0, 0.0, 0.5), 1e-9);
        assertEquals(1.0, JobsBoost.multiplier(50, 0, 0.005, 0.01, -2.0, 1.0), 1e-9);
    }
}

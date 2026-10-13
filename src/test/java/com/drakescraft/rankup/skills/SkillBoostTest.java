package com.drakescraft.rankup.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SkillBoostTest {

    @Test
    void tierAndRebirthsAddUpToWholePercent() {
        // tier 100 * 0.3 = 30, 10 renacimientos * 0.5 = 5
        assertEquals(35, SkillBoost.percent(100, 10, 0.3, 0.5, 60.0));
    }

    @Test
    void capLimitsTheBonus() {
        assertEquals(40, SkillBoost.percent(100, 50, 0.3, 0.5, 40.0));
    }

    @Test
    void noRankMeansNoBonus() {
        assertEquals(0, SkillBoost.percent(0, 0, 0.3, 0.5, 60.0));
    }

    @Test
    void invalidInputsNeverGoNegative() {
        assertEquals(0, SkillBoost.percent(-10, -4, -1.0, -1.0, 60.0));
        assertEquals(0, SkillBoost.percent(50, 5, 0.3, 0.5, -5.0));
    }

    @Test
    void roundsToTheNearestWholePercent() {
        assertEquals(15, SkillBoost.percent(50, 0, 0.3, 0.5, 60.0));
        assertEquals(8, SkillBoost.percent(25, 0, 0.3, 0.5, 60.0)); // 7.5 -> 8
    }
}

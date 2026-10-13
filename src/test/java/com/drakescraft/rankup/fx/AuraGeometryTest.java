package com.drakescraft.rankup.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class AuraGeometryTest {

    @Test
    void helixRisesFromFeetToTheTopAndNarrows() {
        AuraGeometry.Point bottom = AuraGeometry.helix(0.8, 2.4, 2.0, 0.0, 0.0, 0, 2);
        AuraGeometry.Point top = AuraGeometry.helix(0.8, 2.4, 2.0, 1.0, 0.0, 0, 2);
        assertEquals(0.0, bottom.y(), 1e-9);
        assertEquals(2.4, top.y(), 1e-9);
        assertEquals(0.8, Math.hypot(bottom.x(), bottom.z()), 1e-9);
        assertTrue(Math.hypot(top.x(), top.z()) < 0.8);
    }

    @Test
    void strandsAreSpreadEvenlyAroundTheBody() {
        AuraGeometry.Point a = AuraGeometry.helix(1.0, 2.0, 1.0, 0.0, 0.0, 0, 2);
        AuraGeometry.Point b = AuraGeometry.helix(1.0, 2.0, 1.0, 0.0, 0.0, 1, 2);
        assertEquals(-a.x(), b.x(), 1e-9);
        assertEquals(-a.z(), b.z(), 1e-9);
    }

    @Test
    void progressIsClamped() {
        AuraGeometry.Point below = AuraGeometry.helix(1.0, 2.0, 1.0, -3.0, 0.0, 0, 1);
        AuraGeometry.Point above = AuraGeometry.helix(1.0, 2.0, 1.0, 9.0, 0.0, 0, 1);
        assertEquals(0.0, below.y(), 1e-9);
        assertEquals(2.0, above.y(), 1e-9);
    }

    @Test
    void ringHasTheRequestedPointsAllAtTheRadiusAndHeight() {
        List<AuraGeometry.Point> ring = AuraGeometry.ring(0.9, 12, 0.3, 0.5);
        assertEquals(12, ring.size());
        for (AuraGeometry.Point p : ring) {
            assertEquals(0.9, Math.hypot(p.x(), p.z()), 1e-9);
            assertEquals(0.5, p.y(), 1e-9);
        }
        assertEquals(3, AuraGeometry.ring(1.0, 1, 0.0, 0.0).size());
    }

    @Test
    void shockwaveGrowsMonotonicallyAndStopsAtTheMaximum() {
        double previous = -1;
        for (int i = 0; i <= 10; i++) {
            AuraGeometry.Point p = AuraGeometry.shockwave(5.0, 8, i / 10.0).get(0);
            double r = Math.hypot(p.x(), p.z());
            assertTrue(r >= previous);
            previous = r;
        }
        assertEquals(5.0, previous, 1e-9);
    }
}

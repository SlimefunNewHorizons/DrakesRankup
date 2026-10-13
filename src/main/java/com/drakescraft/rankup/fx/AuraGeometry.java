package com.drakescraft.rankup.fx;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometria pura de las auras (sin Bukkit) para poder probarla. Las coordenadas son relativas al
 * centro de los pies del jugador: x/z en el plano, y hacia arriba.
 */
public final class AuraGeometry {

    /** Un punto relativo al jugador. */
    public record Point(double x, double y, double z) {}

    private AuraGeometry() {}

    /** Punto de una hebra de helice: sube de 0 a {@code height} mientras da {@code turns} vueltas. */
    public static Point helix(double radius, double height, double turns, double progress,
                              double phase, int strand, int strands) {
        double p = clamp01(progress);
        int count = Math.max(1, strands);
        double angle = phase + p * turns * Math.PI * 2.0 + (Math.PI * 2.0 * strand) / count;
        // La hebra se estrecha hacia arriba como una llama.
        double r = radius * (1.0 - 0.55 * p);
        return new Point(r * Math.cos(angle), height * p, r * Math.sin(angle));
    }

    /** Puntos equiespaciados de un anillo horizontal a la altura {@code y}. */
    public static List<Point> ring(double radius, int points, double phase, double y) {
        int n = Math.max(3, points);
        List<Point> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double a = phase + (Math.PI * 2.0 * i) / n;
            result.add(new Point(radius * Math.cos(a), y, radius * Math.sin(a)));
        }
        return result;
    }

    /** Anillo de onda expansiva: radio que crece de 0 a {@code maxRadius} con {@code progress}. */
    public static List<Point> shockwave(double maxRadius, int points, double progress) {
        double p = clamp01(progress);
        // Salida rapida y frenada al final, como una onda real.
        double eased = 1.0 - (1.0 - p) * (1.0 - p);
        return ring(maxRadius * eased, points, 0.0, 0.1);
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : Math.min(v, 1.0);
    }
}

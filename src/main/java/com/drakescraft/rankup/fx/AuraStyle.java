package com.drakescraft.rankup.fx;

import org.bukkit.Color;
import org.bukkit.Particle;

/**
 * Aspecto de un aura de transformacion: dos colores (la hebra pasa de uno a otro al subir), la
 * particula de chispas y la forma. Cada transformacion tiene el suyo; ya no se mezclan con las
 * auras pasivas de rango.
 */
public enum AuraStyle {

    SSJ_2(Color.fromRGB(255, 215, 40), Color.fromRGB(255, 250, 200), Particle.ELECTRIC_SPARK, 2, 0.75, 2.3, true),
    SSJ_GOD(Color.fromRGB(230, 25, 40), Color.fromRGB(255, 130, 90), Particle.FLAME, 2, 0.7, 2.2, true),
    SSJ_BLUE(Color.fromRGB(40, 140, 255), Color.fromRGB(190, 240, 255), Particle.ELECTRIC_SPARK, 3, 0.75, 2.4, true),
    MUI(Color.fromRGB(200, 205, 225), Color.fromRGB(255, 255, 255), Particle.END_ROD, 3, 0.8, 2.5, true),
    ULTRA_EGO(Color.fromRGB(120, 30, 190), Color.fromRGB(235, 90, 230), Particle.WITCH, 2, 0.7, 2.2, true),
    GOHAN_BEAST(Color.fromRGB(190, 40, 200), Color.fromRGB(255, 255, 255), Particle.ELECTRIC_SPARK, 2, 0.75, 2.3, true),
    BROLY(Color.fromRGB(60, 220, 80), Color.fromRGB(230, 255, 120), Particle.HAPPY_VILLAGER, 2, 0.95, 2.6, true),
    KAIOKEN(Color.fromRGB(240, 20, 30), Color.fromRGB(255, 120, 60), Particle.FLAME, 2, 0.65, 2.0, false),
    DIVINE(Color.fromRGB(255, 240, 190), Color.fromRGB(255, 190, 70), Particle.END_ROD, 3, 0.6, 2.4, true);

    private final Color primary;
    private final Color secondary;
    private final Particle spark;
    private final int strands;
    private final double radius;
    private final double height;
    private final boolean groundRing;

    AuraStyle(Color primary, Color secondary, Particle spark, int strands, double radius, double height,
              boolean groundRing) {
        this.primary = primary;
        this.secondary = secondary;
        this.spark = spark;
        this.strands = strands;
        this.radius = radius;
        this.height = height;
        this.groundRing = groundRing;
    }

    public Color primary() { return primary; }
    public Color secondary() { return secondary; }
    public Particle spark() { return spark; }
    public int strands() { return strands; }
    public double radius() { return radius; }
    public double height() { return height; }
    public boolean groundRing() { return groundRing; }
}

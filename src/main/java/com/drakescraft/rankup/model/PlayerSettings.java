package com.drakescraft.rankup.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerSettings {
    private boolean particlesEnabled = true;
    private boolean kineticPushEnabled = true;
    private boolean abilitiesEnabled = true;
    private String activeTransformation = null;
    private boolean kiFlightEnabled = true;

    /**
     * Intensidad de las auras: 2 = completa, 1 = reducida (menos densa y menos
     * frecuente), 0 = apagada. Convive con particlesEnabled por retrocompatibilidad:
     * el boolean sigue siendo la puerta on/off y el nivel afina la intensidad.
     */
    private int particleLevel = 2;

    /**
     * Contador de Renacimientos (Rebirths): máximo 50.
     * Otorga +3% de daño permanente y -1% de enfriamiento en habilidades por nivel.
     */
    private int rebirthCount = 0;

    // ==========================================
    // CONFIGURACIÓN DE SUPERPOSICIÓN DE HABILIDADES
    // Permite al jugador activar/desactivar individualmente cada técnica de combate
    // evitando que todo se active a la vez o interfiera con el combate normal.
    // ==========================================
    private boolean seriousPunchEnabled = true;    // Golpe Serio de Saitama (Tier 41 / 50+)
    private boolean santoryuEnabled = true;       // Estilo Tres Espadas de Zoro (Tier 24+)
    private boolean devilFruitEnabled = true;     // Habilidades activas de Frutas del Diablo (Tier 30+)
    private boolean sonicLeapEnabled = true;      // Super Impulso Sónico de 100 bloques (Doble Salto)
    private boolean cursedEnergyEnabled = true;   // Destello Negro & Desmantelar (Jujutsu Tiers 20+)
    private boolean dancingBladesEnabled = false; // Filos Danzantes / Aura Kill (Tier 70+)
    private boolean magnetVortexEnabled = false;  // Vórtice Magnético (Tier 60+)

    public PlayerSettings(boolean particlesEnabled, boolean kineticPushEnabled, boolean abilitiesEnabled) {
        this.particlesEnabled = particlesEnabled;
        this.kineticPushEnabled = kineticPushEnabled;
        this.abilitiesEnabled = abilitiesEnabled;
        this.activeTransformation = null;
        this.kiFlightEnabled = true;
        this.rebirthCount = 0;
        this.seriousPunchEnabled = true;
        this.santoryuEnabled = true;
        this.devilFruitEnabled = true;
        this.sonicLeapEnabled = true;
        this.cursedEnergyEnabled = true;
        this.dancingBladesEnabled = false;
        this.magnetVortexEnabled = false;
    }
}

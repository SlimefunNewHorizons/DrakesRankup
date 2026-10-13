package com.drakescraft.rankup.skills;

/**
 * Calculo puro del bono de XP de skills de AuraSkills segun el rango. Sin Bukkit para poder
 * probarlo. AuraSkills lee el multiplicador del permiso {@code auraskills.multiplier.<porcentaje>}.
 */
public final class SkillBoost {

    private SkillBoost() {}

    /** Porcentaje entero de bono: tier * porTier + renacimientos * porRenacimiento, con tope. */
    public static int percent(int tier, int rebirths, double perTier, double perRebirth, double capPercent) {
        double raw = Math.max(0, tier) * Math.max(0.0, perTier) + Math.max(0, rebirths) * Math.max(0.0, perRebirth);
        double cap = Math.max(0.0, capPercent);
        return (int) Math.round(Math.min(raw, cap));
    }
}

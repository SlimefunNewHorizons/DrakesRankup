package com.drakescraft.rankup.jobs;

/**
 * Calculo puro del bono de dinero de Jobs. Sin dependencias de Bukkit para poder probarlo.
 *
 * Multiplicador = (1 + tier * porTier + renacimientos * porRenacimiento) * multiplicadorVip,
 * limitado por un tope para que un rango alto con booster no desboque la economia.
 */
public final class JobsBoost {

    private JobsBoost() {}

    public static double multiplier(int tier, int rebirths, double perTier, double perRebirth,
                                    double vipMultiplier, double cap) {
        double rank = 1.0 + Math.max(0, tier) * Math.max(0.0, perTier)
                + Math.max(0, rebirths) * Math.max(0.0, perRebirth);
        double vip = vipMultiplier > 0.0 ? vipMultiplier : 1.0;
        double total = rank * vip;
        double max = cap >= 1.0 ? cap : 1.0;
        return Math.min(total, max);
    }
}

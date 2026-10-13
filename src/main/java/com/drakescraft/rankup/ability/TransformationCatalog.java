package com.drakescraft.rankup.ability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orden fijo de las transformaciones y tecnicas equipables, con el tier que las desbloquea.
 * Lo usa el selector rapido (Shift + F) para rotar entre las que el jugador ya tiene; el menu
 * {@code /transformaciones} muestra las mismas claves.
 */
public final class TransformationCatalog {

    private static final Map<String, Integer> REQUIRED_TIER = new LinkedHashMap<>();

    static {
        REQUIRED_TIER.put("SSJ_2", 33);
        REQUIRED_TIER.put("BROLY_LSSJ", 34);
        REQUIRED_TIER.put("SSJ_GOD", 35);
        REQUIRED_TIER.put("SSJ_BLUE", 36);
        REQUIRED_TIER.put("GOHAN_BEAST", 45);
        REQUIRED_TIER.put("MUI", 46);
        REQUIRED_TIER.put("ULTRA_EGO", 47);
        REQUIRED_TIER.put("VEGETTO_SWORD", 48);
        REQUIRED_TIER.put("KAIOKEN", 92);
        REQUIRED_TIER.put("SANTORYU", 24);
        REQUIRED_TIER.put("GOMU_GOMU", 30);
        REQUIRED_TIER.put("MERA_MERA", 55);
        REQUIRED_TIER.put("JIKI_MAGNET", 60);
        REQUIRED_TIER.put("OPE_OPE", 65);
        REQUIRED_TIER.put("DANCING_BLADES", 70);
        REQUIRED_TIER.put("PIKA_PIKA", 75);
        REQUIRED_TIER.put("GURA_GURA", 85);
        REQUIRED_TIER.put("GEAR_FIVE", 88);
        // Tecnicas de Shift + clic que antes se disparaban solas por tier.
        REQUIRED_TIER.put("HAKI_CONQUEROR", 30);
        REQUIRED_TIER.put("SERIOUS_PUNCH", 41);
        REQUIRED_TIER.put("HAKAI", 47);
        REQUIRED_TIER.put("ZENO_ERASE", 50);
    }

    private TransformationCatalog() {}

    public static int requiredTier(String key) {
        Integer tier = key == null ? null : REQUIRED_TIER.get(key.toUpperCase());
        return tier == null ? Integer.MAX_VALUE : tier;
    }

    /** Claves desbloqueadas para un tier, en el orden del catalogo. */
    public static List<String> unlocked(int tier) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : REQUIRED_TIER.entrySet()) {
            if (tier >= entry.getValue()) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Siguiente en la rotacion tras {@code current}: recorre las desbloqueadas y, despues de la
     * ultima, vuelve a "ninguna" ({@code null}); desde "ninguna" pasa a la primera. Si no hay
     * ninguna desbloqueada devuelve null.
     */
    public static String next(String current, int tier) {
        List<String> options = unlocked(tier);
        if (options.isEmpty()) {
            return null;
        }
        if (current == null || current.isBlank()) {
            return options.get(0);
        }
        int index = -1;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).equalsIgnoreCase(current)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return options.get(0);
        }
        return index + 1 < options.size() ? options.get(index + 1) : null;
    }
}

package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.AbilityType;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.entity.Player;

/**
 * Puerta unica de las tecnicas que se disparan con Shift + clic.
 *
 * Antes cada listener decidia por su cuenta con umbrales {@code tier >= N}; al nivel 100 se
 * cumplian todos a la vez y un solo gesto lanzaba varias tecnicas (por ejemplo el Gear 4 se
 * activaba al querer usar otra). Ahora solo se dispara la tecnica de la transformacion equipada
 * en {@code /transformaciones}; si no hay ninguna equipada, la habilidad principal del rango.
 */
public final class TechniqueGate {

    public static final String[] GOMU = {"GOMU_GOMU", "GEAR_4"};
    public static final String[] SERIOUS_PUNCH = {"SERIOUS_PUNCH", "SAITAMA"};
    public static final String[] HAKAI = {"HAKAI", "ULTRA_EGO"};
    public static final String[] ZENO_ERASE = {"ZENO_ERASE"};
    public static final String[] MAGNET = {"JIKI_MAGNET"};
    public static final String[] CONQUEROR = {"HAKI_CONQUEROR", "HAKI", "GEAR_FIVE"};

    private TechniqueGate() {}

    /** Clave de transformacion que corresponde a la habilidad principal de un rango, o null. */
    public static String primaryKey(AbilityType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case DEVIL_FRUIT_GOMU -> "GOMU_GOMU";
            case SERIOUS_PUNCH -> "SERIOUS_PUNCH";
            case HAKAI_AURA, ULTRA_EGO -> "HAKAI";
            case KAMI_DIVINE -> "ZENO_ERASE";
            case MAGNETIC_ATTRACTOR -> "JIKI_MAGNET";
            case HAKI_CONQUEROR -> "HAKI_CONQUEROR";
            default -> null;
        };
    }

    /**
     * Regla pura: con una transformacion equipada solo dispara la que coincide con alguna de las
     * claves; sin ninguna equipada, dispara la habilidad principal del rango.
     */
    public static boolean permits(String equipped, String primaryKey, String... keys) {
        if (equipped != null && !equipped.isBlank()) {
            return matches(equipped, keys);
        }
        return primaryKey != null && matches(primaryKey, keys);
    }

    public static boolean permits(DrakesRankupPlugin plugin, Player player, String... keys) {
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
        Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        String primary = rank != null ? primaryKey(rank.getAbilityType()) : null;
        return permits(settings.getActiveTransformation(), primary, keys);
    }

    private static boolean matches(String value, String[] keys) {
        for (String key : keys) {
            if (key.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}

package com.drakescraft.rankup.task;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AuraTask extends BukkitRunnable {

    private final DrakesRankupPlugin plugin;
    private double angle = 0;

    public AuraTask(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        angle += Math.PI / 10;
        if (angle >= Math.PI * 2 - 1e-9) angle = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.isWorldAllowed(player.getWorld())) {
                continue;
            }
            Location loc = player.getLocation();

            // Staff Angel Mode Double Halo Aura (takes precedence)
            if (plugin.getStaffManager() != null && plugin.getStaffManager().isAngel(player.getUniqueId())) {
                try {
                    double r = 0.55;
                    double x1 = r * Math.cos(angle);
                    double z1 = r * Math.sin(angle);
                    double x2 = r * Math.cos(angle + Math.PI);
                    double z2 = r * Math.sin(angle + Math.PI);

                    loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(x1, 2.25, z1), 1, 0, 0, 0, 0);
                    loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(x2, 2.25, z2), 1, 0, 0, 0, 0);
                    loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 0.15, 0), 1, 0.1, 0.05, 0.1, 0.01);
                } catch (Exception ignored) {}
                continue;
            }

            Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
            if (rank == null) continue;

            PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
            if (!settings.isParticlesEnabled() || settings.getParticleLevel() <= 0) continue;
            if (settings.getParticleLevel() == 1 && ((int) Math.round(angle / (Math.PI / 10)) % 2 != 0)) continue;

            int tier = rank.getTier();
            String activeTrans = settings.getActiveTransformation();
            int rebirths = settings.getRebirthCount();

            try {
                // Aura celestial de Rebirth permanente
                if (rebirths > 0) {
                    double rRad = 0.50;
                    double rx = rRad * Math.cos(angle * 1.5);
                    double rz = rRad * Math.sin(angle * 1.5);
                    loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(rx, 2.35, rz), 1, 0, 0, 0, 0.01);
                    if (rebirths >= 10) {
                        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(-rx, 2.35, -rz), 1, 0, 0, 0, 0);
                    }
                    if (rebirths >= 25) {
                        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(rx * 0.7, 2.45, rz * 0.7), 1, 0, 0, 0, 0);
                    }
                }

                // Active Dragon Ball & Anime Transformation Auras
                if (plugin.getDragonBallListener() != null) {
                    if (plugin.getDragonBallListener().isKaiokenActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0.8, 0), 6, 0.35, 0.5, 0.35, 0, new Particle.DustOptions(Color.fromRGB(255, 20, 30), 1.4f));
                        loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 0.3, 0), 4, 0.2, 0.3, 0.2, 0.03);
                        continue;
                    } else if (plugin.getDragonBallListener().isMuiActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1.0, 0), 4, 0.3, 0.5, 0.3, 0.05);
                        loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 0.2, 0), 3, 0.2, 0.1, 0.2, 0.02);
                        continue;
                    } else if (plugin.getDragonBallListener().isUltraEgoActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.WITCH, loc.clone().add(0, 0.8, 0), 5, 0.3, 0.5, 0.3, 0.05);
                        loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc.clone().add(0, 0.2, 0), 3, 0.2, 0.1, 0.2, 0.03);
                        continue;
                    } else if (plugin.getDragonBallListener().isGohanBeastActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.CRIMSON_SPORE, loc.clone().add(0, 0.8, 0), 5, 0.25, 0.4, 0.25, 0.05);
                        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1.1, 0), 3, 0.3, 0.4, 0.3, 0.05);
                        continue;
                    } else if (plugin.getDragonBallListener().isBrolyActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0, 0.5, 0), 6, 0.35, 0.5, 0.35, 0.02);
                        continue;
                    } else if (plugin.getDragonBallListener().isSSJBlueActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.3, 0), 4, 0.25, 0.4, 0.25, 0.03);
                        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 0.9, 0), 3, 0.3, 0.4, 0.3, 0.08);
                        continue;
                    } else if (plugin.getDragonBallListener().isSSJGodActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 0.4, 0), 5, 0.25, 0.4, 0.25, 0.03);
                        continue;
                    } else if (plugin.getDragonBallListener().isSSJ2Active(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0.8, 0), 6, 0.35, 0.5, 0.35, 0, new Particle.DustOptions(Color.fromRGB(255, 230, 20), 1.5f));
                        loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 0.4, 0), 4, 0.25, 0.4, 0.25, 0.03);
                        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 0.9, 0), 5, 0.35, 0.5, 0.35, 0.08);
                        continue;
                    }
                }

                // One Piece: Gear 5, Gear 4, Gear 3 y Frutas del Diablo
                if (plugin.getOnePieceListener() != null) {
                    if (plugin.getOnePieceListener().isGear5Active(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 1.8, 0), 5, 0.35, 0.15, 0.35, 0.02);
                        loc.getWorld().spawnParticle(Particle.WAX_OFF, loc.clone().add(0, 1.2, 0), 3, 0.3, 0.3, 0.3, 0.05);
                        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.5, 0), 2, 0.2, 0.3, 0.2, 0.01);
                        continue;
                    } else if (plugin.getOnePieceListener().isGear4Active(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0, 0.7, 0), 4, 0.3, 0.4, 0.3, 0.02);
                        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0.9, 0), 5, 0.35, 0.4, 0.35, 0, new Particle.DustOptions(Color.fromRGB(180, 20, 20), 1.3f));
                        continue;
                    } else if (plugin.getOnePieceListener().isGear3Active(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc.clone().add(0, 0.6, 0), 4, 0.6, 0.4, 0.6, 0.05);
                        loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.4, 0), 3, 0.5, 0.2, 0.5, 0.02);
                        continue;
                    } else if (plugin.getOnePieceListener().isMeraActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 0.6, 0), 6, 0.3, 0.4, 0.3, 0.03);
                        loc.getWorld().spawnParticle(Particle.LAVA, loc.clone().add(0, 0.2, 0), 2, 0.2, 0.1, 0.2, 0);
                        continue;
                    } else if (plugin.getOnePieceListener().isOpeActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 0.8, 0), 3, 0.25, 0.3, 0.25, 0.02);
                        continue;
                    } else if (plugin.getOnePieceListener().isPikaActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(0, 0.8, 0), 4, 0.25, 0.4, 0.25, 0.03);
                        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 1.0, 0), 3, 0.25, 0.4, 0.25, 0.05);
                        continue;
                    } else if (plugin.getOnePieceListener().isGuraActive(player.getUniqueId())) {
                        loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 0.6, 0), 5, 0.3, 0.4, 0.3, 0.04);
                        continue;
                    }
                }

                // Equipped Static Aura from Settings
                if ("GOMU_GOMU".equalsIgnoreCase(activeTrans)) {
                    loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc.clone().add(0, 0.3, 0), 2, 0.15, 0.3, 0.15, 0.01);
                } else if ("SANTORYU".equalsIgnoreCase(activeTrans)) {
                    loc.getWorld().spawnParticle(Particle.CRIT, loc.clone().add(0, 0.2, 0), 2, 0.2, 0.2, 0.2, 0.02);
                } else if ("KAIOKEN".equalsIgnoreCase(activeTrans)) {
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0.3, 0), 2, 0.2, 0.3, 0.2, 0, new Particle.DustOptions(Color.fromRGB(240, 20, 30), 1.2f));
                }

                // Auras pasivas por rango continuo (Tiers 1 a 100)
                spawnPassiveAura(loc, tier, rank.getParticleType());
            } catch (Exception ignored) {}
        }
    }

    /** Paleta (principal, secundario) segun el nombre de particula del rango en ranks.yml. */
    private static Color[] paleta(String clave) {
        String k = clave == null ? "" : clave.toUpperCase(java.util.Locale.ROOT);
        if (k.contains("GOLD") || k.contains("SOLAR") || k.contains("YELLOW") || k.contains("KI")) {
            return new Color[] {Color.fromRGB(255, 205, 60), Color.fromRGB(255, 120, 30)};
        } else if (k.contains("PURPLE") || k.contains("HAKAI") || k.contains("ARISE") || k.contains("ILLUSION") || k.contains("POISON") || k.contains("VORTEX")) {
            return new Color[] {Color.fromRGB(170, 80, 255), Color.fromRGB(70, 20, 140)};
        } else if (k.contains("BLUE") || k.contains("CYAN") || k.contains("FROST") || k.contains("FOX")) {
            return new Color[] {Color.fromRGB(80, 220, 255), Color.fromRGB(40, 90, 230)};
        } else if (k.contains("RED") || k.contains("BLOOD") || k.contains("CRIMSON") || k.contains("SNAKE") || k.contains("CURSED")) {
            return new Color[] {Color.fromRGB(255, 60, 60), Color.fromRGB(120, 10, 20)};
        } else if (k.contains("GREEN") || k.contains("EMERALD") || k.contains("CERO")) {
            return new Color[] {Color.fromRGB(90, 255, 140), Color.fromRGB(20, 120, 70)};
        } else if (k.contains("ORANGE") || k.contains("FLAME") || k.contains("HEAT") || k.contains("TAILS")) {
            return new Color[] {Color.fromRGB(255, 150, 40), Color.fromRGB(200, 40, 10)};
        } else if (k.contains("SHADOW") || k.contains("BLACK") || k.contains("DARK") || k.contains("THREAD")) {
            return new Color[] {Color.fromRGB(150, 110, 200), Color.fromRGB(15, 10, 25)};
        } else if (k.contains("WHITE") || k.contains("SILVER") || k.contains("SMOKE") || k.contains("GOD") || k.contains("DIVINE")) {
            return new Color[] {Color.fromRGB(245, 245, 255), Color.fromRGB(150, 170, 220)};
        }
        return new Color[] {Color.fromRGB(190, 150, 255), Color.fromRGB(255, 220, 120)};
    }

    private static void punto(Location l, Color a, Color b, float tam) {
        l.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, l, 1, 0, 0, 0, 0, new Particle.DustTransition(a, b, tam));
    }

    /**
     * Aura pasiva de los 100 tiers: la FORMA depende de la division (tier) y el COLOR del rango (ranks.yml `particle`).
     * Mas tier = mas forma: anillo en el suelo, motas orbitando, helice, halo, espiral, pilar, alas.
     */
    private void spawnPassiveAura(Location loc, int tier, String claveRango) {
        if (tier < 1) return;
        Color[] c = paleta(claveRango);
        Color a = c[0], b = c[1];
        var w = loc.getWorld();
        double yawRad = Math.toRadians(loc.getYaw());
        try {
            int banda = Math.min(9, (tier - 1) / 10);
            // 1) anillo en el suelo (todas las divisiones; mas grueso con el tier)
            int puntos = 4 + banda;
            for (int i = 0; i < puntos; i++) {
                double t = angle + i * (2 * Math.PI / puntos);
                punto(loc.clone().add(0.7 * Math.cos(t), 0.08, 0.7 * Math.sin(t)), a, b, 0.9f);
            }
            // 2) motas orbitando a la cintura (desde tier 11)
            if (banda >= 1) {
                int n = Math.min(4, 1 + banda / 2);
                for (int i = 0; i < n; i++) {
                    double t = -angle * 1.3 + i * (2 * Math.PI / n);
                    punto(loc.clone().add(0.55 * Math.cos(t), 1.0 + 0.15 * Math.sin(t * 2), 0.55 * Math.sin(t)), b, a, 0.8f);
                }
            }
            // 3) helice alrededor del cuerpo (tier 21+)
            if (banda >= 2) {
                int hebras = banda >= 3 ? 2 : 1;
                for (int h = 0; h < hebras; h++) {
                    double t = angle * 2 + h * Math.PI;
                    double y = 0.2 + ((angle / (2 * Math.PI)) + h * 0.5) % 1.0 * 1.8;
                    punto(loc.clone().add(0.45 * Math.cos(t), y, 0.45 * Math.sin(t)), a, b, 1.0f);
                }
            }
            // 4) halo sobre la cabeza (tier 41+)
            if (banda >= 4) {
                for (int i = 0; i < 6; i++) {
                    double t = angle + i * (Math.PI / 3);
                    punto(loc.clone().add(0.3 * Math.cos(t), 2.3, 0.3 * Math.sin(t)), Color.fromRGB(255, 235, 150), a, 0.7f);
                }
            }
            // 5) pilar de energia (tier 61+): columnas de particulas ascendentes
            if (banda >= 6) {
                w.spawnParticle(banda >= 8 ? Particle.END_ROD : Particle.SOUL_FIRE_FLAME,
                        loc.clone().add(0, 0.2, 0), 1, 0.15, 0.1, 0.15, 0.04 + banda * 0.005);
            }
            // 6) alas de particulas detras del jugador (tier 81+)
            if (banda >= 8) {
                double fx = Math.sin(yawRad), fz = -Math.cos(yawRad); // frente
                double rx = Math.cos(yawRad), rz = Math.sin(yawRad);  // derecha
                double aleteo = 0.12 * Math.sin(angle * 2);
                for (int lado = -1; lado <= 1; lado += 2) {
                    for (int k = 1; k <= 4; k++) {
                        double lateral = lado * (0.25 + 0.28 * k);
                        double alto = 1.35 + 0.22 * k - 0.07 * k * k + aleteo * k;
                        double atras = -0.28 - 0.05 * k;
                        punto(loc.clone().add(rx * lateral + fx * atras, alto, rz * lateral + fz * atras), a, b, 1.1f);
                    }
                }
            }
            // 7) destello cosmico ocasional (tier 91+)
            if (banda >= 9 && Math.random() < 0.08) {
                w.spawnParticle(Particle.FLASH, loc.clone().add(0, 1.0, 0), 1, Color.WHITE);
            }
        } catch (Exception ignored) {}
    }
}

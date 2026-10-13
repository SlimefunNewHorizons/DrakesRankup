package com.drakescraft.rankup.fx;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.ability.DragonBallListener;
import com.drakescraft.rankup.ability.OnePieceListener;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Dibuja el aura de las transformaciones de Dragon Ball y la aura divina de los tiers 91-100 con
 * hebras en helice, un anillo en el suelo y chispas. Corre en el hilo principal cada 2 ticks para
 * que el movimiento sea fluido (la AuraTask original pintaba un solo golpe de particulas por
 * segundo) y solo trabaja para quien tiene una forma activa.
 *
 * El destello FLASH ya no se lanza en cada ciclo: antes los tiers 91+ lo disparaban continuamente
 * y tapaban la pantalla; ahora es un pulso cada tres segundos.
 *
 * Fase siguiente: modelos 3D con entidades display, siguiendo el patron de piezas etiquetadas por
 * dueno y limpieza de huerfanos del DisplaySuit de Chagui68 (MultiverseCreatures).
 */
public class AuraRenderTask extends BukkitRunnable {

    private static final int STRAND_TRAIL = 4;
    private static final double TRAIL_STEP = 0.07;
    private static final int PULSE_INTERVAL_RUNS = 30;
    /** El halo recibe una orden de giro cada 10 ejecuciones (medio segundo). */
    private static final int HALO_SYNC_RUNS = 10;

    private final DrakesRankupPlugin plugin;
    private long runs;
    private final AuraHalo halo = new AuraHalo();

    public AuraRenderTask(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        runs++;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.isWorldAllowed(player.getWorld())) {
                continue;
            }
            PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
            if (!settings.isParticlesEnabled() || settings.getParticleLevel() <= 0) {
                halo.remove(player);
                continue;
            }
            AuraStyle style = selectStyle(plugin, player);
            if (style == null) {
                halo.remove(player);
                continue;
            }
            if (settings.getParticleLevel() >= 2 && runs % HALO_SYNC_RUNS == 0) {
                halo.sync(player, style);
            }
            try {
                draw(player.getLocation(), style, settings.getParticleLevel());
            } catch (RuntimeException ignored) {
                // Una particula rechazada no debe tumbar la tarea del resto de jugadores.
            }
        }
    }

    /** Quita todos los halos (cierre del plugin). */
    public void shutdown() {
        halo.removeAll();
    }

    /** Quita el halo de un jugador que sale o cambia de mundo. */
    public void clear(Player player) {
        halo.remove(player);
    }

    /** Aura que corresponde al jugador ahora mismo, o null si no tiene ninguna de este sistema. */
    public static AuraStyle selectStyle(DrakesRankupPlugin plugin, Player player) {
        UUID uuid = player.getUniqueId();
        DragonBallListener db = plugin.getDragonBallListener();
        if (db != null) {
            if (db.isKaiokenActive(uuid)) return AuraStyle.KAIOKEN;
            if (db.isMuiActive(uuid)) return AuraStyle.MUI;
            if (db.isUltraEgoActive(uuid)) return AuraStyle.ULTRA_EGO;
            if (db.isGohanBeastActive(uuid)) return AuraStyle.GOHAN_BEAST;
            if (db.isBrolyActive(uuid)) return AuraStyle.BROLY;
            if (db.isSSJBlueActive(uuid)) return AuraStyle.SSJ_BLUE;
            if (db.isSSJGodActive(uuid)) return AuraStyle.SSJ_GOD;
            if (db.isSSJ2Active(uuid)) return AuraStyle.SSJ_2;
        }
        Rank rank = plugin.getRankManager().getPlayerRank(uuid);
        if (rank != null && rank.getTier() >= 91 && !hasOnePieceForm(plugin.getOnePieceListener(), uuid)) {
            return AuraStyle.DIVINE;
        }
        return null;
    }

    private static boolean hasOnePieceForm(OnePieceListener op, UUID uuid) {
        return op != null && (op.isGear5Active(uuid) || op.isGear4Active(uuid) || op.isGear3Active(uuid)
                || op.isMeraActive(uuid) || op.isOpeActive(uuid) || op.isPikaActive(uuid)
                || op.isGuraActive(uuid));
    }

    private void draw(Location base, AuraStyle style, int level) {
        World world = base.getWorld();
        if (world == null) {
            return;
        }
        boolean full = level >= 2;
        Particle.DustTransition dust = new Particle.DustTransition(style.primary(), style.secondary(), 1.2f);

        int strands = full ? style.strands() : 1;
        double sweep = (runs * TRAIL_STEP * 0.8) % 1.0;
        for (int s = 0; s < strands; s++) {
            for (int k = 0; k < STRAND_TRAIL; k++) {
                double progress = ((sweep + (double) s / strands - k * TRAIL_STEP) % 1.0 + 1.0) % 1.0;
                AuraGeometry.Point p = AuraGeometry.helix(style.radius(), style.height(), 2.2, progress,
                        runs * 0.25, s, strands);
                world.spawnParticle(Particle.DUST_COLOR_TRANSITION, base.getX() + p.x(), base.getY() + p.y(),
                        base.getZ() + p.z(), 1, 0, 0, 0, 0, dust);
            }
        }

        if (full && style.groundRing()) {
            for (AuraGeometry.Point p : AuraGeometry.ring(style.radius() + 0.25, 12, -runs * 0.18, 0.08)) {
                world.spawnParticle(Particle.DUST, base.getX() + p.x(), base.getY() + p.y(), base.getZ() + p.z(),
                        1, 0, 0, 0, 0, new Particle.DustOptions(style.primary(), 0.9f));
            }
        }

        world.spawnParticle(style.spark(), base.getX(), base.getY() + 1.0, base.getZ(), full ? 2 : 1,
                style.radius() * 0.4, 0.7, style.radius() * 0.4, 0.02);

        if (style == AuraStyle.DIVINE) {
            if (full) {
                for (AuraGeometry.Point p : AuraGeometry.ring(0.42, 6, runs * 0.3, 2.35)) {
                    world.spawnParticle(Particle.END_ROD, base.getX() + p.x(), base.getY() + p.y(),
                            base.getZ() + p.z(), 1, 0, 0, 0, 0);
                }
            }
            if (runs % PULSE_INTERVAL_RUNS == 0) {
                world.spawnParticle(Particle.FLASH, base.getX(), base.getY() + 0.2, base.getZ(), 1, 0, 0, 0, 0,
                        Color.WHITE);
            }
        }
    }
}

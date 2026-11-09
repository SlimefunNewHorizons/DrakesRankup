package com.drakescraft.rankup.gui;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.Rank;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Efectos y textos de progreso de rangos. Solo ASCII y caracteres basicos (Bedrock/Geyser),
 * pocas particulas y solo para quien sube (nada de spam global).
 */
public final class RankupFx {
    private RankupFx() {}

    /** Barra de progreso "||||||||||" con colores; 20 segmentos. */
    public static String bar(double frac, int len) {
        int on = (int) Math.round(Math.max(0, Math.min(1, frac)) * len);
        return "&a" + "|".repeat(on) + "&8" + "|".repeat(len - on);
    }

    /** Ingreso estimado por hora de un jugador activo en ese tier (modelo de la auditoria de economia). */
    public static double incomePerHour(int tier) {
        return 8000.0 * Math.pow(1.04, Math.max(0, tier - 1));
    }

    public static String eta(int currentTier, double missing) {
        if (missing <= 0) return "ahora";
        double h = missing / incomePerHour(currentTier);
        if (h < 1.0) return "~" + Math.max(1, Math.round(h * 60)) + " min";
        long hh = (long) h;
        long mm = Math.round((h - hh) * 60);
        return "~" + hh + " h" + (hh < 20 ? " " + mm + " min" : "");
    }

    /** Lineas de progreso hacia el siguiente rango: avance, falta y tiempo estimado. */
    public static java.util.List<String> progressLore(Rank next, int currentTier, double funds) {
        double frac = next.getCost() <= 0 ? 1 : funds / next.getCost();
        java.util.List<String> l = new java.util.ArrayList<>();
        l.add("&7Progreso: " + bar(frac, 20) + " &f" + (int) Math.min(100, frac * 100) + "%");
        if (funds >= next.getCost()) {
            l.add("&aTienes los fondos: puedes subir ahora.");
        } else {
            l.add("&7Te falta: &e$" + RankupMenu.MONEY_FORMAT.format(next.getCost() - funds));
            l.add("&7Tiempo estimado: &f" + eta(currentTier, next.getCost() - funds) + " &8(jugando activo)");
        }
        return l;
    }

    public static void celebrate(DrakesRankupPlugin plugin, Player p, Rank got, Rank next, double fundsAfter) {
        boolean milestone = got.getTier() % 10 == 0;
        Location loc = p.getLocation().add(0, 1.0, 0);
        try {
            p.playSound(loc, milestone ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, 1.0f, milestone ? 1.0f : 1.2f);
            p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, milestone ? 50 : 24, 0.5, 0.8, 0.5, 0.25);
            if (milestone) p.getWorld().spawnParticle(Particle.END_ROD, loc, 30, 0.6, 1.0, 0.6, 0.08);
        } catch (Exception ignored) {}
        if (next == null) {
            p.sendActionBar(Component.text("Rango maximo alcanzado"));
            return;
        }
        String line = "&7Siguiente: " + next.getDisplayName() + " &8| &6$" + RankupMenu.MONEY_FORMAT.format(next.getCost())
                + " &8| &f" + eta(got.getTier(), next.getCost() - fundsAfter);
        Component c = LegacyComponentSerializer.legacyAmpersand().deserialize(line);
        p.sendActionBar(c);
        BossBar bb = BossBar.bossBar(c, (float) Math.max(0, Math.min(1, fundsAfter / next.getCost())),
                milestone ? BossBar.Color.PURPLE : BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        p.showBossBar(bb);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) p.hideBossBar(bb); }, 140L);
    }
}

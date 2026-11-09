package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boss;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Techo unico de poder de los rangos (config.yml -> balance). Todo dano, efecto, empuje y
 * cooldown de las habilidades de rankup pasa por aqui: asi ningun rango supera a los VIP/Titanes
 * de la tienda ni a un equipo de Slimefun de tiempo medio, y no rompe jefes ni PvP.
 */
public final class Balance {
    private static DrakesRankupPlugin plugin;
    private static final Map<String, double[]> WINDOWS = new HashMap<>(); // atacante|victima -> {inicioMs, acumulado}
    private static final Map<UUID, Long> DODGE_CD = new HashMap<>();
    private static EntityDamageByEntityEvent lastBoost, lastReduce;
    private static double boostExtra, reduceFactor = 1.0;

    private Balance() {}

    public static void init(DrakesRankupPlugin p) { plugin = p; }

    private static double d(String key, double def) {
        return plugin == null ? def : plugin.getConfig().getDouble("balance." + key, def);
    }

    public static boolean isBoss(LivingEntity t) {
        return t instanceof Boss || t.getType().name().equals("WARDEN") || t.getMaxHealth() >= d("boss-health", 100);
    }

    /** Dano de habilidad con tope por tipo de victima y presupuesto por ventana de 5 s. */
    public static boolean hit(Player src, LivingEntity t, double dmg) {
        if (src == null || t == null || t.equals(src) || !t.isValid() || t.isDead()) return false;
        if (t instanceof ArmorStand || t instanceof Villager || t instanceof WanderingTrader) return false;
        if (t instanceof Tameable tm && tm.isTamed()) return false;
        boolean pvp = t instanceof Player;
        double cap, budget;
        if (pvp) {
            Player v = (Player) t;
            if (v.getGameMode() == GameMode.CREATIVE || v.getGameMode() == GameMode.SPECTATOR) return false;
            if (plugin != null && plugin.getProtectionGate() != null && plugin.getProtectionGate().isProtected(v.getLocation(), src)) return false;
            cap = d("pvp-hit-cap", 4); budget = d("pvp-window-cap", 6);
        } else if (isBoss(t)) {
            cap = d("boss-hit-cap", 6); budget = d("boss-window-cap", 12);
        } else {
            cap = d("pve-hit-cap", 14); budget = d("pve-window-cap", 28);
        }
        double use = Math.min(dmg, cap);
        long now = System.currentTimeMillis();
        if (WINDOWS.size() > 1500) WINDOWS.values().removeIf(w -> now - w[0] > 5000);
        double[] w = WINDOWS.computeIfAbsent(src.getUniqueId() + "|" + t.getUniqueId(), k -> new double[]{now, 0});
        if (now - w[0] > 5000) { w[0] = now; w[1] = 0; }
        use = Math.min(use, budget - w[1]);
        if (use <= 0.1) return false;
        w[1] += use;
        t.setMetadata("DRAKES_ABILITY_DAMAGE", new FixedMetadataValue(plugin, true));
        try { t.damage(use, src); } finally { t.removeMetadata("DRAKES_ABILITY_DAMAGE", plugin); }
        return true;
    }

    /** Empuje acotado (no catapulta a jefes ni a jugadores). */
    public static void knock(LivingEntity t, Vector v) {
        double max = t instanceof Player ? d("pvp-knockback", 0.5) : isBoss(t) ? 0.0 : d("knockback", 1.0);
        if (max <= 0 || v == null) return;
        if (v.lengthSquared() > max * max) v = v.clone().normalize().multiply(max);
        t.setVelocity(v);
    }

    /** Multiplicador ofensivo atenuado y con tope de dano extra por golpe (suma de todas las fuentes). */
    public static void boost(EntityDamageByEntityEvent e, double mult) {
        if (lastBoost != e) { lastBoost = e; boostExtra = 0; }
        double cap = 4;
        if (e.getEntity() instanceof LivingEntity t) cap = t instanceof Player ? d("pvp-bonus-cap", 1.5) : isBoss(t) ? d("boss-bonus-cap", 2) : d("pve-bonus-cap", 4);
        double want = e.getDamage() * (mult - 1.0) * d("bonus-scale", 0.4);
        double add = Math.max(0, Math.min(want, cap - boostExtra));
        boostExtra += add;
        e.setDamage(e.getDamage() + add);
    }

    /** Reduccion defensiva atenuada: ninguna pasiva baja de -10 % y el total de un golpe no pasa de -15 %. */
    public static void reduce(EntityDamageByEntityEvent e, double mult) { reduceDamage(e, e.getDamage(), mult); }

    public static void reduce(org.bukkit.event.entity.EntityDamageEvent e, double mult) {
        e.setDamage(e.getDamage() * Math.max(mult, d("min-single-mitigation", 0.92)));
    }

    private static void reduceDamage(EntityDamageByEntityEvent e, double cur, double mult) {
        if (lastReduce != e) { lastReduce = e; reduceFactor = 1.0; }
        double floor = d("min-total-mitigation", 0.85);
        double f = Math.max(mult, d("min-single-mitigation", 0.92));
        double next = Math.max(floor, reduceFactor * f);
        e.setDamage(cur * (next / reduceFactor));
        reduceFactor = next;
    }

    /** Esquive pasivo: probabilidad topada y enfriamiento propio. */
    public static boolean dodge(Player p, double chance) {
        double c = Math.min(chance, d("dodge-max-chance", 0.06));
        long now = System.currentTimeMillis();
        if (now < DODGE_CD.getOrDefault(p.getUniqueId(), 0L)) return false;
        if (Math.random() >= c) return false;
        DODGE_CD.put(p.getUniqueId(), now + (long) (d("dodge-cooldown-seconds", 12) * 1000));
        return true;
    }

    /** Cooldown de una habilidad activa: multiplicador global y suelo. */
    public static long cd(long ms) {
        return Math.max((long) (d("min-cooldown-seconds", 10) * 1000), (long) (ms * d("cooldown-multiplier", 2.5)));
    }

    /** Descuento por rebirth acotado (antes hasta -50 %). */
    public static double cdr(int rebirths) { return 1.0 - Math.min(d("rebirth-cdr-max", 0.2), rebirths * 0.005); }

    public static long formMs() { return (long) (d("form-seconds", 45) * 1000); }
    public static int formTicks() { return (int) (d("form-seconds", 45) * 20); }
    public static long formCooldownMs() { return (long) (d("form-cooldown-seconds", 240) * 1000); }
    public static int flightSeconds(int asked) { return (int) Math.min(asked, d("transformation-flight-seconds", 0)); }

    public static PotionEffect pe(PotionEffectType t, int ticks, int amp) { return pe(t, ticks, amp, true, true, true); }
    public static PotionEffect pe(PotionEffectType t, int ticks, int amp, boolean ambient, boolean particles) { return pe(t, ticks, amp, ambient, particles, true); }

    /** Efecto de pocion acotado: buffs max Fuerza I / Velocidad II / Resistencia I; debuffs max nivel II y 5 s. */
    public static PotionEffect pe(PotionEffectType t, int ticks, int amp, boolean ambient, boolean particles, boolean icon) {
        int maxAmp;
        int maxTicks;
        if (t == PotionEffectType.SLOWNESS || t == PotionEffectType.WEAKNESS || t == PotionEffectType.BLINDNESS
                || t == PotionEffectType.DARKNESS || t == PotionEffectType.HUNGER || t == PotionEffectType.MINING_FATIGUE
                || t == PotionEffectType.NAUSEA || t == PotionEffectType.POISON || t == PotionEffectType.LEVITATION) {
            maxAmp = 1; maxTicks = 100;
        } else if (t == PotionEffectType.WITHER) {
            maxAmp = 0; maxTicks = 60;
        } else if (t == PotionEffectType.SPEED || t == PotionEffectType.JUMP_BOOST || t == PotionEffectType.HEALTH_BOOST) {
            maxAmp = 1; maxTicks = Math.max(200, formTicks());
        } else {
            maxAmp = 0; maxTicks = Math.max(200, formTicks()); // fuerza, resistencia, regeneracion, absorcion, etc.
        }
        return new PotionEffect(t, Math.min(ticks, maxTicks), Math.min(amp, maxAmp), ambient, particles, icon);
    }
}

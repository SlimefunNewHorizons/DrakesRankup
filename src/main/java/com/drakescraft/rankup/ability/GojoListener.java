package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * El Infinito de Gojo: pasiva Infinito (desvio de proyectiles y golpes) y dominios
 * (Vacio Infinito, Santuario Malevolo). Todo se afina en config.yml bajo "gojo:".
 */
public class GojoListener implements Listener {

    private final DrakesRankupPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public GojoListener(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    private int tierOf(Player p) {
        Rank r = plugin.getRankManager().getPlayerRank(p.getUniqueId());
        return r == null ? 0 : r.getTier();
    }

    /** Probabilidad de que el Infinito detenga un proyectil: 15% al tier minimo, +1% por tier, tope configurable. */
    public double infinityChance(int tier) {
        int min = plugin.getConfig().getInt("gojo.infinity.min-tier", 19);
        if (tier < min) return 0.0;
        double base = plugin.getConfig().getDouble("gojo.infinity.base-chance", 0.15);
        double per = plugin.getConfig().getDouble("gojo.infinity.per-tier", 0.01);
        double cap = plugin.getConfig().getDouble("gojo.infinity.max-chance", 0.60);
        return Math.min(cap, base + per * (tier - min));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInfinity(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!plugin.isWorldAllowed(p.getWorld())) return;
        PlayerSettings st = plugin.getRankManager().getPlayerSettings(p.getUniqueId());
        if (st == null || !st.isAbilitiesEnabled()) return;
        double chance = infinityChance(tierOf(p));
        if (chance <= 0) return;
        boolean projectile = e.getDamager() instanceof Projectile;
        if (random.nextDouble() >= (projectile ? chance : chance / 2.0)) return;
        e.setCancelled(true);
        if (projectile) e.getDamager().remove();
        Location l = p.getLocation().add(0, 1, 0);
        p.getWorld().spawnParticle(Particle.END_ROD, l, 18, 0.5, 0.7, 0.5, 0.02);
        p.playSound(l, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.6f);
        if (!projectile) {
            Vector away = e.getDamager().getLocation().toVector().subtract(l.toVector()).setY(0);
            if (away.lengthSquared() > 0) e.getDamager().setVelocity(away.normalize().multiply(0.6).setY(0.2));
        }
    }

    /** @return segundos de espera restantes (0 = lanzado). -1 si no cumple requisitos. */
    public long castDomain(Player p, String type) {
        boolean vacio = type.equals("vacio");
        int tier = tierOf(p);
        int need = plugin.getConfig().getInt("gojo.domain." + type + ".min-tier", vacio ? 19 : 20);
        if (!plugin.isWorldAllowed(p.getWorld())) { p.sendMessage("§cLos dominios no funcionan en este mundo."); return -1; }
        if (tier < need) { p.sendMessage("§cNecesitas el tier " + need + " para este dominio."); return -1; }
        long now = System.currentTimeMillis();
        long cd = plugin.getConfig().getLong("gojo.domain.cooldown-seconds", 90) * 1000L;
        Long until = cooldowns.get(p.getUniqueId());
        if (until != null && until > now) return (until - now) / 1000 + 1;
        cooldowns.put(p.getUniqueId(), now + cd);

        boolean sup = tier >= plugin.getConfig().getInt("gojo.domain.supreme-tier", 60);
        double radius = plugin.getConfig().getDouble("gojo.domain.radius", 12) * (sup ? 1.5 : 1.0);
        int ticks = (int) (plugin.getConfig().getInt("gojo.domain.duration-seconds", 8) * (sup ? 1.5 : 1.0) * 20);
        double dmg = plugin.getConfig().getDouble("gojo.domain.santuario.damage", 3.0);
        Location c = p.getLocation().clone();
        p.getWorld().playSound(c, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.7f);
        p.sendTitle(vacio ? "§5§lVACIO INFINITO" : "§4§lSANTUARIO MALEVOLO", "§7Expansion de dominio", 5, 30, 10);
        Set<Mob> frozen = new HashSet<>();

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= ticks || !p.isOnline()) { finish(); return; }
                for (int i = 0; i < 40; i++) { // cascaron esferico de particulas
                    double th = random.nextDouble() * Math.PI * 2, ph = Math.acos(2 * random.nextDouble() - 1);
                    Location pt = c.clone().add(radius * Math.sin(ph) * Math.cos(th), radius * Math.cos(ph) + 1, radius * Math.sin(ph) * Math.sin(th));
                    c.getWorld().spawnParticle(vacio ? Particle.END_ROD : Particle.CRIMSON_SPORE, pt, 1, 0, 0, 0, 0);
                }
                for (LivingEntity le : c.getWorld().getNearbyLivingEntities(c, radius)) {
                    if (le.equals(p) || le instanceof ArmorStand) continue;
                    if (le instanceof Player pl && (pl.getGameMode() == GameMode.CREATIVE || pl.getGameMode() == GameMode.SPECTATOR)) continue;
                    if (le instanceof Player && plugin.getProtectionGate().isProtected(le.getLocation(), p)) continue;
                    if (vacio) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 4, false, false, false));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 30, 3, false, false, false));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false, false));
                        if (le instanceof Mob m && frozen.add(m)) m.setAI(false);
                    } else {
                        le.damage(dmg, p);
                        le.setFireTicks(40);
                    }
                }
                t += 10;
            }
            private void finish() {
                for (Mob m : frozen) if (m.isValid()) m.setAI(true);
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 10L);
        return 0;
    }
}

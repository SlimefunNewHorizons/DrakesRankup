package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Herobrine: jefe publico de 3 fases. Un jefe por mundo, cooldown por jugador, botin para todos los participantes. */
public class HerobrineBoss implements Listener, CommandExecutor {

    private final DrakesRankupPlugin plugin;
    private final NamespacedKey bossKey;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Fight> fights = new HashMap<>(); // mundo -> combate

    private static final class Fight {
        Zombie boss; BossBar bar; int phase = 1; long end;
        final Map<UUID, Double> damage = new HashMap<>();
        final List<Entity> minions = new ArrayList<>();
    }

    public HerobrineBoss(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, "herobrine_boss");
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage("Solo jugadores."); return true; }
        if (!plugin.isWorldAllowed(p.getWorld())) { p.sendMessage("§cHerobrine no habita este mundo."); return true; }
        World w = p.getWorld();
        Fight active = fights.get(w.getUID());
        if (active != null && active.boss.isValid()) { p.sendMessage("§cHerobrine ya esta entre nosotros. ¡Ve a combatirlo!"); return true; }
        long now = System.currentTimeMillis();
        long cd = plugin.getConfig().getLong("herobrine.cooldown-seconds", 600) * 1000L;
        Long until = cooldowns.get(p.getUniqueId());
        if (until != null && until > now) { p.sendMessage("§cLa sombra descansa. Espera " + ((until - now) / 1000 + 1) + "s."); return true; }
        cooldowns.put(p.getUniqueId(), now + cd);
        spawn(p);
        return true;
    }

    private void spawn(Player summoner) {
        World w = summoner.getWorld();
        Location loc = summoner.getLocation().add(summoner.getLocation().getDirection().setY(0).normalize().multiply(8));
        loc.setY(w.getHighestBlockYAt(loc) + 1);
        w.strikeLightningEffect(loc);
        w.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.5f);
        Zombie z = (Zombie) w.spawnEntity(loc, EntityType.ZOMBIE);
        double hp = plugin.getConfig().getDouble("herobrine.health", 400);
        z.setBaby(false);
        z.customName(net.kyori.adventure.text.Component.text("§4§lHEROBRINE §8[§6JEFE§8]"));
        z.setCustomNameVisible(true);
        z.setRemoveWhenFarAway(false);
        z.setPersistent(false);
        z.getAttribute(Attribute.MAX_HEALTH).setBaseValue(hp);
        z.setHealth(hp);
        z.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(plugin.getConfig().getDouble("herobrine.damage", 10));
        z.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.27);
        z.getAttribute(Attribute.FOLLOW_RANGE).setBaseValue(48);
        z.getAttribute(Attribute.KNOCKBACK_RESISTANCE).setBaseValue(0.8);
        z.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        z.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        z.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        z.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        z.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
        for (EquipmentSlot sl : EquipmentSlot.values()) z.getEquipment().setDropChance(sl, 0f);
        z.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        z.getPersistentDataContainer().set(bossKey, PersistentDataType.BYTE, (byte) 1);

        Fight f = new Fight();
        f.boss = z;
        f.bar = Bukkit.createBossBar("§4§lHEROBRINE", BarColor.RED, BarStyle.SEGMENTED_6);
        f.end = System.currentTimeMillis() + plugin.getConfig().getLong("herobrine.lifetime-seconds", 900) * 1000L;
        fights.put(w.getUID(), f);
        for (Player pl : w.getPlayers()) pl.sendMessage("§4§l[HEROBRINE] §c" + summoner.getName() + " ha perturbado mi descanso...");

        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (!z.isValid()) { end(w, f, false); cancel(); return; }
                if (System.currentTimeMillis() > f.end) {
                    for (Player pl : w.getPlayers()) pl.sendMessage("§8La sombra de Herobrine se desvanece en la niebla...");
                    z.remove(); end(w, f, false); cancel(); return;
                }
                tick++;
                f.bar.setProgress(Math.max(0, Math.min(1, z.getHealth() / z.getAttribute(Attribute.MAX_HEALTH).getValue())));
                for (Player pl : w.getPlayers()) {
                    if (pl.getLocation().distanceSquared(z.getLocation()) < 60 * 60) f.bar.addPlayer(pl); else f.bar.removePlayer(pl);
                }
                double frac = z.getHealth() / z.getAttribute(Attribute.MAX_HEALTH).getValue();
                if (f.phase == 1 && frac <= 0.66) phase2(w, z, f);
                if (f.phase == 2 && frac <= 0.33) phase3(w, z, f);
                if (f.phase == 3 && tick % 6 == 0) blink(z);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void phase2(World w, Zombie z, Fight f) {
        f.phase = 2;
        f.bar.setColor(BarColor.PURPLE);
        w.strikeLightningEffect(z.getLocation());
        for (Player p : w.getPlayers()) if (p.getLocation().distanceSquared(z.getLocation()) < 3600) p.sendMessage("§4§l[HEROBRINE] §c¡Mis sombras os devoraran!");
        for (int i = 0; i < 4; i++) {
            Location l = z.getLocation().add(Math.cos(i * Math.PI / 2) * 3, 0, Math.sin(i * Math.PI / 2) * 3);
            LivingEntity m = (LivingEntity) w.spawnEntity(l, i % 2 == 0 ? EntityType.WITHER_SKELETON : EntityType.ZOMBIE);
            m.setRemoveWhenFarAway(true);
            m.setPersistent(false);
            f.minions.add(m);
        }
        z.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 1, false, false));
    }

    private void phase3(World w, Zombie z, Fight f) {
        f.phase = 3;
        f.bar.setColor(BarColor.WHITE);
        w.playSound(z.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2f, 0.6f);
        for (Player p : w.getPlayers()) if (p.getLocation().distanceSquared(z.getLocation()) < 3600) p.sendMessage("§4§l[HEROBRINE] §c¡Ya no hay escapatoria!");
        z.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 60, 1, false, false));
    }

    private void blink(Zombie z) {
        Player target = null; double best = 30 * 30;
        for (Player p : z.getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(z.getLocation());
            if (p.getGameMode() == org.bukkit.GameMode.SURVIVAL && d < best) { best = d; target = p; }
        }
        if (target == null) return;
        Location behind = target.getLocation().subtract(target.getLocation().getDirection().setY(0).normalize().multiply(2));
        z.getWorld().strikeLightningEffect(z.getLocation());
        z.teleport(behind);
        z.getWorld().playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
    }

    private void end(World w, Fight f, boolean killed) {
        f.bar.removeAll();
        for (Entity m : f.minions) if (m.isValid()) m.remove();
        fights.remove(w.getUID());
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!e.getEntity().getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE)) return;
        Entity d = e.getDamager();
        if (d instanceof Projectile pr && pr.getShooter() instanceof Entity sh) d = sh;
        if (!(d instanceof Player p)) return;
        Fight f = fights.get(e.getEntity().getWorld().getUID());
        if (f != null) f.damage.merge(p.getUniqueId(), e.getFinalDamage(), Double::sum);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (!e.getEntity().getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE)) return;
        World w = e.getEntity().getWorld();
        Fight f = fights.get(w.getUID());
        e.getDrops().clear();
        e.setDroppedExp(2000);
        w.strikeLightningEffect(e.getEntity().getLocation());
        if (f == null) return;
        UUID top = null; double best = 0;
        for (Map.Entry<UUID, Double> en : f.damage.entrySet()) if (en.getValue() > best) { best = en.getValue(); top = en.getKey(); }
        for (UUID id : f.damage.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            giveOrDrop(p, new ItemStack(Material.DIAMOND, plugin.getConfig().getInt("herobrine.reward-diamonds", 5)));
            if (id.equals(top)) giveOrDrop(p, essence());
        }
        for (Player p : w.getPlayers()) p.sendMessage("§6§l[HEROBRINE DERROTADO] §e" + (top == null ? "?" : Bukkit.getOfflinePlayer(top).getName()) + " §7asesto el golpe mas fuerte. ¡" + f.damage.size() + " heroes lucharon!");
        end(w, f, true);
    }

    private ItemStack essence() {
        ItemStack it = new ItemStack(Material.NETHER_STAR);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName("§5§lEsencia de Herobrine");
        m.setLore(List.of("§7Un fragmento de la leyenda original.", "§eCanjeable en el mercado de DrakesCraft."));
        it.setItemMeta(m);
        return it;
    }

    private void giveOrDrop(Player p, ItemStack it) {
        p.getInventory().addItem(it).values().forEach(r -> p.getWorld().dropItemNaturally(p.getLocation(), r));
    }

    /** Limpieza al apagar: no deja jefes ni barras colgadas. */
    public void shutdown() {
        for (Fight f : fights.values()) {
            f.bar.removeAll();
            if (f.boss.isValid()) f.boss.remove();
            f.minions.forEach(Entity::remove);
        }
        fights.clear();
    }
}

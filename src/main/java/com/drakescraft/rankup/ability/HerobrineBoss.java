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
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
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
        Location center;
        double radius;
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
        if (a.length == 1 && a[0].equalsIgnoreCase("estado")) {
            showStatus(p, active);
            return true;
        }
        if (a.length != 0) {
            p.sendMessage("§eUso: /herobrine [estado]");
            return true;
        }
        if (active != null && active.boss.isValid()) { p.sendMessage("§cHerobrine ya esta entre nosotros. ¡Ve a combatirlo!"); return true; }
        long now = System.currentTimeMillis();
        long cd = plugin.getConfig().getLong("herobrine.cooldown-seconds", 600) * 1000L;
        Long until = cooldowns.get(p.getUniqueId());
        if (until != null && until > now) { p.sendMessage("§cLa sombra descansa. Espera " + ((until - now) / 1000 + 1) + "s."); return true; }
        cooldowns.put(p.getUniqueId(), now + cd);
        spawn(p);
        return true;
    }

    private void showStatus(Player player, Fight f) {
        if (f == null || !f.boss.isValid() || f.boss.isDead() || System.currentTimeMillis() >= f.end) {
            player.sendMessage("§7No hay un combate de Herobrine activo en este mundo.");
            return;
        }
        double damage = f.damage.getOrDefault(player.getUniqueId(), 0.0);
        double minimum = Math.max(0, plugin.getConfig().getDouble("herobrine.minimum-reward-damage", 4));
        boolean qualified = damage > 0 && damage >= minimum && canParticipate(f, player);
        player.sendMessage("§4[HEROBRINE] §eFase " + f.phase + "/3 | Vida: "
                + Math.ceil(f.boss.getHealth()) + "/" + Math.ceil(f.boss.getAttribute(Attribute.MAX_HEALTH).getValue())
                + " | Tiempo restante: " + Math.max(0, (f.end - System.currentTimeMillis() + 999) / 1000) + "s.");
        player.sendMessage("§7Arena: X=" + f.center.getBlockX() + " Z=" + f.center.getBlockZ()
                + " | Radio: " + (int) f.radius + " | " + (inArena(f, player.getLocation()) ? "Dentro" : "Fuera") + ".");
        player.sendMessage("§7Tu daño efectivo: " + damage + " / minimo " + minimum + ". "
                + (qualified ? "§aParticipacion suficiente; sigue vivo en la arena hasta la victoria."
                             : "§ePara obtener botin, aporta daño suficiente y permanece vivo en Survival/Adventure dentro de la arena."));
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
        // La limpieza del encuentro no debe llevarse objetos de los jugadores.
        z.setCanPickupItems(false);
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
        f.center = loc.clone();
        f.radius = Math.max(8, Math.min(128, plugin.getConfig().getDouble("herobrine.arena-radius", 48)));
        f.bar = Bukkit.createBossBar("§4§lHEROBRINE §7| Fase 1/3", BarColor.RED, BarStyle.SEGMENTED_6);
        f.end = System.currentTimeMillis() + plugin.getConfig().getLong("herobrine.lifetime-seconds", 900) * 1000L;
        fights.put(w.getUID(), f);
        summoner.sendMessage("§7Arena: radio de " + (int) f.radius + " bloques. Combate en Survival/Adventure y permanece dentro hasta la victoria para recibir botin.");
        for (Player pl : w.getPlayers()) pl.sendMessage("§4§l[HEROBRINE] §c" + summoner.getName() + " ha perturbado mi descanso...");

        new BukkitRunnable() {
            int tick = 0;
            @Override public void run() {
                if (!z.isValid() || fights.get(w.getUID()) != f) { end(w, f, false); cancel(); return; }
                if (System.currentTimeMillis() >= f.end) {
                    for (Player pl : w.getPlayers()) pl.sendMessage("§8La sombra de Herobrine se desvanece en la niebla...");
                    z.remove(); end(w, f, false); cancel(); return;
                }
                tick++;
                clearIneligibleTarget(f, z);
                if (!inArena(f, z.getLocation())) {
                    z.setTarget(null);
                    z.teleport(f.center);
                }
                for (Entity minion : f.minions) {
                    if (minion.isValid() && minion instanceof Mob mob) clearIneligibleTarget(f, mob);
                    if (minion.isValid() && !inArena(f, minion.getLocation())) {
                        if (minion instanceof org.bukkit.entity.Mob mob) mob.setTarget(null);
                        minion.teleport(f.center);
                    }
                }
                f.bar.setProgress(Math.max(0, Math.min(1, z.getHealth() / z.getAttribute(Attribute.MAX_HEALTH).getValue())));
                for (Player viewer : new ArrayList<>(f.bar.getPlayers())) {
                    if (!inArena(f, viewer.getLocation())) f.bar.removePlayer(viewer);
                }
                for (Player pl : w.getPlayers()) {
                    if (inArena(f, pl.getLocation())) f.bar.addPlayer(pl); else f.bar.removePlayer(pl);
                }
                double frac = z.getHealth() / z.getAttribute(Attribute.MAX_HEALTH).getValue();
                if (f.phase == 1 && frac <= 0.66) phase2(w, z, f);
                if (f.phase == 2 && frac <= 0.33) phase3(w, z, f);
                if (f.phase == 3 && tick % 6 == 0) blink(z, f);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void phase2(World w, Zombie z, Fight f) {
        f.phase = 2;
        f.bar.setTitle("§4§lHEROBRINE §7| Fase 2/3: Sombras");
        f.bar.setColor(BarColor.PURPLE);
        w.strikeLightningEffect(z.getLocation());
        for (Player p : w.getPlayers()) if (p.getLocation().distanceSquared(z.getLocation()) < 3600) p.sendMessage("§4§l[HEROBRINE] §c¡Mis sombras os devoraran!");
        for (int i = 0; i < 4; i++) {
            Location l = z.getLocation().add(Math.cos(i * Math.PI / 2) * 3, 0, Math.sin(i * Math.PI / 2) * 3);
            LivingEntity m = (LivingEntity) w.spawnEntity(l, i % 2 == 0 ? EntityType.WITHER_SKELETON : EntityType.ZOMBIE);
            m.setRemoveWhenFarAway(true);
            m.setPersistent(false);
            m.setCanPickupItems(false);
            f.minions.add(m);
        }
        z.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1, false, false));
    }

    private void phase3(World w, Zombie z, Fight f) {
        f.phase = 3;
        f.bar.setTitle("§4§lHEROBRINE §7| Fase 3/3: Furia");
        f.bar.setColor(BarColor.WHITE);
        w.playSound(z.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2f, 0.6f);
        for (Player p : w.getPlayers()) if (p.getLocation().distanceSquared(z.getLocation()) < 3600) p.sendMessage("§4§l[HEROBRINE] §c¡Ya no hay escapatoria!");
        z.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, PotionEffect.INFINITE_DURATION, 1, false, false));
    }

    private void blink(Zombie z, Fight f) {
        Player target = null; double best = 30 * 30;
        for (Player p : z.getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(z.getLocation());
            if (canParticipate(f, p) && d < best) { best = d; target = p; }
        }
        if (target == null) return;
        Location behind = target.getLocation().subtract(target.getLocation().getDirection().setY(0).normalize().multiply(2));
        if (!inArena(f, behind) || !behind.getWorld().getWorldBorder().isInside(behind)
                || !behind.getBlock().isPassable() || !behind.clone().add(0, 1, 0).getBlock().isPassable()
                || !behind.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) return;
        z.getWorld().strikeLightningEffect(z.getLocation());
        z.teleport(behind);
        z.getWorld().playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
    }

    private void end(World w, Fight f, boolean killed) {
        f.bar.removeAll();
        for (Entity m : f.minions) if (m.isValid()) m.remove();
        fights.remove(w.getUID(), f);
    }

    private boolean inArena(Fight f, Location location) {
        if (!f.center.getWorld().equals(location.getWorld())) return false;
        double dx = location.getX() - f.center.getX();
        double dz = location.getZ() - f.center.getZ();
        return dx * dx + dz * dz <= f.radius * f.radius;
    }

    private boolean canParticipate(Fight f, Player player) {
        return !player.isDead()
                && (player.getGameMode() == org.bukkit.GameMode.SURVIVAL
                    || player.getGameMode() == org.bukkit.GameMode.ADVENTURE)
                && inArena(f, player.getLocation());
    }

    private boolean canTarget(Fight f, LivingEntity target) {
        return target instanceof Player player
                ? canParticipate(f, player) : inArena(f, target.getLocation());
    }

    private void clearIneligibleTarget(Fight f, Mob mob) {
        LivingEntity target = mob.getTarget();
        if (target != null && !canTarget(f, target)) mob.setTarget(null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        Fight f = fights.get(e.getEntity().getWorld().getUID());
        if (f == null || (e.getEntity() != f.boss && !f.minions.contains(e.getEntity()))) return;
        if (e.getTarget() != null && !canTarget(f, e.getTarget())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!e.getEntity().getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE)) return;
        Entity d = e.getDamager();
        if (d instanceof Projectile pr && pr.getShooter() instanceof Entity sh) d = sh;
        if (!(d instanceof Player p)) return;
        Fight f = fights.get(e.getEntity().getWorld().getUID());
        if (f == null || !f.boss.equals(e.getEntity()) || !canParticipate(f, p)) return;
        double damage = Math.min(e.getFinalDamage(), f.boss.getHealth());
        if (Double.isFinite(damage) && damage > 0) f.damage.merge(p.getUniqueId(), damage, Double::sum);
    }

    /** La arena delimita el combate, incluidos proyectiles que ya estaban en vuelo. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArenaDamage(EntityDamageByEntityEvent e) {
        Fight f = fights.get(e.getEntity().getWorld().getUID());
        if (f == null) return;
        Entity attacker = e.getDamager();
        if (attacker instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) attacker = shooter;
        boolean victimInFight = e.getEntity().equals(f.boss) || f.minions.contains(e.getEntity());
        boolean attackerInFight = attacker.equals(f.boss) || f.minions.contains(attacker);
        if ((victimInFight || attackerInFight) && System.currentTimeMillis() >= f.end) {
            e.setCancelled(true);
            return;
        }
        if (victimInFight && attacker instanceof Player player
                && (!canParticipate(f, player) || !inArena(f, e.getEntity().getLocation()))) e.setCancelled(true);
        if (attackerInFight && e.getEntity() instanceof Player player
                && (!canParticipate(f, player) || !inArena(f, attacker.getLocation()))) e.setCancelled(true);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (!e.getEntity().getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE)) return;
        World w = e.getEntity().getWorld();
        Fight f = fights.get(w.getUID());
        if (f == null || !f.boss.equals(e.getEntity())) return;
        e.getDrops().clear();
        // La experiencia forma parte del botin de participacion, sin orbes para espectadores.
        e.setDroppedExp(0);
        // El temporizador limpia cada segundo; morir en ese intervalo no amplía el plazo.
        if (System.currentTimeMillis() >= f.end) {
            end(w, f, false);
            return;
        }
        w.strikeLightningEffect(e.getEntity().getLocation());
        double minimum = Math.max(0, plugin.getConfig().getDouble("herobrine.minimum-reward-damage", 4));
        Map<UUID, Double> eligible = new HashMap<>();
        for (Map.Entry<UUID, Double> entry : f.damage.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (entry.getValue() >= minimum && player != null
                    && canParticipate(f, player)) eligible.put(entry.getKey(), entry.getValue());
        }
        UUID top = null; double best = 0;
        for (Map.Entry<UUID, Double> en : eligible.entrySet()) if (en.getValue() > best) { best = en.getValue(); top = en.getKey(); }
        int experience = Math.max(0, plugin.getConfig().getInt("herobrine.reward-experience", 2000));
        int share = eligible.isEmpty() ? 0 : experience / eligible.size();
        int remainder = eligible.isEmpty() ? 0 : experience % eligible.size();
        for (UUID id : eligible.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            int diamonds = Math.max(0, Math.min(64, plugin.getConfig().getInt("herobrine.reward-diamonds", 5)));
            if (diamonds > 0) giveOrDrop(p, new ItemStack(Material.DIAMOND, diamonds));
            if (id.equals(top)) giveOrDrop(p, essence());
            int earnedExperience = share + (id.equals(top) ? remainder : 0);
            if (earnedExperience > 0) p.giveExp(earnedExperience);
            p.sendMessage("§6[HEROBRINE] §eBotin: " + diamonds + " diamantes"
                    + (id.equals(top) ? " y 1 esencia" : "") + ", " + earnedExperience
                    + " XP. Daño aportado: " + Math.round(eligible.get(id)) + ".");
        }
        for (Player p : w.getPlayers()) p.sendMessage("§6§l[HEROBRINE DERROTADO] §e" + (top == null ? "Sin participantes elegibles" : Bukkit.getOfflinePlayer(top).getName()) + " §7| Recompensas para " + eligible.size() + " participantes.");
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

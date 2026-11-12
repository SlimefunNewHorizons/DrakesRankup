package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.block.BlockMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.entity.ArrowMock;
import org.mockbukkit.mockbukkit.entity.ZombieMock;
import org.mockbukkit.mockbukkit.entity.WitherSkeletonMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.mockbukkit.mockbukkit.world.Coordinate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HerobrineBossTest {
    private ServerMock server;
    private DrakesRankupPlugin plugin;
    private WorldMock world;
    private PlayerMock player;
    private Zombie boss;

    // MockBukkit lacks despawn/pickup flags. Preserve their values without
    // replacing command dispatch, arena scheduling or reward handling.
    private static final class ArenaZombieMock extends ZombieMock {
        private boolean removeWhenFarAway = true;
        private boolean canPickupItems = true;

        ArenaZombieMock(ServerMock server) {
            super(server, UUID.randomUUID());
            // ZombieMock only initializes health and movement speed.
            registerAttribute(Attribute.ATTACK_DAMAGE);
            registerAttribute(Attribute.FOLLOW_RANGE);
            registerAttribute(Attribute.KNOCKBACK_RESISTANCE);
        }

        @Override public void setRemoveWhenFarAway(boolean remove) { removeWhenFarAway = remove; }
        @Override public boolean getRemoveWhenFarAway() { return removeWhenFarAway; }
        @Override public void setCanPickupItems(boolean pickup) { canPickupItems = pickup; }
        @Override public boolean getCanPickupItems() { return canPickupItems; }
    }

    private static final class ArenaSkeletonMock extends WitherSkeletonMock {
        private boolean removeWhenFarAway = true;
        private boolean canPickupItems = true;

        ArenaSkeletonMock(ServerMock server) { super(server, UUID.randomUUID()); }

        @Override public void setRemoveWhenFarAway(boolean remove) { removeWhenFarAway = remove; }
        @Override public boolean getRemoveWhenFarAway() { return removeWhenFarAway; }
        @Override public void setCanPickupItems(boolean pickup) { canPickupItems = pickup; }
        @Override public boolean getCanPickupItems() { return canPickupItems; }
    }

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        // MockBukkit does not implement cosmetic lightning; gameplay stays real.
        world = new WorldMock(Material.GRASS_BLOCK, 64) {
            private final Map<Coordinate, BlockMock> collisionBlocks = new HashMap<>();

            @Override public BlockMock getBlockAt(Coordinate coordinate) {
                return collisionBlocks.computeIfAbsent(coordinate, key -> {
                    BlockMock original = super.getBlockAt(key);
                    return new BlockMock(original.getType(), original.getLocation()) {
                        // This fixture uses only full cubes and air. Complex
                        // collision shapes still require a real server/client.
                        @Override public boolean isPassable() { return getType().isAir(); }
                    };
                });
            }

            @Override public LightningStrike strikeLightningEffect(Location location) { return null; }

            @Override public Entity spawnEntity(Location location, EntityType type) {
                if (type == EntityType.WITHER_SKELETON) {
                    ArenaSkeletonMock skeleton = new ArenaSkeletonMock(server);
                    server.registerEntity(skeleton);
                    skeleton.teleport(location);
                    return skeleton;
                }
                if (type != EntityType.ZOMBIE) return super.spawnEntity(location, type);
                ArenaZombieMock zombie = new ArenaZombieMock(server);
                server.registerEntity(zombie);
                zombie.teleport(location);
                return zombie;
            }
        };
        world.setName("herobrine_arena_test");
        server.addWorld(world);
        plugin = MockBukkit.load(DrakesRankupPlugin.class);
        player = server.addPlayer("Participant");
        player.teleport(new Location(world, 0, 65, 0));
        assertTrue(player.performCommand("herobrine"));
        boss = world.getEntitiesByClass(Zombie.class).iterator().next();
        assertFalse(boss.getRemoveWhenFarAway());
        assertEquals(plugin.getConfig().getDouble("herobrine.health"), boss.getHealth());
        assertEquals(plugin.getConfig().getDouble("herobrine.damage"),
                boss.getAttribute(Attribute.ATTACK_DAMAGE).getBaseValue());
        player.teleport(boss.getLocation());
    }

    @AfterEach
    void tearDown() { MockBukkit.unmock(); }

    private void hit(PlayerMock attacker, double damage, boolean cancelled) {
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
                attacker, boss, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                DamageSource.builder(DamageType.PLAYER_ATTACK).withCausingEntity(attacker).withDirectEntity(attacker).build(), damage);
        event.setCancelled(cancelled);
        server.getPluginManager().callEvent(event);
    }

    private void defeat() {
        server.getPluginManager().callEvent(new EntityDeathEvent(boss,
                DamageSource.builder(DamageType.PLAYER_ATTACK).withCausingEntity(player).withDirectEntity(player).build(),
                new ArrayList<ItemStack>()));
    }

    private String status(PlayerMock viewer) {
        while (viewer.nextMessage() != null) { /* discard earlier fight announcements */ }
        assertTrue(viewer.performCommand("herobrine estado"));
        StringBuilder messages = new StringBuilder();
        String message;
        while ((message = viewer.nextMessage()) != null) messages.append(message).append('\n');
        return messages.toString();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void statusReportsCurrentPhaseArenaAndEffectiveParticipation(int phase) {
        hit(player, 10, false);
        hit(player, 100, true);
        boss.setHealth(phase == 1 ? 400 : phase == 2 ? 200 : 100);
        server.getScheduler().performTicks(20);
        String messages = status(player);
        assertTrue(messages.contains("Fase " + phase + "/3"), messages);
        assertTrue(messages.contains("Vida: " + (phase == 1 ? "400.0" : phase == 2 ? "200.0" : "100.0")), messages);
        assertTrue(messages.contains("Tiempo restante:"), messages);
        assertTrue(messages.contains("X=" + boss.getLocation().getBlockX()), messages);
        assertTrue(messages.contains("Z=" + boss.getLocation().getBlockZ()), messages);
        assertTrue(messages.contains("Radio: 48 | Dentro"), messages);
        assertTrue(messages.contains("Tu daño efectivo: 10.0 / minimo 4.0"), messages);
        assertTrue(messages.contains("Participacion suficiente"), messages);
        assertEquals(1, world.getEntitiesByClass(Zombie.class).stream().filter(z -> z.equals(boss)).count());
        assertFalse(player.getInventory().contains(Material.DIAMOND));
        assertFalse(player.getInventory().contains(Material.NETHER_STAR));
    }

    @ParameterizedTest
    @EnumSource(GameMode.class)
    void statusRechecksCombatModeWithoutDiscardingContribution(GameMode mode) {
        hit(player, 10, false);
        player.setGameMode(mode);
        String messages = status(player);
        assertTrue(messages.contains("Tu daño efectivo: 10.0"), messages);
        boolean eligible = mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
        assertEquals(eligible, messages.contains("Participacion suficiente"), messages);
    }

    @Test
    void statusExplainsMissingContributionAndLeavingArena() {
        assertFalse(status(player).contains("Participacion suficiente"));
        hit(player, 10, false);
        player.teleport(player.getLocation().add(100, 0, 0));
        String messages = status(player);
        assertTrue(messages.contains("Radio: 48 | Fuera"), messages);
        assertTrue(messages.contains("Tu daño efectivo: 10.0"), messages);
        assertFalse(messages.contains("Participacion suficiente"), messages);
    }

    @Test
    void statusAndInvalidArgumentsWithoutFightDoNotSummonOrConsumeCooldown() {
        defeat();
        boss.remove();
        PlayerMock visitor = server.addPlayer("Visitor");
        visitor.teleport(player.getLocation());
        assertTrue(status(visitor).contains("No hay un combate"));
        assertTrue(visitor.performCommand("herobrine desconocido"));
        assertTrue(visitor.nextMessage().contains("Uso: /herobrine [estado]"));
        assertTrue(visitor.performCommand("herobrine estado extra"));
        assertTrue(visitor.nextMessage().contains("Uso: /herobrine [estado]"));
        assertTrue(world.getEntitiesByClass(Zombie.class).isEmpty());
        assertTrue(visitor.performCommand("herobrine"));
        assertEquals(1, world.getEntitiesByClass(Zombie.class).size());
    }

    private EntityDamageByEntityEvent attack(Entity attacker, Entity victim, boolean projectile) {
        Entity direct = attacker;
        if (projectile) {
            ArrowMock arrow = new ArrowMock(server, UUID.randomUUID());
            server.registerEntity(arrow);
            arrow.teleport(victim.getLocation());
            arrow.setShooter((org.bukkit.projectiles.ProjectileSource) attacker);
            direct = arrow;
        }
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(direct, victim,
                projectile ? EntityDamageEvent.DamageCause.PROJECTILE : EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                DamageSource.builder(projectile ? DamageType.ARROW : DamageType.MOB_ATTACK)
                        .withCausingEntity(attacker).withDirectEntity(direct).build(), 10);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void arenaBlocksMeleeAndArrowsInBothDirectionsAfterPlayerLeaves(boolean projectile) {
        boss.setHealth(boss.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.5);
        server.getScheduler().performTicks(20);
        var mobs = world.getEntitiesByClass(Mob.class);
        assertEquals(5, mobs.size());
        Location inside = player.getLocation();
        player.teleport(inside.clone().add(100, 0, 0));
        for (Mob mob : mobs) {
            assertTrue(attack(player, mob, projectile).isCancelled());
            assertTrue(attack(mob, player, projectile).isCancelled());
        }
        player.teleport(inside);
        defeat();
        assertFalse(player.getInventory().contains(Material.DIAMOND));
        assertFalse(player.getInventory().contains(Material.NETHER_STAR));
    }

    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"CREATIVE", "SPECTATOR"})
    void privilegedPlayersCannotDamageBossOrShadows(GameMode mode) {
        boss.setHealth(boss.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.5);
        server.getScheduler().performTicks(20);
        player.setGameMode(mode);
        for (Mob mob : world.getEntitiesByClass(Mob.class)) {
            assertTrue(attack(player, mob, false).isCancelled());
            assertTrue(attack(player, mob, true).isCancelled());
        }
    }

    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"SURVIVAL", "ADVENTURE"})
    void eligiblePlayersCanFightBossAndShadowsInArena(GameMode mode) {
        boss.setHealth(boss.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.5);
        server.getScheduler().performTicks(20);
        player.setGameMode(mode);
        for (Mob mob : world.getEntitiesByClass(Mob.class)) {
            assertFalse(attack(player, mob, false).isCancelled());
            assertFalse(attack(player, mob, true).isCancelled());
            assertFalse(attack(mob, player, false).isCancelled());
            assertFalse(attack(mob, player, true).isCancelled());
        }
        defeat();
        assertTrue(player.getInventory().contains(Material.DIAMOND, 5));
        assertTrue(player.getInventory().contains(Material.NETHER_STAR));
    }

    @Test
    void displacedBossCannotDealOrReceivePlayerDamageUntilReturnedToArena() {
        Location center = boss.getLocation();
        boss.teleport(center.clone().add(100, 0, 0));
        assertTrue(attack(player, boss, true).isCancelled());
        assertTrue(attack(boss, player, true).isCancelled());
        server.getScheduler().performTicks(20);
        assertEquals(center, boss.getLocation());
        assertFalse(attack(player, boss, false).isCancelled());
    }

    @Test
    void arenaGuardDoesNotCancelUnrelatedCombatOutsideArena() {
        player.teleport(boss.getLocation().add(100, 0, 0));
        Entity unrelated = world.spawnEntity(player.getLocation(), EntityType.ZOMBIE);
        assertFalse(attack(player, unrelated, false).isCancelled());
        assertFalse(attack(unrelated, player, true).isCancelled());
    }

    @Test
    void phasePowersLastUntilFightEndsInsteadOfExpiringAfterOneMinute() {
        boss.setHealth(boss.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.3);
        server.getScheduler().performTicks(20);
        assertEquals(PotionEffect.INFINITE_DURATION, boss.getPotionEffect(PotionEffectType.SPEED).getDuration());
        assertEquals(PotionEffect.INFINITE_DURATION, boss.getPotionEffect(PotionEffectType.STRENGTH).getDuration());
        assertEquals(1, boss.getPotionEffect(PotionEffectType.SPEED).getAmplifier());
        assertEquals(1, boss.getPotionEffect(PotionEffectType.STRENGTH).getAmplifier());
    }

    @Test
    void cancelledAndZeroDamageDoNotEarnLoot() {
        hit(player, 10, true);
        hit(player, 0, false);
        defeat();
        assertFalse(player.getInventory().contains(Material.DIAMOND));
        assertFalse(player.getInventory().contains(Material.NETHER_STAR));
    }

    @Test
    void damageBelowThresholdDoesNotEarnLoot() {
        hit(player, 3, false);
        defeat();
        assertFalse(player.getInventory().contains(Material.DIAMOND));
    }

    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"CREATIVE", "SPECTATOR"})
    void privilegedModeDamageDoesNotEarnLootAfterReturningToSurvival(GameMode mode) {
        player.setGameMode(mode);
        hit(player, 30, false);
        player.setGameMode(GameMode.SURVIVAL);
        defeat();
        assertFalse(player.getInventory().contains(Material.DIAMOND));
        assertFalse(player.getInventory().contains(Material.NETHER_STAR));
    }

    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"CREATIVE", "SPECTATOR"})
    void switchingToPrivilegedModeBeforeDeathAwardsEssenceToEligibleContributor(GameMode mode) {
        PlayerMock second = server.addPlayer("Eligible");
        second.teleport(boss.getLocation());
        hit(player, 30, false);
        hit(second, 5, false);
        player.setGameMode(mode);
        defeat();
        assertFalse(player.getInventory().contains(Material.DIAMOND));
        assertFalse(player.getInventory().contains(Material.NETHER_STAR));
        assertTrue(second.getInventory().contains(Material.DIAMOND, 5));
        assertTrue(second.getInventory().contains(Material.NETHER_STAR));
    }

    @Test
    void adventureCombatStillEarnsLoot() {
        player.setGameMode(GameMode.ADVENTURE);
        hit(player, 10, false);
        defeat();
        assertTrue(player.getInventory().contains(Material.DIAMOND, 5));
        assertTrue(player.getInventory().contains(Material.NETHER_STAR));
    }

    @Test
    void qualifyingParticipantsReceiveLootOnce() {
        PlayerMock second = server.addPlayer("Second");
        second.teleport(boss.getLocation());
        hit(player, 10, false);
        hit(second, 5, false);
        defeat();
        defeat();
        assertTrue(player.getInventory().contains(Material.DIAMOND, 5));
        assertEquals(5, player.getInventory().all(Material.DIAMOND).values().stream().mapToInt(ItemStack::getAmount).sum());
        assertTrue(second.getInventory().contains(Material.DIAMOND, 5));
        assertTrue(player.getInventory().contains(Material.NETHER_STAR));
        assertFalse(second.getInventory().contains(Material.NETHER_STAR));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void fullInventoryDropsOnlyRewardsThatDoNotFit(int freeDiamondSpace) {
        // Paper 26.2 declares 43 slots, but PlayerInventoryMock only maps
        // storage/equipment through slot 40. Its addItem scans all 43 slots,
        // including the two unmapped slots, so occupy the whole mock inventory.
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Material.COBBLESTONE, 64));
        }
        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        player.getInventory().setItemInOffHand(new ItemStack(Material.COBBLESTONE, 64));
        if (freeDiamondSpace > 0) {
            player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 64 - freeDiamondSpace));
        }
        assertEquals(-1, player.getInventory().firstEmpty(), "The mock must have no spare slots");
        // getContents returns live mirrors: clone before changing expectations.
        ItemStack[] expectedInventory = Arrays.stream(player.getInventory().getContents())
                .map(ItemStack::clone).toArray(ItemStack[]::new);
        if (freeDiamondSpace > 0) expectedInventory[0].setAmount(64);
        hit(player, 10, false);
        defeat();
        defeat();

        int inventoryDiamonds = player.getInventory().all(Material.DIAMOND).values().stream()
                .mapToInt(ItemStack::getAmount).sum();
        assertEquals(freeDiamondSpace == 0 ? 0 : 64, inventoryDiamonds);
        assertFalse(player.getInventory().contains(Material.NETHER_STAR));
        var drops = world.getEntitiesByClass(Item.class);
        assertEquals(2, drops.size());
        assertEquals(5 - freeDiamondSpace, drops.stream().map(Item::getItemStack)
                .filter(item -> item.getType() == Material.DIAMOND).mapToInt(ItemStack::getAmount).sum());
        assertEquals(1, drops.stream().map(Item::getItemStack)
                .filter(item -> item.getType() == Material.NETHER_STAR).mapToInt(ItemStack::getAmount).sum());
        assertArrayEquals(expectedInventory, player.getInventory().getContents(),
                "Only the existing diamond stack may change; preserve every other slot");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void arrowRewardsBelongToShooterOnlyWhenInsideArena(boolean outside) {
        ArrowMock arrow = new ArrowMock(server, UUID.randomUUID());
        server.registerEntity(arrow);
        arrow.teleport(boss.getLocation());
        arrow.setShooter(player);
        if (outside) player.teleport(boss.getLocation().add(100, 0, 0));
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
                arrow, boss, EntityDamageEvent.DamageCause.PROJECTILE,
                DamageSource.builder(DamageType.ARROW).withCausingEntity(player).withDirectEntity(arrow).build(), 10);
        server.getPluginManager().callEvent(event);
        player.teleport(boss.getLocation());
        defeat();
        assertEquals(!outside, player.getInventory().contains(Material.DIAMOND, 5));
        assertEquals(!outside, player.getInventory().contains(Material.NETHER_STAR));
    }

    @Test
    void overkillDoesNotStealTopContributorReward() {
        PlayerMock second = server.addPlayer("Overkill");
        second.teleport(boss.getLocation());
        hit(player, 30, false);
        boss.setHealth(10);
        hit(second, 1000, false);
        defeat();
        assertTrue(player.getInventory().contains(Material.NETHER_STAR));
        assertFalse(second.getInventory().contains(Material.NETHER_STAR));
    }

    @Test
    void damageFromOutsideArenaDoesNotEarnLoot() {
        player.teleport(boss.getLocation().add(100, 0, 0));
        hit(player, 10, false);
        player.teleport(boss.getLocation());
        defeat();
        assertFalse(player.getInventory().contains(Material.DIAMOND));
    }

    @Test
    void bossCannotTargetPlayersOutsideArena() {
        player.teleport(boss.getLocation().add(100, 0, 0));
        EntityTargetLivingEntityEvent event = new EntityTargetLivingEntityEvent(
                boss, player, EntityTargetEvent.TargetReason.CLOSEST_PLAYER);
        server.getPluginManager().callEvent(event);
        assertTrue(event.isCancelled());
    }

    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"CREATIVE", "SPECTATOR"})
    void bossAndMinionCannotAcquirePrivilegedPlayer(GameMode mode) {
        boss.setHealth(boss.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.5);
        server.getScheduler().performTicks(20);
        player.setGameMode(mode);
        for (Mob mob : world.getEntitiesByClass(Mob.class)) {
            EntityTargetLivingEntityEvent event = new EntityTargetLivingEntityEvent(
                    mob, player, EntityTargetEvent.TargetReason.CLOSEST_PLAYER);
            server.getPluginManager().callEvent(event);
            assertTrue(event.isCancelled());
        }
    }

    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"CREATIVE", "SPECTATOR"})
    void bossAndMinionsReleaseExistingTargetAfterModeChange(GameMode mode) {
        boss.setHealth(boss.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.5);
        server.getScheduler().performTicks(20);
        var mobs = world.getEntitiesByClass(Mob.class);
        assertEquals(5, mobs.size());
        for (Mob mob : mobs) {
            mob.setTarget(player);
            assertEquals(player, mob.getTarget());
        }
        player.setGameMode(mode);
        server.getScheduler().performTicks(20);
        for (Mob mob : mobs) assertNull(mob.getTarget());
    }

    @Test
    void bossAndMinionsReleaseExistingTargetWhenPlayerLeavesArena() {
        boss.setHealth(boss.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.5);
        server.getScheduler().performTicks(20);
        var mobs = world.getEntitiesByClass(Mob.class);
        assertEquals(5, mobs.size());
        for (Mob mob : mobs) mob.setTarget(player);
        player.teleport(boss.getLocation().add(100, 0, 0));
        Location outside = player.getLocation();
        server.getScheduler().performTicks(20);
        for (Mob mob : mobs) assertNull(mob.getTarget());
        assertEquals(outside, player.getLocation());
    }

    @Test
    void adventureTargetInsideArenaRemainsValid() {
        player.setGameMode(GameMode.ADVENTURE);
        EntityTargetLivingEntityEvent event = new EntityTargetLivingEntityEvent(
                boss, player, EntityTargetEvent.TargetReason.CLOSEST_PLAYER);
        server.getPluginManager().callEvent(event);
        assertFalse(event.isCancelled());
        boss.setTarget(player);
        server.getScheduler().performTicks(20);
        assertEquals(player, boss.getTarget());
    }

    @Test
    void bossReturnsToArenaWithoutMovingPlayerOrTerrain() {
        Location center = boss.getLocation();
        Location playerLocation = player.getLocation();
        boss.teleport(center.clone().add(100, 0, 0));
        server.getScheduler().performTicks(20);
        assertEquals(center, boss.getLocation());
        assertEquals(playerLocation, player.getLocation());
        assertEquals(Material.GRASS_BLOCK, center.clone().subtract(0, 1, 0).getBlock().getType());
    }

    @Test
    void bossCannotCollectPlayerItemsBeforeCleanup() {
        assertFalse(boss.getCanPickupItems());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shadowsCannotCollectPlayerItemsBeforeDeathOrShutdown(boolean shutdown) {
        ItemStack belongings = new ItemStack(Material.DIAMOND_SWORD);
        Item dropped = world.dropItem(player.getLocation(), belongings);
        boss.setHealth(boss.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.5);
        server.getScheduler().performTicks(20);
        var mobs = new ArrayList<>(world.getEntitiesByClass(Mob.class));
        assertEquals(5, mobs.size());
        for (Mob mob : mobs) assertFalse(mob.getCanPickupItems(), mob.getType().name());
        Mob unrelated = (Mob) world.spawnEntity(player.getLocation(), EntityType.ZOMBIE);

        if (shutdown) server.getPluginManager().disablePlugin(plugin); else defeat();

        assertTrue(dropped.isValid());
        assertEquals(belongings, dropped.getItemStack());
        for (Mob mob : mobs) if (!mob.equals(boss) || shutdown) assertFalse(mob.isValid());
        assertTrue(unrelated.isValid());
        assertTrue(unrelated.getCanPickupItems());
    }

    @Test
    void phasesTriggerAtHealthThresholdsAndSummonOnlyOneWave() {
        double maximum = boss.getAttribute(Attribute.MAX_HEALTH).getValue();
        boss.setHealth(maximum * 0.67);
        server.getScheduler().performTicks(20);
        assertEquals(1, world.getEntitiesByClass(Zombie.class).size());
        assertFalse(boss.hasPotionEffect(PotionEffectType.SPEED));

        boss.setHealth(maximum * 0.66);
        server.getScheduler().performTicks(20);
        assertEquals(3, world.getEntitiesByClass(Zombie.class).size());
        assertEquals(2, world.getEntitiesByClass(org.bukkit.entity.WitherSkeleton.class).size());
        assertTrue(boss.hasPotionEffect(PotionEffectType.SPEED));
        assertFalse(boss.hasPotionEffect(PotionEffectType.STRENGTH));

        boss.setHealth(maximum * 0.34);
        server.getScheduler().performTicks(20);
        assertFalse(boss.hasPotionEffect(PotionEffectType.STRENGTH));
        boss.setHealth(maximum * 0.33);
        server.getScheduler().performTicks(20);
        assertTrue(boss.hasPotionEffect(PotionEffectType.STRENGTH));
        server.getScheduler().performTicks(20);
        assertEquals(3, world.getEntitiesByClass(Zombie.class).size());
        assertEquals(2, world.getEntitiesByClass(org.bukkit.entity.WitherSkeleton.class).size());

        defeat();
        assertTrue(world.getEntitiesByClass(org.bukkit.entity.WitherSkeleton.class).isEmpty());
        assertEquals(1, world.getEntitiesByClass(Zombie.class).size());
    }

    private Location prepareBlink() {
        Location target = boss.getLocation().add(10, 0, 0);
        target.setYaw(0);
        target.setPitch(0);
        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(target);
        boss.setHealth(boss.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.30);
        return target.clone().subtract(0, 0, 2);
    }

    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"SURVIVAL", "ADVENTURE"})
    void finalPhaseBlinksBehindParticipantWithoutMovingPlayerOrTerrain(GameMode mode) {
        Location destination = prepareBlink();
        player.setGameMode(mode);
        Location initial = boss.getLocation();
        Location playerLocation = player.getLocation();
        server.getScheduler().performTicks(100);
        assertEquals(initial, boss.getLocation());
        server.getScheduler().performTicks(20);
        assertEquals(destination, boss.getLocation());
        assertEquals(playerLocation, player.getLocation());
        assertEquals(Material.GRASS_BLOCK, destination.clone().subtract(0, 1, 0).getBlock().getType());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    void blinkRejectsMissingFloorAndBlockedFeetOrHead(int offset) {
        Location destination = prepareBlink();
        destination.clone().add(0, offset, 0).getBlock().setType(offset == -1 ? Material.AIR : Material.STONE);
        Location initial = boss.getLocation();
        server.getScheduler().performTicks(120);
        assertEquals(initial, boss.getLocation());
    }

    @Test
    void blinkIgnoresDeadPlayerAwaitingRespawn() {
        prepareBlink();
        player.setHealth(0);
        assertTrue(player.isDead());
        assertTrue(world.getPlayers().contains(player));
        Location initial = boss.getLocation();
        server.getScheduler().performTicks(120);
        assertEquals(initial, boss.getLocation());
    }

    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"SURVIVAL", "ADVENTURE"})
    void deadNearestPlayerDoesNotPreventBlinkToLivingParticipant(GameMode mode) {
        prepareBlink();
        player.setHealth(0);
        assertTrue(player.isDead());
        PlayerMock survivor = server.addPlayer("Survivor");
        survivor.setGameMode(mode);
        Location survivorLocation = boss.getLocation().add(15, 0, 0);
        survivorLocation.setYaw(0);
        survivorLocation.setPitch(0);
        survivor.teleport(survivorLocation);
        Location destination = survivorLocation.clone().subtract(0, 0, 2);
        server.getScheduler().performTicks(120);
        assertEquals(destination, boss.getLocation());
        assertEquals(survivorLocation, survivor.getLocation());
    }

    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"CREATIVE", "SPECTATOR"})
    void blinkIgnoresPlayersOutsideCombatModes(GameMode mode) {
        prepareBlink();
        player.setGameMode(mode);
        Location initial = boss.getLocation();
        server.getScheduler().performTicks(120);
        assertEquals(initial, boss.getLocation());
    }

    @ParameterizedTest
    @EnumSource(value = GameMode.class, names = {"CREATIVE", "SPECTATOR"})
    void blinkChoosesNearestParticipantAcrossSurvivalAndAdventure(GameMode observerMode) {
        Location destination = prepareBlink();
        player.setGameMode(GameMode.ADVENTURE);
        PlayerMock observer = server.addPlayer("Observer");
        observer.setGameMode(observerMode);
        observer.teleport(boss.getLocation().add(5, 0, 0));
        PlayerMock survivor = server.addPlayer("Survivor");
        survivor.teleport(boss.getLocation().add(15, 0, 0));
        Location observerLocation = observer.getLocation();
        Location survivorLocation = survivor.getLocation();

        server.getScheduler().performTicks(120);

        assertEquals(destination, boss.getLocation());
        assertEquals(observerLocation, observer.getLocation());
        assertEquals(survivorLocation, survivor.getLocation());
    }

    @Test
    void participantWhoLeavesArenaBeforeDeathReceivesNoReward() {
        hit(player, 10, false);
        player.teleport(boss.getLocation().add(100, 0, 0));
        defeat();
        assertFalse(player.getInventory().contains(Material.DIAMOND));
        assertFalse(player.getInventory().contains(Material.NETHER_STAR));
    }

    @Test
    void pluginDisableCleansFightWithoutLootOrRemovingUnrelatedMob() {
        hit(player, 10, false);
        boss.setHealth(boss.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.60);
        server.getScheduler().performTicks(20);
        var fightMobs = new ArrayList<>(world.getEntitiesByClass(Mob.class));
        assertEquals(5, fightMobs.size());
        Entity unrelated = world.spawnEntity(boss.getLocation(), EntityType.ZOMBIE);

        server.getPluginManager().disablePlugin(plugin);
        server.getScheduler().performTicks(40);

        assertTrue(fightMobs.stream().noneMatch(Entity::isValid));
        assertTrue(unrelated.isValid());
        assertFalse(player.getInventory().contains(Material.DIAMOND));
        assertFalse(player.getInventory().contains(Material.NETHER_STAR));
        assertTrue(world.getEntitiesByClass(Item.class).isEmpty());
    }

    @Test
    void removedBossCleansShadowsWithoutLootAndAllowsNewSummoner() {
        hit(player, 10, false);
        boss.setHealth(boss.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.60);
        server.getScheduler().performTicks(20);
        var fightMobs = new ArrayList<>(world.getEntitiesByClass(Mob.class));
        assertEquals(5, fightMobs.size());
        boss.remove();

        server.getScheduler().performTicks(20);

        assertTrue(fightMobs.stream().noneMatch(Entity::isValid));
        assertFalse(player.getInventory().contains(Material.DIAMOND));
        assertFalse(player.getInventory().contains(Material.NETHER_STAR));
        assertTrue(world.getEntitiesByClass(Item.class).isEmpty());
        PlayerMock second = server.addPlayer("NewSummoner");
        second.teleport(player.getLocation());
        assertTrue(second.performCommand("herobrine"));
        assertEquals(1, world.getEntitiesByClass(Zombie.class).size());
    }

    @Test
    void oldFightCleanupDoesNotRemoveReplacementOrTransferDamageCredit() {
        hit(player, 30, false);
        boss.setHealth(boss.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.60);
        server.getScheduler().performTicks(20);
        var oldMobs = new ArrayList<>(world.getEntitiesByClass(Mob.class));
        assertEquals(5, oldMobs.size());
        boss.remove();
        PlayerMock second = server.addPlayer("ReplacementSummoner");
        second.teleport(player.getLocation());
        assertTrue(second.performCommand("herobrine"));
        Zombie replacement = world.getEntitiesByClass(Zombie.class).stream()
                .filter(zombie -> !oldMobs.contains(zombie)).findFirst().orElseThrow();

        // Both fight timers run on this tick; the old one must not clear the new fight.
        server.getScheduler().performTicks(20);
        assertTrue(oldMobs.stream().noneMatch(Entity::isValid));
        assertTrue(replacement.isValid());
        boss = replacement;
        second.teleport(boss.getLocation());
        hit(second, 5, false);
        defeat();
        defeat();

        assertFalse(player.getInventory().contains(Material.DIAMOND));
        assertFalse(player.getInventory().contains(Material.NETHER_STAR));
        assertEquals(5, second.getInventory().all(Material.DIAMOND).values().stream()
                .mapToInt(ItemStack::getAmount).sum());
        assertEquals(1, second.getInventory().all(Material.NETHER_STAR).values().stream()
                .mapToInt(ItemStack::getAmount).sum());
    }
}

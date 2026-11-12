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

    // MockBukkit lacks the despawn flag. Preserve its value without replacing
    // command dispatch, damage events, arena scheduling or reward handling.
    private static final class ArenaZombieMock extends ZombieMock {
        private boolean removeWhenFarAway = true;

        ArenaZombieMock(ServerMock server) {
            super(server, UUID.randomUUID());
            // ZombieMock only initializes health and movement speed.
            registerAttribute(Attribute.ATTACK_DAMAGE);
            registerAttribute(Attribute.FOLLOW_RANGE);
            registerAttribute(Attribute.KNOCKBACK_RESISTANCE);
        }

        @Override public void setRemoveWhenFarAway(boolean remove) { removeWhenFarAway = remove; }
        @Override public boolean getRemoveWhenFarAway() { return removeWhenFarAway; }
    }

    private static final class ArenaSkeletonMock extends WitherSkeletonMock {
        private boolean removeWhenFarAway = true;

        ArenaSkeletonMock(ServerMock server) { super(server, UUID.randomUUID()); }

        @Override public void setRemoveWhenFarAway(boolean remove) { removeWhenFarAway = remove; }
        @Override public boolean getRemoveWhenFarAway() { return removeWhenFarAway; }
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
        for (int slot = 0; slot < player.getInventory().getStorageContents().length; slot++) {
            player.getInventory().setItem(slot, new ItemStack(Material.COBBLESTONE, 64));
        }
        // MockBukkit's addItem also scans equipment slots. Keep those occupied
        // with real equipment while exercising the storage overflow path.
        player.getInventory().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        player.getInventory().setItemInOffHand(new ItemStack(Material.COBBLESTONE, 64));
        if (freeDiamondSpace > 0) {
            player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 64 - freeDiamondSpace));
        }
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
        assertEquals(64, player.getInventory().getItem(1).getAmount());
        assertEquals(Material.COBBLESTONE, player.getInventory().getItem(1).getType());
        assertEquals(Material.DIAMOND_HELMET, player.getInventory().getHelmet().getType());
        assertEquals(Material.COBBLESTONE, player.getInventory().getItemInOffHand().getType());
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

    @Test
    void finalPhaseBlinksBehindSurvivalPlayerWithoutMovingPlayerOrTerrain() {
        Location destination = prepareBlink();
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

    @Test
    void deadNearestPlayerDoesNotPreventBlinkToLivingParticipant() {
        prepareBlink();
        player.setHealth(0);
        assertTrue(player.isDead());
        PlayerMock survivor = server.addPlayer("Survivor");
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
    @EnumSource(value = GameMode.class, names = {"CREATIVE", "SPECTATOR", "ADVENTURE"})
    void blinkIgnoresPlayersOutsideSurvival(GameMode mode) {
        prepareBlink();
        player.setGameMode(mode);
        Location initial = boss.getLocation();
        server.getScheduler().performTicks(120);
        assertEquals(initial, boss.getLocation());
    }

    @Test
    void participantWhoLeavesArenaBeforeDeathReceivesNoReward() {
        hit(player, 10, false);
        player.teleport(boss.getLocation().add(100, 0, 0));
        defeat();
        assertFalse(player.getInventory().contains(Material.DIAMOND));
        assertFalse(player.getInventory().contains(Material.NETHER_STAR));
    }
}

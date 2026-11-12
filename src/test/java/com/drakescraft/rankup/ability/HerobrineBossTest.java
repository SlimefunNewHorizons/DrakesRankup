package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.entity.ZombieMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.ArrayList;
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

        ArenaZombieMock(ServerMock server) { super(server, UUID.randomUUID()); }

        @Override public void setRemoveWhenFarAway(boolean remove) { removeWhenFarAway = remove; }
        @Override public boolean getRemoveWhenFarAway() { return removeWhenFarAway; }
    }

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        // MockBukkit does not implement cosmetic lightning; gameplay stays real.
        world = new WorldMock(Material.GRASS_BLOCK, 64) {
            @Override public LightningStrike strikeLightningEffect(Location location) { return null; }

            @Override public Entity spawnEntity(Location location, EntityType type) {
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
}

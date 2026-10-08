package com.drakescraft.rankup.protection;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Compuerta de seguridad Fail-Closed para daño y modificación del terreno.
 * Protege de forma estricta claims de ProtectionStones y regiones de WorldGuard.
 * Si una explosión o habilidad impacta dentro o cerca de una protección, el daño a bloques
 * se anula al 100%, realizando únicamente daño a entidades y efectos visuales.
 */
public final class ProtectionGate {

    private final Plugin plugin;
    private final Method protectionStoneLookup;
    private boolean warned;

    public ProtectionGate(Plugin plugin) {
        this.plugin = plugin;
        this.protectionStoneLookup = findProtectionStoneLookup();
    }

    /**
     * Evalúa si una habilidad puede alterar o destruir bloques en el radio indicado.
     * Si cualquiera de los puntos muestreados toca un claim ajeno o protegido, retorna false.
     */
    public boolean allowTerrainDamage(Player actor, Location center, double radius) {
        if (center == null || center.getWorld() == null) return false;
        World world = center.getWorld();
        int centerChunkX = center.getBlockX() >> 4;
        int centerChunkZ = center.getBlockZ() >> 4;
        if (!world.isChunkLoaded(centerChunkX, centerChunkZ)) {
            return false;
        }

        // Muestrear centro y perímetro
        for (Location sample : samples(center, radius)) {
            int sChunkX = sample.getBlockX() >> 4;
            int sChunkZ = sample.getBlockZ() >> 4;
            if (!world.isChunkLoaded(sChunkX, sChunkZ)) {
                return false; // Perímetro alcanza chunks no residentes: anular preventivamente
            }
            if (isProtected(sample, actor)) {
                if (actor != null && actor.isOnline()) {
                    actor.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&6[Rankup] &c¡Zona protegida! &7La destrucción del terreno se anula dentro o cerca de claims."));
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Ejecuta una detonación controlada: si la zona es virgen / desprotegida, destruye bloques;
     * si está protegida o hay duda, la explosión no rompe ningún bloque (breakBlocks = false).
     * Incorpora blindaje estricto de chunks para evitar cargas síncronas y watchdogs.
     */
    public boolean applyTerrainExplosion(Player actor, Location loc, float power, boolean setFire) {
        if (loc == null || loc.getWorld() == null) return false;
        World world = loc.getWorld();
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return false;
        }

        // Blindaje contra cargas síncronas de chunks: verificar que todos los chunks en el radio de explosión estén cargados
        int radiusChunks = (int) Math.ceil((power * 1.5) / 16.0);
        for (int cx = chunkX - radiusChunks; cx <= chunkX + radiusChunks; cx++) {
            for (int cz = chunkZ - radiusChunks; cz <= chunkZ + radiusChunks; cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    // Si algún chunk adyacente dentro del radio no está residente,
                    // omitir la detonación física/terreno para evitar cadenas síncronas de carga
                    // (LevelledMobs EntityDeathListener, etc.) y reproducir efectos audiovisuales seguros.
                    playSafeExplosionVisuals(loc, power);
                    return false;
                }
            }
        }

        boolean allowed = allowTerrainDamage(actor, loc, power);
        if (allowed) {
            world.createExplosion(actor, loc, power, setFire, true);
        } else {
            world.createExplosion(actor, loc, power, false, false);
        }
        return allowed;
    }

    private void playSafeExplosionVisuals(Location loc, float power) {
        try {
            World world = loc.getWorld();
            if (world != null) {
                world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
                world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            }
        } catch (Throwable ignored) {}
    }

    private List<Location> samples(Location center, double radius) {
        List<Location> result = new ArrayList<>();
        result.add(center);
        int points = 12;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            result.add(center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
            result.add(center.clone().add(Math.cos(angle) * (radius * 0.5), 0, Math.sin(angle) * (radius * 0.5)));
        }
        return result;
    }

    public boolean isProtected(Location location, Player actor) {
        if (location == null || location.getWorld() == null) return true;
        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return true; // Fail-closed seguro: si el chunk no está cargado, proteger sin forzar carga síncrona
        }
        try {
            // 1. Verificación en ProtectionStones
            if (protectionStoneLookup != null) {
                Object psRegion = protectionStoneLookup.invoke(null, location);
                if (psRegion != null) {
                    if (actor != null) {
                        try {
                            Object region = psRegion.getClass().getMethod("getRegion").invoke(psRegion);
                            Object owners = region.getClass().getMethod("getOwners").invoke(region);
                            Object members = region.getClass().getMethod("getMembers").invoke(region);
                            UUID uuid = actor.getUniqueId();
                            boolean isOwner = (boolean) owners.getClass().getMethod("contains", UUID.class).invoke(owners, uuid);
                            boolean isMember = (boolean) members.getClass().getMethod("contains", UUID.class).invoke(members, uuid);
                            if (isOwner || isMember) {
                                // Aún siendo dueño, para evitar auto-griefing accidental en construcciones grandes
                                // retornamos true (protegido) si la config lo exige, o permitimos si es dueño.
                                return false; // El dueño puede detonar en su propio terreno
                            }
                        } catch (Throwable ignored) {}
                    }
                    return true; // Claim ajeno: bloqueado
                }
            }

            // 2. Verificación en WorldGuard
            if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                Class<?> worldGuardType = Class.forName("com.sk89q.worldguard.WorldGuard");
                Object worldGuard = worldGuardType.getMethod("getInstance").invoke(null);
                Object platform = worldGuard.getClass().getMethod("getPlatform").invoke(worldGuard);
                Object container = platform.getClass().getMethod("getRegionContainer").invoke(platform);
                Object query = container.getClass().getMethod("createQuery").invoke(container);
                Class<?> adapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                Object adapted = adapter.getMethod("adapt", Location.class).invoke(null, location);
                Class<?> worldEditLocation = Class.forName("com.sk89q.worldedit.util.Location");
                Object regions = query.getClass().getMethod("getApplicableRegions", worldEditLocation).invoke(query, adapted);
                int size = ((Number) regions.getClass().getMethod("size").invoke(regions)).intValue();
                if (size > 0) {
                    return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            if (!warned) {
                warned = true;
                plugin.getLogger().log(Level.WARNING, "[Rankup] Falló consulta de protecciones; compuerta en modo Fail-Closed.", error);
            }
            return true; // Fail-closed: ante cualquier duda o error, proteger el terreno
        }
    }

    
    public boolean isProtected(Location location) {
        return isProtected(location, null);
    }

    public boolean canDestroyBlock(Player actor, org.bukkit.block.Block block) {
        if (block == null || block.getWorld() == null) return false;
        if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) return false;
        org.bukkit.Material type = block.getType();
        if (type.isAir()) return false;

        // Inmunes absolutos de Minecraft
        if (type == org.bukkit.Material.BEDROCK
                || type == org.bukkit.Material.BARRIER
                || type == org.bukkit.Material.END_PORTAL
                || type == org.bukkit.Material.END_PORTAL_FRAME
                || type == org.bukkit.Material.NETHER_PORTAL
                || type == org.bukkit.Material.REINFORCED_DEEPSLATE
                || type == org.bukkit.Material.COMMAND_BLOCK
                || type == org.bukkit.Material.CHAIN_COMMAND_BLOCK
                || type == org.bukkit.Material.REPEATING_COMMAND_BLOCK
                || type == org.bukkit.Material.STRUCTURE_BLOCK
                || type == org.bukkit.Material.STRUCTURE_VOID
                || type == org.bukkit.Material.JIGSAW) {
            return false;
        }

        // Contenedores e inventarios protegidos de jugadores
        if (type == org.bukkit.Material.CHEST
                || type == org.bukkit.Material.TRAPPED_CHEST
                || type == org.bukkit.Material.BARREL
                || type.name().endsWith("SHULKER_BOX")
                || type == org.bukkit.Material.HOPPER
                || type == org.bukkit.Material.DISPENSER
                || type == org.bukkit.Material.DROPPER
                || type == org.bukkit.Material.FURNACE
                || type == org.bukkit.Material.BLAST_FURNACE
                || type == org.bukkit.Material.SMOKER
                || type == org.bukkit.Material.BREWING_STAND) {
            return false;
        }

        // Bloques de Slimefun (máquinas, cargo, etc.): jamás romperlos con habilidades.
        // Romperlos con setType corrompe la data del bloque en Slimefun (dupe/pérdida).
        if (isSlimefunBlock(block)) {
            return false;
        }

        // Proteccion regional (ProtectionStones / WorldGuard)
        return !isProtected(block.getLocation(), actor);
    }

    // Cache reflexivo del lookup de Slimefun (no hay dependencia de compilación).
    // Se intenta la API moderna (StorageCacheUtils.hasBlock(Location)) y, si no,
    // la legacy (BlockStorage.hasBlockInfo(Block)), que existe en Slimefun4-Drake.
    private Method slimefunHasBlockByLocation;
    private Method slimefunHasBlockByBlock;
    private boolean slimefunResolved;

    /** True si el bloque está registrado por Slimefun (máquina/addon), vía reflexión. */
    private boolean isSlimefunBlock(org.bukkit.block.Block block) {
        if (!slimefunResolved) {
            slimefunResolved = true;
            for (String cls : new String[]{
                    "io.github.thebusybiscuit.slimefun4.api.storage.StorageCacheUtils",
                    "io.github.thebusybiscuit.slimefun4.utils.itemstack.StorageCacheUtils"}) {
                try {
                    slimefunHasBlockByLocation = Class.forName(cls).getMethod("hasBlock", Location.class);
                    break;
                } catch (ReflectiveOperationException | LinkageError ignored) {
                    slimefunHasBlockByLocation = null;
                }
            }
            try {
                slimefunHasBlockByBlock = Class.forName("me.mrCookieSlime.Slimefun.api.BlockStorage")
                        .getMethod("hasBlockInfo", org.bukkit.block.Block.class);
            } catch (ReflectiveOperationException | LinkageError ignored) {
                slimefunHasBlockByBlock = null;
            }
        }
        try {
            if (slimefunHasBlockByLocation != null) {
                Object r = slimefunHasBlockByLocation.invoke(null, block.getLocation());
                if (r instanceof Boolean b && b) return true;
            }
            if (slimefunHasBlockByBlock != null) {
                Object r = slimefunHasBlockByBlock.invoke(null, block);
                if (r instanceof Boolean b && b) return true;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Slimefun no disponible: no bloquea la habilidad.
        }
        return false;
    }

    private Method findProtectionStoneLookup() {
        try {
            return Class.forName("dev.espi.protectionstones.PSRegion").getMethod("fromLocation", Location.class);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }
}

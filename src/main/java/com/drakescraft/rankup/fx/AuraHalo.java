package com.drakescraft.rankup.fx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Halo 3D sobre la cabeza del jugador hecho con una entidad display (1.20+): una estrella que gira
 * con el color de su aura. Es pasajero del jugador, asi que lo sigue sin tareas de teletransporte,
 * y el giro lo interpola el cliente (una orden cada medio segundo, no una por tick).
 *
 * El patron viene del DisplaySuit de Chagui68 (MultiverseCreatures): cada pieza lleva una etiqueta
 * propia y el id de su dueno, la caja de colision se deja en cero para que no se trague los golpes
 * de nadie, y al arrancar se barren las piezas huerfanas de un cierre sucio anterior.
 */
public final class AuraHalo {

    /** Etiqueta que identifica una pieza de halo de este plugin en el mundo. */
    public static final String TAG = "drakesrankup_halo";
    private static final String OWNER_PREFIX = "drakesrankup_halo_owner:";
    private static final float HEIGHT = 2.45f;
    private static final float SCALE = 0.55f;
    /** Cuarto de vuelta por orden: el cliente interpola, y asi el giro no es ambiguo. */
    private static final float STEP = (float) (Math.PI / 2.0);
    private static final int INTERPOLATION_TICKS = 10;

    private final Map<UUID, ItemDisplay> halos = new HashMap<>();
    private final Map<UUID, Float> angles = new HashMap<>();

    /** Crea el halo si falta y le da el siguiente cuarto de vuelta, con el color del estilo. */
    public void sync(Player player, AuraStyle style) {
        UUID id = player.getUniqueId();
        ItemDisplay display = halos.get(id);
        if (display == null || !display.isValid() || !player.getPassengers().contains(display)) {
            remove(player);
            display = spawn(player);
            if (display == null) {
                return;
            }
            mount(player, display);
            halos.put(id, display);
        }
        display.setGlowColorOverride(style.primary());
        float angle = angles.merge(id, STEP, Float::sum);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(INTERPOLATION_TICKS);
        display.setTransformation(pose(angle));
    }

    /** Quita el halo del jugador, si lo tiene. */
    public void remove(Player player) {
        UUID id = player.getUniqueId();
        angles.remove(id);
        ItemDisplay display = halos.remove(id);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    public boolean has(Player player) {
        return halos.containsKey(player.getUniqueId());
    }

    /** Quita todos los halos (cierre del plugin). */
    public void removeAll() {
        for (ItemDisplay display : halos.values()) {
            if (display.isValid()) {
                display.remove();
            }
        }
        halos.clear();
        angles.clear();
    }

    /** Barre piezas huerfanas de un cierre sucio anterior (se llama al arrancar). */
    public static int sweepOrphans() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(ItemDisplay.class)) {
                if (isHalo(entity)) {
                    entity.remove();
                    removed++;
                }
            }
        }
        return removed;
    }

    /** Pieza etiquetada como halo de este plugin. */
    public static boolean isHalo(Entity entity) {
        return entity.getScoreboardTags().contains(TAG);
    }

    private ItemDisplay spawn(Player player) {
        try {
            return player.getWorld().spawn(player.getLocation(), ItemDisplay.class, d -> {
                d.setItemStack(new ItemStack(Material.NETHER_STAR));
                d.setBillboard(Display.Billboard.FIXED);
                d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                d.setBrightness(new Display.Brightness(15, 15));
                d.setGlowing(true);
                d.setPersistent(false);
                // Caja a cero: una display con caja real se traga los golpes del jugador.
                d.setDisplayWidth(0f);
                d.setDisplayHeight(0f);
                d.setViewRange(0.8f);
                d.setTransformation(pose(0f));
                d.addScoreboardTag(TAG);
                d.addScoreboardTag(OWNER_PREFIX + player.getUniqueId());
            });
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void mount(Player player, ItemDisplay display) {
        if (!player.getPassengers().contains(display)) {
            player.addPassenger(display);
        }
    }

    private static Transformation pose(float angle) {
        return new Transformation(new Vector3f(0f, HEIGHT, 0f),
                new AxisAngle4f(angle, 0f, 1f, 0f),
                new Vector3f(SCALE, SCALE, SCALE),
                new AxisAngle4f(0f, 0f, 0f, 1f));
    }
}

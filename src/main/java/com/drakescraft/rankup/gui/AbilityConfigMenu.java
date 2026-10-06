package com.drakescraft.rankup.gui;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Menú interactivo de Configuración y Superposición de Habilidades.
 * Permite a cada jugador activar o desactivar selectivamente técnicas individuales
 * (Golpe Serio, Santoryu, Frutas del Diablo, Super Salto, Jujutsu, Filos, Magnetismo).
 */
public class AbilityConfigMenu implements InventoryHolder {

    private final DrakesRankupPlugin plugin;
    private final Player player;
    private Inventory inv;

    public AbilityConfigMenu(DrakesRankupPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public Inventory getInventory() {
        return this.inv;
    }

    public void open() {
        this.inv = Bukkit.createInventory(this, 45, ChatColor.translateAlternateColorCodes('&', "&8⚙ &6&lSUPERPOSICIÓN DE PODERES"));

        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, bg);
        }

        Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings s = plugin.getRankManager().getPlayerSettings(player.getUniqueId());

        // 1. Golpe Serio de Saitama (Slot 10)
        boolean punchOn = s.isSeriousPunchEnabled();
        inv.setItem(10, buildToggleItem(
                punchOn,
                Material.FIRE_CHARGE,
                "&c&lGolpe Serio de Saitama &7(Death Punch)",
                tier >= 41 || tier >= 50,
                Arrays.asList(
                        "&8▪ &eTipo: &fDestrucción Telúrica y Daño Verdadero",
                        "&8▪ &7Estado: " + (punchOn ? "&a&l[ACTIVADO]" : "&c&l[DESACTIVADO]"),
                        "&8▪ &fDesactivado previene cráteres telúricos accidentales.",
                        "&8▪ &bActivación: &fPuño limpio o Shift + Golpe.",
                        "&8▪ &6Penetración: &fIgnora escudos y sets Infinity de SF.",
                        "&eClic para alternar estado."
                )
        ));

        // 2. Estilo Tres Espadas de Zoro (Slot 12)
        boolean zoroOn = s.isSantoryuEnabled();
        inv.setItem(12, buildToggleItem(
                zoroOn,
                Material.NETHERITE_SWORD,
                "&2&lEstilo Tres Espadas (Santoryu - Zoro)",
                tier >= 24,
                Arrays.asList(
                        "&8▪ &eTipo: &fEspadas Voladoras 3D y Corte Sanzen Sekai",
                        "&8▪ &7Estado: " + (zoroOn ? "&a&l[ACTIVADO]" : "&c&l[DESACTIVADO]"),
                        "&8▪ &fClic Derecho: &aEspada Voladora 3D que taja a distancia.",
                        "&8▪ &fShift + Clic Derecho: &aSanzen Sekai (Impulso cortante).",
                        "&eClic para alternar estado."
                )
        ));

        // 3. Frutas del Diablo & Haki (Slot 14)
        boolean fruitOn = s.isDevilFruitEnabled();
        inv.setItem(14, buildToggleItem(
                fruitOn,
                Material.ENCHANTED_GOLDEN_APPLE,
                "&6&lTécnicas de Frutas del Diablo & Haki",
                tier >= 30,
                Arrays.asList(
                        "&8▪ &eTipo: &fPoderes de Gomu, Mera, Ope, Pika, Gura y Haki",
                        "&8▪ &7Estado: " + (fruitOn ? "&a&l[ACTIVADO]" : "&c&l[DESACTIVADO]"),
                        "&8▪ &fPermite desatar técnicas activas en combate.",
                        "&eClic para alternar estado."
                )
        ));

        // 4. Super Impulso Sónico (Doble Salto 100 Bloques) (Slot 16)
        boolean leapOn = s.isSonicLeapEnabled();
        inv.setItem(16, buildToggleItem(
                leapOn,
                Material.FEATHER,
                "&b&lSuper Impulso Sónico (100 Bloques)",
                tier >= 31,
                Arrays.asList(
                        "&8▪ &eTipo: &fPropulsión aerodinámica y vuelo supersónico",
                        "&8▪ &7Estado: " + (leapOn ? "&a&l[ACTIVADO]" : "&c&l[DESACTIVADO]"),
                        "&8▪ &fDoble salto en el aire lanza al jugador hacia el frente",
                        "  &fcon estela sónica y planeo controlado.",
                        "&eClic para alternar estado."
                )
        ));

        // 5. Energía Maldita Jujutsu (Slot 28)
        boolean curseOn = s.isCursedEnergyEnabled();
        inv.setItem(28, buildToggleItem(
                curseOn,
                Material.WITHER_ROSE,
                "&5&lEnergía Maldita (Black Flash & Desmantelar)",
                tier >= 20,
                Arrays.asList(
                        "&8▪ &eTipo: &fDestellos Negros y Cortes de Sukuna en golpe",
                        "&8▪ &7Estado: " + (curseOn ? "&a&l[ACTIVADO]" : "&c&l[DESACTIVADO]"),
                        "&8▪ &fDesactiva los efectos de impacto oscuro si prefieres combate vanilla.",
                        "&eClic para alternar estado."
                )
        ));

        // 6. Filos Danzantes / Aura Kill (Slot 30)
        boolean bladesOn = s.isDancingBladesEnabled();
        inv.setItem(30, buildToggleItem(
                bladesOn,
                Material.DIAMOND_SWORD,
                "&d&lFilos Danzantes (Aura Kill Perimetral)",
                tier >= 70,
                Arrays.asList(
                        "&8▪ &eTipo: &fDaño perimetral a monstruos hostiles cercanos",
                        "&8▪ &7Estado: " + (bladesOn ? "&a&l[ACTIVADO]" : "&c&l[DESACTIVADO]"),
                        "&8▪ &fOtorga daño constante a criaturas en tu radio.",
                        "&eClic para alternar estado."
                )
        ));

        // 7. Vórtice Magnético (Jiki Jiki) (Slot 32)
        boolean magnetOn = s.isMagnetVortexEnabled();
        inv.setItem(32, buildToggleItem(
                magnetOn,
                Material.LODESTONE,
                "&9&lVórtice Magnético (Atracción 200 Bloques)",
                tier >= 60,
                Arrays.asList(
                        "&8▪ &eTipo: &fAtracción electromagnética masiva de drops",
                        "&8▪ &7Estado: " + (magnetOn ? "&a&l[ACTIVADO]" : "&c&l[DESACTIVADO]"),
                        "&8▪ &fAtrae objetos del suelo a gran velocidad.",
                        "&eClic para alternar estado."
                )
        ));

        // 8. Partículas y Auras (Slot 34)
        int lvl = s.getParticleLevel();
        String lbl = lvl == 2 ? "&aCompletas" : (lvl == 1 ? "&eReducidas" : "&cApagadas");
        inv.setItem(34, createItem(
                Material.BLAZE_POWDER,
                "&e&lDensidad de Partículas y Auras",
                Arrays.asList(
                        "&8▪ &eNivel Actual: " + lbl,
                        "&8▪ &7Completas &8-> &7Auras densas con rayos y estelas.",
                        "&8▪ &7Reducidas &8-> &7Auras sutiles de bajo impacto.",
                        "&8▪ &7Apagadas  &8-> &7Sin partículas.",
                        "&eClic para cambiar nivel de aura."
                )
        ));

        // Botón Volver a Transformaciones (Slot 40)
        inv.setItem(40, createItem(Material.ARROW, "&c⬅ Volver al Menú de Transformaciones"));

        player.openInventory(this.inv);
    }

    public void handleClick(InventoryClickEvent e, Player p) {
        int slot = e.getRawSlot();
        PlayerSettings s = plugin.getRankManager().getPlayerSettings(p.getUniqueId());

        if (slot == 40) {
            new TransformationMenu(plugin, p).open();
            return;
        }

        if (slot == 10) {
            boolean next = !s.isSeriousPunchEnabled();
            s.setSeriousPunchEnabled(next);
            plugin.getRankManager().savePlayerData();
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, next ? 1.5f : 0.7f);
            p.sendMessage("§e[Rankup] §7Golpe Serio de Saitama: " + (next ? "§aActivado" : "§cDesactivado"));
            open();
            return;
        }

        if (slot == 12) {
            boolean next = !s.isSantoryuEnabled();
            s.setSantoryuEnabled(next);
            plugin.getRankManager().savePlayerData();
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, next ? 1.5f : 0.7f);
            p.sendMessage("§e[Rankup] §7Estilo Tres Espadas de Zoro: " + (next ? "§aActivado" : "§cDesactivado"));
            open();
            return;
        }

        if (slot == 14) {
            boolean next = !s.isDevilFruitEnabled();
            s.setDevilFruitEnabled(next);
            plugin.getRankManager().savePlayerData();
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, next ? 1.5f : 0.7f);
            p.sendMessage("§e[Rankup] §7Técnicas de Frutas del Diablo & Haki: " + (next ? "§aActivado" : "§cDesactivado"));
            open();
            return;
        }

        if (slot == 16) {
            boolean next = !s.isSonicLeapEnabled();
            s.setSonicLeapEnabled(next);
            plugin.getRankManager().savePlayerData();
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, next ? 1.5f : 0.7f);
            p.sendMessage("§e[Rankup] §7Super Impulso Sónico (100 Bloques): " + (next ? "§aActivado" : "§cDesactivado"));
            open();
            return;
        }

        if (slot == 28) {
            boolean next = !s.isCursedEnergyEnabled();
            s.setCursedEnergyEnabled(next);
            plugin.getRankManager().savePlayerData();
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, next ? 1.5f : 0.7f);
            p.sendMessage("§e[Rankup] §7Energía Maldita Jujutsu: " + (next ? "§aActivado" : "§cDesactivado"));
            open();
            return;
        }

        if (slot == 30) {
            boolean next = !s.isDancingBladesEnabled();
            s.setDancingBladesEnabled(next);
            plugin.getRankManager().savePlayerData();
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, next ? 1.5f : 0.7f);
            p.sendMessage("§e[Rankup] §7Filos Danzantes / Aura Kill: " + (next ? "§aActivado" : "§cDesactivado"));
            open();
            return;
        }

        if (slot == 32) {
            boolean next = !s.isMagnetVortexEnabled();
            s.setMagnetVortexEnabled(next);
            plugin.getRankManager().savePlayerData();
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, next ? 1.5f : 0.7f);
            p.sendMessage("§e[Rankup] §7Vórtice Magnético: " + (next ? "§aActivado" : "§cDesactivado"));
            open();
            return;
        }

        if (slot == 34) {
            int lvl = (s.getParticleLevel() + 2) % 3;
            s.setParticleLevel(lvl);
            s.setParticlesEnabled(lvl > 0);
            plugin.getRankManager().savePlayerData();
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            String etiqueta = lvl == 2 ? "§aCompletas" : (lvl == 1 ? "§eReducidas" : "§cApagadas");
            p.sendMessage("§e[Rankup] §7Densidad de partículas: " + etiqueta);
            open();
            return;
        }
    }

    private ItemStack buildToggleItem(boolean enabled, Material mat, String name, boolean unlocked, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> list = new ArrayList<>();
            for (String l : lore) {
                list.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            if (unlocked) {
                list.add(ChatColor.translateAlternateColorCodes('&', "&a✔ Habilidad Desbloqueada"));
            } else {
                list.add(ChatColor.translateAlternateColorCodes('&', "&c✖ Bloqueada por rango insuficiente"));
            }
            meta.setLore(list);
            if (enabled) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> list = new ArrayList<>();
            for (String l : lore) {
                list.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            meta.setLore(list);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            item.setItemMeta(meta);
        }
        return item;
    }
}

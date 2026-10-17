package com.drakescraft.rankup.gui;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransformationMenu implements InventoryHolder {

    private final DrakesRankupPlugin plugin;
    private final Player player;
    private Inventory inv;

    public TransformationMenu(DrakesRankupPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public Inventory getInventory() {
        return this.inv;
    }

    public void open() {
        this.inv = Bukkit.createInventory(this, 54, ChatColor.translateAlternateColorCodes('&', "&8⚡ &6&lTRANSFORMATIONS & ABILITIES"));

        // Fill background with black glass panes
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, bg);
        }

        Rank rank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
        String active = settings.getActiveTransformation();

        // 1. SSJ God (Tier 35)
        inv.setItem(10, buildTransformationItem(
                tier >= 35,
                "SSJ_GOD".equalsIgnoreCase(active),
                Material.REDSTONE_BLOCK,
                "&c&lSuper Saiyan God",
                35,
                Arrays.asList(
                        "&8▪ &eType: &fCrimson Divine Ki",
                        "&8▪ &aEffects: &fRegeneration II, Absorption II and Divine Agility (5m).",
                        "&8▪ &bActivation: &fHold sneak (Shift) until it detonates."
                )
        ));

        // 2. SSJ Blue (Tier 36)
        inv.setItem(11, buildTransformationItem(
                tier >= 36,
                "SSJ_BLUE".equalsIgnoreCase(active),
                Material.LAPIS_BLOCK,
                "&9&lSuper Saiyan Blue",
                36,
                Arrays.asList(
                        "&8▪ &eType: &fBlue Super Saiyan Divine Ki",
                        "&8▪ &aEffects: &fStrength II, Speed II and Resistance I for 5 minutes.",
                        "&8▪ &bActivation: &fHold sneak (Shift) until it detonates."
                )
        ));

        // 3. Ultra Instinto (MUI) (Tier 46)
        inv.setItem(12, buildTransformationItem(
                tier >= 46,
                "MUI".equalsIgnoreCase(active),
                Material.NETHER_STAR,
                "&f&lUltra Instinct (MUI)",
                46,
                Arrays.asList(
                        "&8▪ &eType: &fAngelic State",
                        "&8▪ &aEffects: &fMiraculous full attack evasion for 60 seconds.",
                        "&8▪ &cStrain: &7Repeated use within 2m causes bodily fatigue.",
                        "&8▪ &bActivation: &fHold sneak (Shift) until it detonates."
                )
        ));

        // 4. Ultra Ego (Mega Instinto) (Tier 47)
        inv.setItem(13, buildTransformationItem(
                tier >= 47,
                "ULTRA_EGO".equalsIgnoreCase(active),
                Material.PURPLE_GLAZED_TERRACOTTA,
                "&5&lUltra Ego",
                47,
                Arrays.asList(
                        "&8▪ &eType: &fGod of Destruction Power",
                        "&8▪ &aEffects: &fThe lower your health, the greater your destructive damage for 5 minutes.",
                        "&8▪ &bActivation: &fHold sneak (Shift) until it detonates."
                )
        ));

        // 5. Gohan Beast (Tier 45)
        inv.setItem(14, buildTransformationItem(
                tier >= 45,
                "GOHAN_BEAST".equalsIgnoreCase(active),
                Material.AMETHYST_CLUSTER,
                "&d&lGohan Beast",
                45,
                Arrays.asList(
                        "&8▪ &eType: &fUnleashed Hybrid Fury",
                        "&8▪ &aEffects: &f+75% critical chance and magenta lightning for 5m.",
                        "&8▪ &bActivation: &fHold sneak (Shift) until it detonates."
                )
        ));

        // 6. Broly Berserker (Tier 34)
        inv.setItem(15, buildTransformationItem(
                tier >= 34,
                "BROLY_LSSJ".equalsIgnoreCase(active),
                Material.EMERALD_BLOCK,
                "&a&lLegendary Broly (LSSJ)",
                34,
                Arrays.asList(
                        "&8▪ &eType: &fDestructive Berserker Power",
                        "&8▪ &aEffects: &fStrength III, Resistance II and knockback immunity (5m).",
                        "&8▪ &bActivation: &fHold sneak (Shift) until it detonates."
                )
        ));

        // 7. Espada de Vegetto (Tier 48)
        inv.setItem(16, buildTransformationItem(
                tier >= 48,
                "VEGETTO_SWORD".equalsIgnoreCase(active),
                Material.GOLDEN_SWORD,
                "&e&lSword of Light (Vegetto)",
                48,
                Arrays.asList(
                        "&8▪ &eType: &fSaiyan Fusion Technique",
                        "&8▪ &aEffects: &fFires a piercing golden beam for 14 blocks.",
                        "&8▪ &bActivation: &fShift + right-click with a sword in hand."
                )
        ));

        // 8. Super Saiyajin 2 (Tier 33)
        inv.setItem(17, buildTransformationItem(
                tier >= 33,
                "SSJ_2".equalsIgnoreCase(active),
                Material.LIGHTNING_ROD,
                "&e&lSuper Saiyan 2",
                33,
                Arrays.asList(
                        "&8▪ &eType: &fAscended Golden Ki with Bio-Electricity",
                        "&8▪ &aEffects: &fPersistent lightning arcs, Strength II and Speed II (5m).",
                        "&8▪ &bActivation: &fHold sneak (Shift) until it detonates."
                )
        ));

        // 9. Fruta Mera Mera (Tier 55)
        inv.setItem(19, buildTransformationItem(
                tier >= 55,
                "MERA_MERA".equalsIgnoreCase(active),
                Material.BLAZE_POWDER,
                "&6&lMera Mera Fruit (Ace / Sabo)",
                55,
                Arrays.asList(
                        "&8▪ &eType: &fLogia Devil Fruit (Fire)",
                        "&8▪ &aEffects: &fFire immunity, Fire Fist (Hiken) and solar Entei.",
                        "&8▪ &bActivation: &fRight-click (Hiken) / shift + right-click (Entei)."
                )
        ));

        // 10. Fruta Ope Ope (Tier 65)
        inv.setItem(20, buildTransformationItem(
                tier >= 65,
                "OPE_OPE".equalsIgnoreCase(active),
                Material.HEART_OF_THE_SEA,
                "&b&lOpe Ope Fruit (Trafalgar Law)",
                65,
                Arrays.asList(
                        "&8▪ &eType: &fSurgical Operation Fruit",
                        "&8▪ &aEffects: &fROOM dome and Shambles (position swap).",
                        "&8▪ &bActivation: &fRight-click (ROOM) / shift + right-click (Shambles)."
                )
        ));

        // 11. Fruta Pika Pika (Tier 75)
        inv.setItem(21, buildTransformationItem(
                tier >= 75,
                "PIKA_PIKA".equalsIgnoreCase(active),
                Material.GLOWSTONE,
                "&e&lPika Pika Fruit (Kizaru)",
                75,
                Arrays.asList(
                        "&8▪ &eType: &fAbsolute Light Fruit",
                        "&8▪ &aEffects: &fPhoton speed and Yata no Kagami light leap.",
                        "&8▪ &bActivation: &fShift + left-click (Light Leap)."
                )
        ));

        // 12. Fruta Gura Gura (Tier 85)
        inv.setItem(22, buildTransformationItem(
                tier >= 85,
                "GURA_GURA".equalsIgnoreCase(active),
                Material.IRON_BLOCK,
                "&f&lGura Gura Fruit (Whitebeard)",
                85,
                Arrays.asList(
                        "&8▪ &eType: &fSeismic Earthquake Fruit",
                        "&8▪ &aEffects: &fA strike that cracks space and launches enemies into the air.",
                        "&8▪ &bActivation: &fLeft-click in combat."
                )
        ));

        // 13. Vórtice Magnético (Tier 60)
        inv.setItem(23, buildTransformationItem(
                tier >= 60,
                "JIKI_MAGNET".equalsIgnoreCase(active),
                Material.LODESTONE,
                "&b&lMagnetic Vortex (Jiki Jiki)",
                60,
                Arrays.asList(
                        "&8▪ &eType: &fAttractive Polar Magnetism",
                        "&8▪ &aEffects: &fInstantly attracts all items within a 200-block radius.",
                        "&8▪ &bActivation: &fShift + right-click or /rankup magnet."
                )
        ));

        // 14. Filos Danzantes / Aura Kill (Tier 70)
        inv.setItem(24, buildTransformationItem(
                tier >= 70,
                "DANCING_BLADES".equalsIgnoreCase(active),
                Material.DIAMOND_SWORD,
                "&c&lDancing Blades (Aura Kill)",
                70,
                Arrays.asList(
                        "&8▪ &eType: &fTelekinetic Weapon Mastery",
                        "&8▪ &aEffects: &fSwords and axes float and strike on their own within 10 blocks.",
                        "&8▪ &bActivation: &fClick to toggle autonomous Aura Kill."
                )
        ));

        // 15. Sun God Nika (Gear 5) (Tier 88)
        inv.setItem(25, buildTransformationItem(
                tier >= 88,
                "GEAR_FIVE".equalsIgnoreCase(active),
                Material.NETHER_STAR,
                "&f&l☀ Sun God Nika (Gear 5)",
                88,
                Arrays.asList(
                        "&8▪ &eType: &fShonen Warrior of Liberation",
                        "&8▪ &aEffects: &fDrums of liberation, cartoon bouncing with no fall damage,",
                        "  &fKaminari lightning throws and the colossal Bajrang Gun (5m).",
                        "&8▪ &bActivation: &fShift + left-click (Bajrang) / left-click (Kaminari)."
                )
        ));

        // 16. Luffy Gomu Gomu (Gear 2, 3 y 4 Bounceman) (Tier 30)
        inv.setItem(28, buildTransformationItem(
                tier >= 30,
                "GOMU_GOMU".equalsIgnoreCase(active),
                Material.SLIME_BALL,
                "&c&lGomu Gomu Fruit (Luffy)",
                30,
                Arrays.asList(
                        "&8▪ &eType: &fRubber Human",
                        "&8▪ &aEffects: &fGear 2 (steam), Gear 3 (2.8x giant) and Gear 4 (Bounceman).",
                        "  &fGear 4 continuously bounces on the ground just like in the anime!",
                        "&8▪ &bActivation: &fSneak + right-click (Gear 3) / sneak + left-click (Gear 4)."
                )
        ));

        // 17. Maestría Santoryu (Zoro) (Tier 24)
        inv.setItem(29, buildTransformationItem(
                tier >= 24,
                "SANTORYU".equalsIgnoreCase(active),
                Material.IRON_SWORD,
                "&2&lThree-Sword Style (Santoryu - Zoro)",
                24,
                Arrays.asList(
                        "&8▪ &eType: &fWano Swordsman Mastery",
                        "&8▪ &aPositive Effects: &f+25% permanent physical sword damage",
                        "  &fand a ranged Flying Dragon wind slash.",
                        "&8▪ &bActivation: &fRight-click with a sword in combat."
                )
        ));

        // 18. Kaio-ken Aumentado (Tier 92)
        inv.setItem(30, buildTransformationItem(
                tier >= 92,
                "KAIOKEN".equalsIgnoreCase(active),
                Material.RED_CANDLE,
                "&c&lKaio-ken (x3 / x10 / x20)",
                92,
                Arrays.asList(
                        "&8▪ &eType: &fDivine Saiyan Multiplier",
                        "&8▪ &aPositive Effects: &fSpeed III, Strength II, Jump II and Free Flight (5m).",
                        "&8▪ &bActivation: &fHold sneak (Shift) until it detonates."
                )
        ));

        // 19. Vuelo de Ki Supersónico (Tier 31+)
        inv.setItem(31, createItem(
                tier >= 31 ? Material.FIREWORK_ROCKET : Material.GUNPOWDER,
                tier >= 31 ? "&b&l⚡ Supersonic Ki Flight" : "&c🔒 Ki Flight (Requires Tier 31)",
                "&8▪ &eType: &fShonen Aerial Propulsion",
                "&8▪ &aEffects: &f100-block super boost with W + double jump (scaling cooldown).",
                "",
                tier >= 31 ? (settings.isKiFlightEnabled() ? "&a✔ ENABLED &7(Click to toggle)" : "&c✖ DISABLED &7(Click to enable)") : "&cLocked by rank."
        ));

        // Desequipar (Slot 40)
        inv.setItem(40, createItem(Material.BARRIER, "&c&lUnequip Active Transformation",
                "&7Removes the selected transformation and restores",
                "&7your current rank's natural passive aura.",
                "",
                "&eClick to unequip"
        ));

        // Volver al menú de Rangos (Slot 49)
        inv.setItem(49, createItem(Material.ARROW, "&a◀ Back to Rank Menu", "&7Open the main /rankup interface"));

        player.openInventory(this.inv);
    }

    private ItemStack buildTransformationItem(boolean unlocked, boolean isEquipped, Material mat, String name, int reqTier, List<String> details) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Required Level: &6Tier " + reqTier);
        lore.add("");
        lore.addAll(details);
        lore.add("");

        if (isEquipped) {
            lore.add("&a✔ EQUIPPED AND ACTIVE!");
            return createItem(Material.ENCHANTED_BOOK, name + " &8(&aEQUIPPED&8)", lore);
        } else if (unlocked) {
            lore.add("&e▶ Unlocked. Click to equip it as active.");
            return createItem(mat, name + " &8(&eAvailable&8)", lore);
        } else {
            lore.add("&c🔒 Locked. You must advance to Tier " + reqTier + ".");
            return createItem(Material.RED_STAINED_GLASS_PANE, name + " &8(&cLocked&8)", lore);
        }
    }

    public void handleClick(InventoryClickEvent e, Player p) {
        int slot = e.getRawSlot();
        Rank rank = plugin.getRankManager().getPlayerRank(p.getUniqueId());
        int tier = (rank != null) ? rank.getTier() : 0;
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(p.getUniqueId());

        // Back to /rankup (Slot 49)
        if (slot == 49) {
            new RankupMenu(plugin, p, 0).open();
            return;
        }

        // Unequip (Slot 40)
        if (slot == 40) {
            settings.setActiveTransformation(null);
            plugin.getRankManager().savePlayerData();
            if (plugin.getDragonBallListener() != null) {
                plugin.getDragonBallListener().revertTransformations(p);
            }
            if (plugin.getOnePieceListener() != null) {
                plugin.getOnePieceListener().revertTransformations(p);
            }
            p.setFlySpeed(0.10f);
            if (!plugin.hasExternalFlight(p) && p.getGameMode() != org.bukkit.GameMode.CREATIVE && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                p.setFlying(false);
                p.setAllowFlight(false);
            }
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.8f);
            p.sendMessage("§b[Transformation] §7Your transformation has been unequipped. Aura and flight restored to your rank baseline.");
            open();
            return;
        }

        // Toggle Ki Flight (Slot 31)
        if (slot == 31) {
            if (tier < 31 && !settings.isKiFlightEnabled() && !p.hasPermission("drakesrankup.staff") && !plugin.getStaffManager().isAngel(p.getUniqueId())) {
                p.sendMessage("§c[Rankup] You need to reach Tier 31 (Saiyan) to use Ki Flight.");
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            boolean newState = !settings.isKiFlightEnabled();
            settings.setKiFlightEnabled(newState);
            plugin.getRankManager().savePlayerData();
            p.setFlySpeed(0.10f);
            if (!newState && !plugin.hasExternalFlight(p) && p.getGameMode() != org.bukkit.GameMode.CREATIVE && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                p.setFlying(false);
                p.setAllowFlight(false);
            }
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            p.sendMessage("§b[Rankup] §7Supersonic Ki Flight: " + (newState ? "§aEnabled" : "§cDisabled (normal speed restored)"));
            open();
            return;
        }

        // Selection Slots
        String newTrans = null;
        int reqTier = 0;

        if (slot == 10) { newTrans = "SSJ_GOD"; reqTier = 35; }
        else if (slot == 11) { newTrans = "SSJ_BLUE"; reqTier = 36; }
        else if (slot == 12) { newTrans = "MUI"; reqTier = 46; }
        else if (slot == 13) { newTrans = "ULTRA_EGO"; reqTier = 47; }
        else if (slot == 14) { newTrans = "GOHAN_BEAST"; reqTier = 45; }
        else if (slot == 15) { newTrans = "BROLY_LSSJ"; reqTier = 34; }
        else if (slot == 16) { newTrans = "VEGETTO_SWORD"; reqTier = 48; }
        else if (slot == 17) { newTrans = "SSJ_2"; reqTier = 33; }
        else if (slot == 19) { newTrans = "MERA_MERA"; reqTier = 55; }
        else if (slot == 20) { newTrans = "OPE_OPE"; reqTier = 65; }
        else if (slot == 21) { newTrans = "PIKA_PIKA"; reqTier = 75; }
        else if (slot == 22) { newTrans = "GURA_GURA"; reqTier = 85; }
        else if (slot == 23) { newTrans = "JIKI_MAGNET"; reqTier = 60; }
        else if (slot == 24) { newTrans = "DANCING_BLADES"; reqTier = 70; }
        else if (slot == 25) { newTrans = "GEAR_FIVE"; reqTier = 88; }
        else if (slot == 28) { newTrans = "GOMU_GOMU"; reqTier = 30; }
        else if (slot == 29) { newTrans = "SANTORYU"; reqTier = 24; }
        else if (slot == 30) { newTrans = "KAIOKEN"; reqTier = 92; }

        if (newTrans != null) {
            if (tier < reqTier) {
                p.sendMessage("§c[Transformation] Tier " + reqTier + " is required to unlock this technique.");
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            settings.setActiveTransformation(newTrans);
            plugin.getRankManager().savePlayerData();
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1.0f, 1.2f);
            p.sendMessage("§a[Transformation] §fYou have equipped: §e" + newTrans + "§f!");
            p.sendMessage("§7Use the ability controls to unleash its power.");

            // Activación inmediata para Gear 5 si se selecciona directamente
            if ("GEAR_FIVE".equalsIgnoreCase(newTrans)) {
                plugin.getOnePieceListener().activateGear5(p);
            }
            open();
        }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        return createItem(mat, name, Arrays.asList(lore));
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat != null ? mat : Material.STONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> coloredLore = new ArrayList<>();
            for (String l : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', l));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

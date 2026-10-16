package com.drakescraft.rankup.gui;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.*;

public class RankupMenu implements InventoryHolder {

    private final DrakesRankupPlugin plugin;
    @Getter
    private final Player player;
    @Getter
    private final int page; // 0 to 4 (Divisions I to V)
    public static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###,###,###.##");

    private static final String[] DIVISION_NAMES = {
            "Division I: Science & Nen",
            "Division II: Shinobi & Sorcery",
            "Division III: Pirate Path & Reapers",
            "Division IV: Z Warriors & Monarch",
            "Division V: Supreme Transcendence",
            "Division VI: Supreme Curses",
            "Division VII: Shinigami Reiatsu",
            "Division VIII: Monarchs & Demons",
            "Division IX: Cosmic Legends",
            "Division X: Multiversal Gods"
    };

    public RankupMenu(DrakesRankupPlugin plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = Math.max(0, Math.min(9, page));
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(this, 54, "§8§lDRAKES RANKUP §8- " + DIVISION_NAMES[page]);

        // Background fillers
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glass);
        }

        // Division Tabs for current 5-division cluster (0-4 or 5-9)
        int clusterStart = (page / 5) * 5;
        int[] tabSlots = {0, 1, 2, 6, 7};
        for (int i = 0; i < 5; i++) {
            int d = clusterStart + i;
            if (d >= 10) break;
            boolean current = (d == page);
            Material tabMat = current ? Material.GOLD_BLOCK : Material.IRON_BARS;
            String prefix = current ? "&6&l▶ " : "&7";
            inv.setItem(tabSlots[i], createItem(tabMat, prefix + DIVISION_NAMES[d],
                    "&7Tiers: &f" + ((d * 10) + 1) + " to " + ((d * 10) + 10),
                    current ? "&aActive tab" : "&eClick to view this division"
            ));
        }

        // Fast cluster switch buttons (Slots 3 and 5)
        if (page < 5) {
            inv.setItem(5, createItem(Material.COMPASS, "&e&lDivisions VI - X ▶", "&7Click to explore Tiers 51 to 100"));
        } else {
            inv.setItem(3, createItem(Material.COMPASS, "&e&l◀ Divisions I - V", "&7Click to return to Tiers 1 to 50"));
        }

        // Player Info (Slot 4)
        Rank currentRank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        int currentTier = currentRank != null ? currentRank.getTier() : 0;
        double monedero = plugin.getEconomy() != null ? plugin.getEconomy().getBalance(player) : 0.0;
        double bolsa = plugin.getRankManager().getBolsa(player.getUniqueId());
        double balance = monedero + bolsa; // fondos de ascenso: bolsa + monedero
        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());

        inv.setItem(4, createItem(Material.PLAYER_HEAD, "&e&l" + player.getName(),
                "&7Current rank: " + (currentRank != null ? currentRank.getDisplayName() : "&7None"),
                "&7Tier: &6" + currentTier + "/100",
                "&7Rebirth: &e" + settings.getRebirthCount() + "/50 &7(+" + (settings.getRebirthCount() * 3) + "% damage)",
                "&7Wallet: &a$" + MONEY_FORMAT.format(monedero),
                "&7Rankup fund: &6$" + MONEY_FORMAT.format(bolsa) + " &8(/rankup bolsa)",
                "&7Ability: " + (currentRank != null && currentRank.getAbilityType() != null ? "&b" + currentRank.getAbilityType().getName() : "&7None"),
                "&7Kinetic Push: " + (currentRank != null && currentRank.isHasKineticPush() ? "&aUnlocked" : "&cLocked"),
                "",
                "&8Personal settings:",
                "&7Pushes: " + (settings.isKineticPushEnabled() ? "&aON" : "&cOFF"),
                "&7Particles: " + (settings.isParticlesEnabled() ? "&aON" : "&cOFF"),
                "&7Abilities: " + (settings.isAbilitiesEnabled() ? "&aON" : "&cOFF")
        ));

        // Settings Toggle Button (Slot 8)
        inv.setItem(8, createItem(Material.REPEATER, "&b&lQuick Settings",
                "&7Click to toggle your effects:",
                "&8▪ &7Pushes: " + (settings.isKineticPushEnabled() ? "&aEnabled" : "&cDisabled"),
                "&8▪ &7Particles: " + (settings.isParticlesEnabled() ? "&aEnabled" : "&cDisabled"),
                "",
                "&eClick to toggle Kinetic Push"
        ));

        // Center 10 Ranks for Current Division
        int[] slots = {19, 20, 21, 22, 23, 28, 29, 30, 31, 32};
        int startTier = (page * 10) + 1;
        int endTier = Math.min(startTier + 9, 100);

        int slotIdx = 0;
        for (int t = startTier; t <= endTier; t++) {
            Rank r = plugin.getRankManager().getRankByTier(t);
            if (r == null) continue;
            int targetSlot = slots[slotIdx++];

            String statusTitle;
            List<String> lore = new ArrayList<>();
            lore.add("&8" + r.getDivision());
            lore.add("&7Cost: &6$" + MONEY_FORMAT.format(r.getCost()));
            lore.add("&7Type: " + (r.isPermanent() ? "&aPermanent (Anchor)" : "&eTemporary (14 days)"));
            lore.add("");
            lore.add("&e&nPerks and Benefits:&r");
            for (String perk : r.getPerks()) {
                lore.add(" &8▪ &f" + ChatColor.translateAlternateColorCodes('&', perk));
            }
            if (r.getAbilityType() != null && !r.getAbilityType().name().equals("NONE")) {
                lore.add(" &8▪ &bAbility: " + r.getAbilityType().getName());
            }
            if (r.isHasKineticPush()) {
                lore.add(" &8▪ &aKinetic Push: Power x" + r.getPushMultiplier() + " (CD: " + r.getPushCooldownSeconds() + "s)");
            }
            lore.add("");

            if (currentTier >= t) {
                statusTitle = "&a✔ " + r.getDisplayName() + " &8(&aUnlocked&8)";
                lore.add("&aYou have already mastered this rank.");
                inv.setItem(targetSlot, createItem(Material.LIME_DYE, statusTitle, lore));
            } else if (t == currentTier + 1) {
                statusTitle = "&e⚡ " + r.getDisplayName() + " &8(&eAvailable!&8)";
                boolean canAfford = balance >= r.getCost();
                if (canAfford) {
                    lore.add("&a▶ You have enough money! Click to rank up.");
                } else {
                    lore.add("&c✖ You need &e$" + MONEY_FORMAT.format(r.getCost() - balance) + " &cmore to rank up.");
                }
                inv.setItem(targetSlot, createItem(r.getIcon(), statusTitle, lore));
            } else {
                statusTitle = "&c🔒 " + r.getDisplayName() + " &8(&cLocked&8)";
                lore.add("&cYou must unlock earlier ranks first.");
                inv.setItem(targetSlot, createItem(Material.RED_STAINED_GLASS_PANE, statusTitle, lore));
            }
        }

        // Bottom Controls
        // Slot 45: Núcleo de Estabilidad de Rango (Mantención)
        if (currentRank == null || currentTier == 0) {
            inv.setItem(45, createItem(Material.RESPAWN_ANCHOR, "&8🏺 Stability Core",
                    "&7You do not have an active rank.",
                    "&7Reach Tier 1 to begin your journey."));
        } else if (currentRank.isPermanent()) {
            inv.setItem(45, createItem(Material.BEACON, "&a&l🏺 Permanent Rank",
                    "&7Your rank: " + currentRank.getDisplayName(),
                    "&8" + currentRank.getDivision(),
                    "",
                    "&a✔ ETERNAL RANK (ANCHOR)!",
                    "&7This rank acts as a sacred anchor.",
                    "&7It requires no tribute and never decays."));
        } else {
            long remainingMs = plugin.getRankManager().getRemainingMaintenanceMs(player.getUniqueId());
            long days = remainingMs / 86400000L;
            long hours = (remainingMs % 86400000L) / 3600000L;
            double mCost = currentRank.getMaintenanceCost();
            boolean affordM = balance >= mCost;

            Material mIcon;
            String statusColor;
            String statusText;
            if (days >= 3) {
                mIcon = Material.RESPAWN_ANCHOR;
                statusColor = "&a";
                statusText = "OPTIMAL";
            } else if (days >= 1) {
                mIcon = Material.CRYING_OBSIDIAN;
                statusColor = "&e";
                statusText = "UNSTABLE";
            } else {
                mIcon = Material.REDSTONE_BLOCK;
                statusColor = "&c";
                statusText = "CRITICAL";
            }

            int filledBars = (int) Math.max(0, Math.min(10, (days * 10) / 14));
            StringBuilder bar = new StringBuilder("&8[");
            for (int b = 0; b < 10; b++) {
                if (b < filledBars) {
                    bar.append(statusColor).append("▮");
                } else {
                    bar.append("&8▯");
                }
            }
            bar.append("&8]");

            inv.setItem(45, createItem(mIcon, "&6&l🏺 Rank Stability Core",
                    "&7Rank: " + currentRank.getDisplayName() + " &8(Temporary)",
                    "&7Stability: " + statusColor + statusText + " " + bar.toString(),
                    "&7Time remaining: " + statusColor + days + "d " + hours + "h",
                    "",
                    "&7Maintenance cost: &6$" + MONEY_FORMAT.format(mCost) + " &7(+14 days)",
                    "",
                    affordM ? "&a▶ Click to feed the Core and renew" : "&c✖ Insufficient funds to renew"));
        }

        // Help & Info Button (Slot 46)
        inv.setItem(46, createItem(Material.BOOK, "&e&lRankup Information",
                "&7100 Shonen Anime Ranks",
                "&7100% in-game economy (Dragmas)",
                "&7Fully compatible with God VIPs",
                "",
                "&fUseful commands: &6/rankup&f, &6/rankup max, &6/rankup mantener"
        ));

        // Transformation Menu Button (Slot 47)
        inv.setItem(47, createItem(Material.DRAGON_BREATH, "&d&l⚡ Transformations Menu",
                "&7Open the anime transformations selector,",
                "&7abilities, Ki Flight, and special techniques.",
                "",
                "&eClick to open /transform"
        ));

        if (page > 0) {
            inv.setItem(48, createItem(Material.ARROW, "&a◀ Previous Division", "&7Go to " + DIVISION_NAMES[page - 1]));
        }

        Rank next = plugin.getRankManager().getNextRank(player.getUniqueId());
        if (next != null) {
            boolean afford = balance >= next.getCost();
            inv.setItem(49, createItem(Material.NETHER_STAR, "&6&lRANK UP TO THE NEXT RANK",
                    "&7Next: " + next.getDisplayName(),
                    "&7Price: &6$" + MONEY_FORMAT.format(next.getCost()),
                    "",
                    afford ? "&a✔ Click to pay and rank up now" : "&c✖ You do not have enough funds"
            ));
        } else {
            inv.setItem(49, createItem(Material.BEACON, "&6&lMAXIMUM RANK REACHED!", "&aYou completed all 100 DrakesCraft anime ranks. Use /rebirth to begin again."));
        }

        if (page < 9) {
            inv.setItem(50, createItem(Material.ARROW, "&aNext Division ▶", "&7Go to " + DIVISION_NAMES[page + 1]));
        }

        // Rankup Kit Button (Slot 51)
        int div = currentTier > 0 ? Math.min(10, ((currentTier - 1) / 10) + 1) : 0;
        boolean canClaimKit = div > 0 && plugin.getKitManager() != null && plugin.getKitManager().canClaim(player.getUniqueId(), div);
        long remMs = (div > 0 && plugin.getKitManager() != null) ? plugin.getKitManager().getRemainingCooldownMs(player.getUniqueId(), div) : 0;
        long h = remMs / (1000 * 60 * 60);
        long m = (remMs % (1000 * 60 * 60)) / (1000 * 60);

        inv.setItem(51, createItem(Material.CHEST_MINECART, "&6&l🎁 Claim Rankup Kit",
                "&7Get your current division's daily kit with",
                "&7armor, custom-enchanted weapons, and",
                "&7healing Senzu Beans.",
                "",
                div == 0 ? "&c✖ Requires at least Tier 1" :
                        (canClaimKit ? "&a✔ AVAILABLE! Click to claim" : "&c⏳ Available in &e" + h + "h " + m + "m"),
                "",
                "&eClick to run /rankup kit"
        ));

        // Max Rankup Button (Slot 52)
        inv.setItem(52, createItem(Material.EMERALD, "&a&lRANK UP AS FAR AS POSSIBLE",
                "&7Calculate your bank balance and advance",
                "&7through every possible consecutive tier.",
                "",
                "&eClick to run /rankup max"
        ));

        player.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent e, Player p) {
        int slot = e.getRawSlot();

        // Division Tabs for current cluster
        int clusterStart = (page / 5) * 5;
        int[] tabSlots = {0, 1, 2, 6, 7};
        for (int i = 0; i < 5; i++) {
            if (slot == tabSlots[i]) {
                int targetDiv = clusterStart + i;
                if (targetDiv < 10) {
                    new RankupMenu(plugin, p, targetDiv).open();
                }
                return;
            }
        }

        // Jump between Clusters (Slot 5: 0-4 -> 5, Slot 3: 5-9 -> 0)
        if (slot == 5 && page < 5) {
            new RankupMenu(plugin, p, 5).open();
            return;
        }
        if (slot == 3 && page >= 5) {
            new RankupMenu(plugin, p, 0).open();
            return;
        }

        // Settings Toggle (Slot 8)
        if (slot == 8) {
            PlayerSettings s = plugin.getRankManager().getPlayerSettings(p.getUniqueId());
            s.setKineticPushEnabled(!s.isKineticPushEnabled());
            plugin.getRankManager().savePlayerData();
            p.sendMessage("§b[Rankup] §7Kinetic Push: " + (s.isKineticPushEnabled() ? "§aEnabled" : "§cDisabled"));
            new RankupMenu(plugin, p, page).open();
            return;
        }

        // Maintenance Núcleo Button (Slot 45)
        if (slot == 45) {
            Rank rank = plugin.getRankManager().getPlayerRank(p.getUniqueId());
            if (rank == null || rank.getTier() == 0) {
                p.sendMessage("§cYou do not have an active rank that requires maintenance.");
                return;
            }
            if (rank.isPermanent()) {
                p.sendMessage("§aYour current rank (" + rank.getDisplayName() + "§a) is permanent and requires no maintenance.");
                return;
            }
            boolean ok = plugin.getRankManager().processMaintenance(p);
            if (ok) {
                new RankupMenu(plugin, p, page).open();
            }
            return;
        }

        // Transformation Menu Button (Slot 47)
        if (slot == 47) {
            new TransformationMenu(plugin, p).open();
            return;
        }

        // Previous Page
        if (slot == 48 && page > 0) {
            new RankupMenu(plugin, p, page - 1).open();
            return;
        }

        // Next Page
        if (slot == 50 && page < 9) {
            new RankupMenu(plugin, p, page + 1).open();
            return;
        }

        // Claim Kit Button (Slot 51)
        if (slot == 51) {
            if (plugin.getKitManager() != null) {
                plugin.getKitManager().claimKit(p);
                new RankupMenu(plugin, p, page).open();
            }
            return;
        }

        // Max Rankup Button (Slot 52)
        if (slot == 52) {
            p.closeInventory();
            p.performCommand("rankup max");
            return;
        }

        // Rankup Next Button (Slot 49)
        if (slot == 49) {
            p.closeInventory();
            p.performCommand("rankup");
            return;
        }

        // Click on current available rank to buy directly
        int[] rankSlots = {19, 20, 21, 22, 23, 28, 29, 30, 31, 32};
        int startTier = (page * 10) + 1;
        int currentTier = plugin.getRankManager().getPlayerTier(p.getUniqueId());

        for (int i = 0; i < rankSlots.length; i++) {
            if (slot == rankSlots[i]) {
                int clickedTier = startTier + i;
                if (clickedTier == currentTier + 1) {
                    p.closeInventory();
                    p.performCommand("rankup");
                }
                return;
            }
        }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        return createItem(mat, name, Arrays.asList(lore));
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

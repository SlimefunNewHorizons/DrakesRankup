package com.drakescraft.rankup.ability;

import com.drakescraft.rankup.DrakesRankupPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/** Carne de zombie cocinable (horno, ahumador, fogata): sana y sin hambre ni veneno. */
public class PurifiedMeat implements Listener {

    private final DrakesRankupPlugin plugin;
    private final NamespacedKey key;

    public PurifiedMeat(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "carne_purificada");
        ItemStack out = item();
        Bukkit.addRecipe(new FurnaceRecipe(new NamespacedKey(plugin, "carne_purificada_horno"), out, Material.ROTTEN_FLESH, 0.35f, 200));
        Bukkit.addRecipe(new SmokingRecipe(new NamespacedKey(plugin, "carne_purificada_ahumador"), out, Material.ROTTEN_FLESH, 0.35f, 100));
        Bukkit.addRecipe(new CampfireRecipe(new NamespacedKey(plugin, "carne_purificada_fogata"), out, Material.ROTTEN_FLESH, 0.35f, 600));
    }

    public void unregister() {
        for (String s : List.of("horno", "ahumador", "fogata")) Bukkit.removeRecipe(new NamespacedKey(plugin, "carne_purificada_" + s));
    }

    private ItemStack item() {
        ItemStack it = new ItemStack(Material.COOKED_BEEF);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName("§6§lCarne Purificada");
        m.setLore(List.of("§7Carne de zombie cocinada y purificada.", "§aRestaura hambre y cura."));
        m.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(m);
        return it;
    }

    private boolean isPurified(ItemStack it) {
        return it != null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /** Clasico queda vanilla: la carne podrida no se cocina en mundos donde el rankup esta desactivado. */
    @EventHandler
    public void onCook(BlockCookEvent e) {
        if (isPurified(e.getResult()) && !plugin.isWorldAllowed(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler
    public void onEat(PlayerItemConsumeEvent e) {
        if (!isPurified(e.getItem())) return;
        Player p = e.getPlayer();
        p.removePotionEffect(PotionEffectType.HUNGER);
        p.removePotionEffect(PotionEffectType.POISON);
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 0));
    }
}

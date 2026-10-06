package com.drakescraft.rankup.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TransformationGuiListener implements Listener {

    private final Map<UUID, Long> clickDebounce = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        boolean isTrans = event.getInventory().getHolder() instanceof TransformationMenu;
        boolean isConfig = event.getInventory().getHolder() instanceof AbilityConfigMenu;
        if (!isTrans && !isConfig) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ClickType click = event.getClick();
        if (click.isKeyboardClick() || click.isShiftClick() || click == ClickType.DOUBLE_CLICK || click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
            return;
        }

        long now = System.currentTimeMillis();
        long last = clickDebounce.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 250) {
            return;
        }
        clickDebounce.put(player.getUniqueId(), now);

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
            if (isTrans) {
                ((TransformationMenu) event.getInventory().getHolder()).handleClick(event, player);
            } else {
                ((AbilityConfigMenu) event.getInventory().getHolder()).handleClick(event, player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TransformationMenu || event.getInventory().getHolder() instanceof AbilityConfigMenu) {
            event.setCancelled(true);
        }
    }
}

package com.drakescraft.rankup.listener;

import com.drakescraft.rankup.DrakesRankupPlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class RankDecayListener implements Listener {

    private final DrakesRankupPlugin plugin;

    public RankDecayListener(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 1. Revisar si su rango expiró mientras estaba desconectado
        plugin.getRankManager().checkDecay(player.getUniqueId(), true);

        // 2. Asegurar grupo de LuckPerms si tiene rango activo
        int tier = plugin.getRankManager().getPlayerTier(player.getUniqueId());
        if (tier > 0) {
            com.drakescraft.rankup.model.Rank r = plugin.getRankManager().getRankByTier(tier);
            if (r != null) {
                plugin.getRankManager().ensureLuckPermsGroup(player, r);
            }
        }

        // 3. Avisar con gracia si le quedan pocas horas de estabilidad (solo si está en un mundo custom permitido)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && plugin.isWorldAllowed(player.getWorld())) {
                plugin.getRankManager().checkWarning(player);
            }
        }, 40L); // 2 segundos tras conectar
    }

    /** Deja al jugador sin ningun poder de rankup (clasico): transformaciones, escala, efectos, vuelo. */
    private void stripPowers(Player player) {
        if (plugin.getDragonBallListener() != null) plugin.getDragonBallListener().revertTransformations(player);
        if (plugin.getOnePieceListener() != null) plugin.getOnePieceListener().revertTransformations(player);
        if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
            if (!player.hasPermission("drakesrankup.admin") && !plugin.hasExternalFlight(player)) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
        player.setFlySpeed(0.10f);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isWorldAllowed(player.getWorld())) {
            // El jugador ingresó a un mundo deshabilitado (ej. clasico)
            stripPowers(player);
        } else {
            // Si regresa a un mundo custom, recordar estabilidad si está próximo al vencimiento
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && plugin.isWorldAllowed(player.getWorld())) {
                    plugin.getRankManager().checkWarning(player);
                }
            }, 20L);
        }
    }
}

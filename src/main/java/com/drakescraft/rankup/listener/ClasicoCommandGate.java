package com.drakescraft.rankup.listener;

import com.drakescraft.rankup.DrakesRankupPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Set;

/** En mundos donde el rankup esta deshabilitado (Clasico) bloquea los comandos de comodidad que el rango otorga. */
public class ClasicoCommandGate implements Listener {
    private static final Set<String> BLOQUEADOS = Set.of(
            "hat", "ehat", "workbench", "eworkbench", "wb", "ewb", "craft", "ecraft",
            "near", "enear", "nearby", "back", "eback", "return", "ereturn",
            "anvil", "eanvil", "grindstone", "egrindstone", "cartographytable", "loom", "smithingtable", "stonecutter");
    private final DrakesRankupPlugin plugin;

    public ClasicoCommandGate(DrakesRankupPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (plugin.isWorldAllowed(p.getWorld()) || p.hasPermission("drakesrankup.admin")) return;
        String msg = e.getMessage();
        if (msg.length() < 2) return;
        String cmd = msg.substring(1).split(" ", 2)[0].toLowerCase();
        int i = cmd.indexOf(':');
        if (i >= 0) cmd = cmd.substring(i + 1);
        if (BLOQUEADOS.contains(cmd)) {
            e.setCancelled(true);
            p.sendMessage("§c[DrakesCraft] Ese comando no esta disponible en Clasico: aqui se juega vanilla.");
        }
    }
}

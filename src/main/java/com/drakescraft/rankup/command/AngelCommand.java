package com.drakescraft.rankup.command;

import com.drakescraft.rankup.DrakesRankupPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AngelCommand implements CommandExecutor {

    private final DrakesRankupPlugin plugin;

    public AngelCommand(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly in-game players can use Angel Mode.");
            return true;
        }

        if (!player.hasPermission("drakesrankup.staff") && !player.hasPermission("drakesrankup.admin")) {
            player.sendMessage("§cYou are not authorized to activate Angel Mode.");
            return true;
        }

        if (!plugin.isWorldAllowed(player.getWorld())) {
            player.sendMessage(plugin.getWorldBlockedMessage());
            return true;
        }

        plugin.getStaffManager().toggleAngel(player);
        return true;
    }
}

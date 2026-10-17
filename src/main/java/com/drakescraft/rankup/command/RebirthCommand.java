package com.drakescraft.rankup.command;

import com.drakescraft.rankup.DrakesRankupPlugin;
import com.drakescraft.rankup.model.PlayerSettings;
import com.drakescraft.rankup.model.Rank;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RebirthCommand implements CommandExecutor, TabCompleter {

    private final DrakesRankupPlugin plugin;

    public RebirthCommand(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando solo puede ser ejecutado por un jugador.");
            return true;
        }

        if (!plugin.isWorldAllowed(player.getWorld())) {
            player.sendMessage(plugin.getWorldBlockedMessage());
            return true;
        }

        PlayerSettings settings = plugin.getRankManager().getPlayerSettings(player.getUniqueId());
        int currentRebirth = settings.getRebirthCount();
        Rank currentRank = plugin.getRankManager().getPlayerRank(player.getUniqueId());
        int currentTier = currentRank != null ? currentRank.getTier() : 0;
        int maxTier = 100;

        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            plugin.getRankManager().processRebirth(player);
            return true;
        }

        // Mostrar menú informativo de Rebirth
        player.sendMessage("§8§m--------------------------------------------------");
        player.sendMessage("  §6§lREBIRTH SYSTEM · DRAKESRANKUP");
        player.sendMessage("§8§m--------------------------------------------------");
        player.sendMessage("§7Current Rebirth level: §e" + currentRebirth + " §8/ §650");
        player.sendMessage("§7Your current rank: " + (currentRank != null ? currentRank.getDisplayName() : "§7No rank") + " §8(Tier " + currentTier + "/" + maxTier + ")");
        player.sendMessage("");
        player.sendMessage("§e§nPermanent Rebirth benefits:§r");
        player.sendMessage(" §8▪ §fPhysical and magic damage: §a+" + (currentRebirth * 3) + "% §7(+3% per Rebirth)");
        player.sendMessage(" §8▪ §fCooldown reduction: §b-" + Math.min(50, currentRebirth) + "% §7(-1% CDR per Rebirth)");
        player.sendMessage(" §8▪ §fVisual badge in chat, TAB, and exclusive celestial auras.");
        player.sendMessage("");

        if (currentRebirth >= 50) {
            player.sendMessage("§6§lYOU HAVE REACHED THE GLORY OF 50 REBIRTHS!");
        } else if (currentTier < maxTier) {
            player.sendMessage("§c✖ Requirements not met: reach §6Tier " + maxTier + " §cto be reborn.");
            player.sendMessage("§7Rebirth resets your rank progress to §aTier 1 (Senku)§7, but your");
            player.sendMessage("§7damage multipliers and Rebirth auras are kept forever.");
        } else {
            player.sendMessage("§a✔ YOU MEET ALL REQUIREMENTS FOR REBIRTH!");
            player.sendMessage("§eType §6/rebirth confirm §eto perform the supreme rebirth.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
        }
        player.sendMessage("§8§m--------------------------------------------------");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> sub = new ArrayList<>();
            if ("confirm".startsWith(args[0].toLowerCase())) sub.add("confirm");
            return sub;
        }
        return Collections.emptyList();
    }
}

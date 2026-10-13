package com.drakescraft.rankup.skills;

import com.drakescraft.rankup.DrakesRankupPlugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Une DrakesRankup con AuraSkills: el rango y los renacimientos suben el XP de skills.
 *
 * Usa los multiplicadores por permiso de AuraSkills ({@code auraskills.multiplier.<porcentaje>})
 * con un PermissionAttachment transitorio, igual que el booster de VIP++: no toca LuckPerms ni
 * persiste, y no depende de la API interna de AuraSkills (si no esta instalado, nadie lee el
 * permiso). Los bonos de Rankup y de VIP++ se suman en AuraSkills.
 */
public final class SkillBoostService implements Listener {

    private static final String PERM_PREFIX = "auraskills.multiplier.";
    private static final long REFRESH_TICKS = 20L * 30L;

    private final DrakesRankupPlugin plugin;
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> current = new ConcurrentHashMap<>();
    private boolean enabled;
    private double perTier;
    private double perRebirth;
    private double capPercent;

    public SkillBoostService(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        FileConfiguration cfg = plugin.getConfig();
        enabled = cfg.getBoolean("skills-boost.enabled", true);
        perTier = cfg.getDouble("skills-boost.percent-per-tier", 0.3);
        perRebirth = cfg.getDouble("skills-boost.percent-per-rebirth", 0.5);
        capPercent = cfg.getDouble("skills-boost.cap-percent", 60.0);
        if (!enabled) {
            plugin.getLogger().info("Bono de XP de skills desactivado en config.yml.");
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("AuraSkills") == null) {
            plugin.getLogger().info("AuraSkills no detectado: el bono de skills queda latente.");
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    refresh(p);
                }
            }
        }.runTaskTimer(plugin, 100L, REFRESH_TICKS);
    }

    /** Porcentaje de bono que corresponde ahora al jugador. */
    public int percentFor(Player player) {
        if (!enabled) {
            return 0;
        }
        UUID id = player.getUniqueId();
        return SkillBoost.percent(plugin.getRankManager().getPlayerTier(id),
                plugin.getRankManager().getRebirthCount(id), perTier, perRebirth, capPercent);
    }

    public void refresh(Player player) {
        int percent = percentFor(player);
        UUID id = player.getUniqueId();
        Integer previous = current.get(id);
        if (previous != null && previous == percent) {
            return;
        }
        removeAttachment(player);
        if (percent > 0) {
            PermissionAttachment attachment = player.addAttachment(plugin);
            attachment.setPermission(PERM_PREFIX + percent, true);
            attachments.put(id, attachment);
        }
        current.put(id, percent);
        player.recalculatePermissions();
    }

    private void removeAttachment(Player player) {
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            try {
                player.removeAttachment(attachment);
            } catch (IllegalArgumentException ignored) {
                // El attachment ya no existe tras un relog; nada que limpiar.
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                refresh(event.getPlayer());
            }
        }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeAttachment(event.getPlayer());
        current.remove(event.getPlayer().getUniqueId());
    }
}

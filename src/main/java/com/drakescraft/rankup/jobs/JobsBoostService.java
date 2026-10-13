package com.drakescraft.rankup.jobs;

import com.drakescraft.rankup.DrakesRankupPlugin;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Potencia el dinero que paga Jobs Reborn segun el rango, los renacimientos y el VIP++.
 *
 * Se engancha a {@code JobsPrePaymentEvent} por reflexion: Rankup no depende de Jobs ni de
 * VIP++ al compilar, y si alguno no esta instalado el bono simplemente no aplica.
 */
public final class JobsBoostService implements Listener {

    private static final String JOBS_EVENT = "com.gamingmesh.jobs.api.JobsPrePaymentEvent";

    private final DrakesRankupPlugin plugin;
    private boolean enabled;
    private double perTier;
    private double perRebirth;
    private double cap;
    private boolean includeVip;
    private boolean boostPoints;

    private Method getPlayer;
    private Method getAmount;
    private Method setAmount;
    private Method getPoints;
    private Method setPoints;
    private Method vipJobsMultiplier;

    public JobsBoostService(DrakesRankupPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(FileConfiguration cfg) {
        enabled = cfg.getBoolean("jobs-boost.enabled", true);
        perTier = cfg.getDouble("jobs-boost.money-per-tier", 0.005);
        perRebirth = cfg.getDouble("jobs-boost.money-per-rebirth", 0.01);
        cap = cfg.getDouble("jobs-boost.cap", 3.0);
        includeVip = cfg.getBoolean("jobs-boost.include-vip", true);
        boostPoints = cfg.getBoolean("jobs-boost.boost-points", false);
    }

    /** Registra el listener si Jobs esta presente. */
    public void register() {
        load(plugin.getConfig());
        if (!enabled) {
            plugin.getLogger().info("Bono de Jobs desactivado en config.yml.");
            return;
        }
        Plugin jobs = Bukkit.getPluginManager().getPlugin("Jobs");
        if (jobs == null) {
            plugin.getLogger().info("Jobs no detectado: el bono de dinero queda latente.");
            return;
        }
        try {
            Class<?> eventClass = Class.forName(JOBS_EVENT, true, jobs.getClass().getClassLoader());
            getPlayer = eventClass.getMethod("getPlayer");
            getAmount = eventClass.getMethod("getAmount");
            setAmount = eventClass.getMethod("setAmount", double.class);
            getPoints = eventClass.getMethod("getPoints");
            setPoints = eventClass.getMethod("setPoints", double.class);
            @SuppressWarnings("unchecked")
            Class<? extends Event> typed = (Class<? extends Event>) eventClass;
            Bukkit.getPluginManager().registerEvent(typed, this, EventPriority.HIGH,
                    (listener, event) -> {
                        if (typed.isInstance(event)) {
                            apply(event);
                        }
                    }, plugin, true);
            plugin.getLogger().info("Bono de dinero de Jobs activo (x"
                    + cap + " como maximo).");
        } catch (ReflectiveOperationException | LinkageError ex) {
            plugin.getLogger().warning("No se pudo enganchar Jobs (" + ex.getClass().getSimpleName()
                    + "): " + ex.getMessage());
        }
    }

    private void apply(Event event) {
        try {
            Object who = getPlayer.invoke(event);
            if (!(who instanceof OfflinePlayer offline)) {
                return;
            }
            Player player = offline.getPlayer();
            if (player == null) {
                return;
            }
            double multiplier = multiplierFor(player);
            if (multiplier <= 1.0) {
                return;
            }
            double amount = (double) getAmount.invoke(event);
            if (amount > 0.0) {
                setAmount.invoke(event, amount * multiplier);
            }
            if (boostPoints) {
                double points = (double) getPoints.invoke(event);
                if (points > 0.0) {
                    setPoints.invoke(event, points * multiplier);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            plugin.getLogger().warning("Bono de Jobs omitido: " + ex.getMessage());
        }
    }

    /** Multiplicador actual del jugador; tambien lo usa el placeholder. */
    public double multiplierFor(Player player) {
        if (!enabled) {
            return 1.0;
        }
        int tier = plugin.getRankManager().getPlayerTier(player.getUniqueId());
        int rebirths = plugin.getRankManager().getRebirthCount(player.getUniqueId());
        return JobsBoost.multiplier(tier, rebirths, perTier, perRebirth, vipMultiplier(player), cap);
    }

    private double vipMultiplier(Player player) {
        if (!includeVip) {
            return 1.0;
        }
        try {
            if (vipJobsMultiplier == null) {
                Plugin vip = Bukkit.getPluginManager().getPlugin("DrakesVIPPlusPlus");
                if (vip == null) {
                    return 1.0;
                }
                Class<?> api = Class.forName("com.drakescraft.vip.api.DrakesVipApi", true,
                        vip.getClass().getClassLoader());
                vipJobsMultiplier = api.getMethod("getJobsMoneyMultiplier", Player.class);
            }
            Object value = vipJobsMultiplier.invoke(null, player);
            return value instanceof Double d ? d : 1.0;
        } catch (ReflectiveOperationException | LinkageError ex) {
            return 1.0;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}

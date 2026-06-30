package com.simpleplots.util;

import com.simpleplots.SimplePlots;
import com.simpleplots.api.Plot;
import com.simpleplots.api.PlotId;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Periodically checks for inactive players and auto-unclaims their plot clusters.
 */
public class InactivityWatchdog extends BukkitRunnable {
    private final SimplePlots plugin;

    public InactivityWatchdog(SimplePlots plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("inactivity-expiration.enabled", true)) {
            return;
        }

        int expiryDays = plugin.getConfig().getInt("inactivity-expiration.expiry-days", 30);
        long expiryMillis = TimeUnit.DAYS.toMillis(expiryDays);
        long now = System.currentTimeMillis();

        // Retrieve all unique plot owners
        Set<UUID> owners = new HashSet<>();
        for (Plot plot : plugin.getPlotAPI().getAllPlots()) {
            if (plot.isClaimed()) {
                owners.add(plot.getOwner());
            }
        }

        for (UUID ownerUuid : owners) {
            plugin.getDatabaseManager().getPlayerLastSeen(ownerUuid).thenAccept(lastSeen -> {
                long lastActive = lastSeen;
                if (lastActive <= 0) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ownerUuid);
                    lastActive = offlinePlayer.getLastPlayed();
                    if (lastActive <= 0) {
                        // Update to current time to prevent instant expiry
                        plugin.getDatabaseManager().updatePlayerLastSeen(ownerUuid, now);
                        return;
                    }
                }

                long inactiveDuration = now - lastActive;
                if (inactiveDuration >= expiryMillis) {
                    // Find all plots owned by this player
                    List<Plot> ownedPlots = new ArrayList<>();
                    for (Plot plot : plugin.getPlotAPI().getAllPlots()) {
                        if (plot.isClaimed() && plot.getOwner().equals(ownerUuid)) {
                            ownedPlots.add(plot);
                        }
                    }

                    // Delete them cluster-by-cluster
                    Set<PlotId> processed = new HashSet<>();
                    for (Plot plot : ownedPlots) {
                        if (processed.contains(plot.getId())) continue;

                        Set<PlotId> cluster = plugin.getPlotAPI().getMergedCluster(plot.getWorld(), plot.getId());
                        processed.addAll(cluster);

                        plugin.getPlotAPI().unclaimPlotCluster(plot.getWorld(), plot.getId()).thenRun(() -> {
                            plugin.getLogger().info("Automatically unclaimed plot cluster at " + plot.getWorld() + 
                                                    " starting at " + plot.getId() + " due to inactivity of player " + ownerUuid);
                        });
                    }
                }
            });
        }
    }
}

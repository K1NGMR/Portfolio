package com.simpleplots.placeholder;

import com.simpleplots.SimplePlots;
import com.simpleplots.api.Plot;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * PlaceholderAPI Expansion for SimplePlots placeholders.
 */
public class PlotPlaceholderExpansion extends PlaceholderExpansion {
    private final SimplePlots plugin;

    public PlotPlaceholderExpansion(SimplePlots plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "plot";
    }

    @Override
    public @NotNull String getAuthor() {
        return "K1NGMR";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // Register expansion even after PAPI reload
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return "";
        }
        Player player = offlinePlayer.getPlayer();
        if (player == null) return "";

        String lowerParam = params.toLowerCase();

        // Global / Player-specific placeholders
        if (lowerParam.equals("allowed_plot_count") || lowerParam.equals("limit") || lowerParam.equals("plots_limit")) {
            if (player.hasPermission("plots.admin") || player.hasPermission("plots.plot.infinite")) {
                return "infinite";
            }
            int max = plugin.getConfig().getInt("max-plots-per-player", 3);
            for (int i = 100; i >= 1; i--) {
                if (player.hasPermission("plots.plot." + i)) {
                    max = i;
                    break;
                }
            }
            return String.valueOf(max);
        }

        if (lowerParam.equals("owned") || lowerParam.equals("plots_owned")) {
            // Count distinct clusters owned by the player
            java.util.Set<com.simpleplots.api.PlotId> owned = new java.util.HashSet<>();
            String worldName = null;
            for (Plot plot : plugin.getPlotAPI().getAllPlots()) {
                if (plot.getOwner() != null && plot.getOwner().equals(player.getUniqueId())) {
                    owned.add(plot.getId());
                    if (worldName == null) worldName = plot.getWorld();
                }
            }
            if (owned.isEmpty()) return "0";

            int clusterCount = 0;
            java.util.Set<com.simpleplots.api.PlotId> visited = new java.util.HashSet<>();
            for (com.simpleplots.api.PlotId pid : owned) {
                if (!visited.contains(pid)) {
                    clusterCount++;
                    java.util.Queue<com.simpleplots.api.PlotId> queue = new java.util.LinkedList<>();
                    queue.add(pid);
                    visited.add(pid);
                    while (!queue.isEmpty()) {
                        com.simpleplots.api.PlotId current = queue.poll();
                        Plot currentPlot = plugin.getPlotAPI().getPlot(worldName, current);
                        if (currentPlot == null) continue;

                        if (currentPlot.isMergedE()) {
                            com.simpleplots.api.PlotId next = new com.simpleplots.api.PlotId(current.getX() + 1, current.getZ());
                            if (owned.contains(next) && !visited.contains(next)) {
                                visited.add(next);
                                queue.add(next);
                            }
                        }
                        if (currentPlot.isMergedW()) {
                            com.simpleplots.api.PlotId next = new com.simpleplots.api.PlotId(current.getX() - 1, current.getZ());
                            if (owned.contains(next) && !visited.contains(next)) {
                                visited.add(next);
                                queue.add(next);
                            }
                        }
                        if (currentPlot.isMergedN()) {
                            com.simpleplots.api.PlotId next = new com.simpleplots.api.PlotId(current.getX(), current.getZ() - 1);
                            if (owned.contains(next) && !visited.contains(next)) {
                                visited.add(next);
                                queue.add(next);
                            }
                        }
                        if (currentPlot.isMergedS()) {
                            com.simpleplots.api.PlotId next = new com.simpleplots.api.PlotId(current.getX(), current.getZ() + 1);
                            if (owned.contains(next) && !visited.contains(next)) {
                                visited.add(next);
                                queue.add(next);
                            }
                        }
                    }
                }
            }
            return String.valueOf(clusterCount);
        }

        // Plot-specific placeholders (based on where the player is currently standing)
        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            if (lowerParam.equals("current_id") || lowerParam.equals("plots_current_id") || lowerParam.equals("id")) {
                return "None";
            }
            return "None";
        }

        switch (lowerParam) {
            case "owner":
                return plugin.getUuidCache().getName(plot.getOwner());
            case "id":
            case "current_id":
            case "plots_current_id":
                return plot.getId().toString();
            case "biome":
                return player.getLocation().getBlock().getBiome().name();
            case "flags":
                if (plot.getFlags().isEmpty()) return "None";
                return plot.getFlags().entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(", "));
            case "trusted_count":
                return String.valueOf(plot.getTrusted().size());
            case "allowed_count":
                return String.valueOf(plot.getAdded().size());
            case "rating":
            case "average_rating":
            case "rating_average":
                return String.format("%.2f", plot.getAverageRating());
            case "ratings_count":
                return String.valueOf(plot.getRatings().size());
            case "denied_count":
                return String.valueOf(plot.getDenied().size());
            case "is_claimed":
                return String.valueOf(plot.isClaimed());
            case "is_merged":
                return String.valueOf(plot.isMergedN() || plot.isMergedS() || plot.isMergedE() || plot.isMergedW());
            case "merged_directions":
                java.util.List<String> dirs = new java.util.ArrayList<>();
                if (plot.isMergedN()) dirs.add("N");
                if (plot.isMergedS()) dirs.add("S");
                if (plot.isMergedE()) dirs.add("E");
                if (plot.isMergedW()) dirs.add("W");
                if (dirs.isEmpty()) return "None";
                return String.join(", ", dirs);
            case "members_count":
                return String.valueOf(plot.getTrusted().size() + plot.getAdded().size());
            default:
                return null;
        }
    }
}

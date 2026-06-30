package com.simpleplots;

import com.simpleplots.api.Plot;
import com.simpleplots.api.PlotGeometry;
import com.simpleplots.api.PlotId;
import com.simpleplots.api.events.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public API class for SimplePlots.
 */
public class PlotAPI {
    private final SimplePlots plugin;
    private final Map<String, Map<PlotId, Plot>> plotCache = new ConcurrentHashMap<>();
    private final Map<String, String> customFlags = new ConcurrentHashMap<>();

    public PlotAPI(SimplePlots plugin) {
        this.plugin = plugin;
    }

    /**
     * Retrieves the plot at the given Location.
     * Returns null if no plot is claimed at this location.
     */
    public Plot getPlotAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        if (PlotGeometry.isRoad(loc)) return null;
        PlotId id = PlotGeometry.getPlotId(loc);
        if (id == null) return null;
        Plot plot = getPlot(loc.getWorld().getName(), id);
        if (plot != null && !plot.isClaimed()) {
            return null;
        }
        return plot;
    }

    /**
     * Gets a plot by its world and PlotId.
     */
    public Plot getPlot(String world, PlotId id) {
        if (world == null || id == null) return null;
        Map<PlotId, Plot> worldPlots = plotCache.get(world.toLowerCase());
        if (worldPlots == null) return null;
        return worldPlots.get(id);
    }

    /**
     * Creates and caches a plot in memory. Should only be used by loading routines or claiming.
     */
    public void addPlotToCache(Plot plot) {
        plotCache.computeIfAbsent(plot.getWorld().toLowerCase(), k -> new ConcurrentHashMap<>())
                .put(plot.getId(), plot);
    }

    /**
     * Removes a plot from the cache.
     */
    public void removePlotFromCache(String world, PlotId id) {
        Map<PlotId, Plot> worldPlots = plotCache.get(world.toLowerCase());
        if (worldPlots != null) {
            worldPlots.remove(id);
        }
    }

    /**
     * Gets all claimed plots for a player.
     */
    public Collection<Plot> getPlayerPlots(UUID uuid) {
        List<Plot> playerPlots = new ArrayList<>();
        for (Map<PlotId, Plot> worldPlots : plotCache.values()) {
            for (Plot plot : worldPlots.values()) {
                if (plot.getOwner() != null && plot.getOwner().equals(uuid)) {
                    playerPlots.add(plot);
                }
            }
        }
        return playerPlots;
    }

    public Collection<Plot> getAllPlots() {
        List<Plot> allPlots = new ArrayList<>();
        for (Map<PlotId, Plot> worldPlots : plotCache.values()) {
            for (Plot plot : worldPlots.values()) {
                if (plot.isClaimed()) {
                    allPlots.add(plot);
                }
            }
        }
        return allPlots;
    }

    /**
     * Registers a custom flag.
     */
    public void registerCustomFlag(String flagName, String defaultValue) {
        customFlags.put(flagName.toLowerCase(), defaultValue);
    }

    /**
     * Checks if a flag is registered.
     */
    public boolean isFlagRegistered(String flagName) {
        return customFlags.containsKey(flagName.toLowerCase());
    }

    /**
     * Gets all registered flags.
     */
    public Map<String, String> getRegisteredFlags() {
        return Collections.unmodifiableMap(customFlags);
    }

    public SimplePlots getPlugin() {
        return plugin;
    }

    public void clearCache() {
        plotCache.clear();
    }

    public Set<PlotId> getMergedCluster(String worldName, PlotId startId) {
        Set<PlotId> cluster = new HashSet<>();
        Queue<PlotId> queue = new LinkedList<>();
        queue.add(startId);
        cluster.add(startId);

        while (!queue.isEmpty()) {
            PlotId current = queue.poll();
            Plot currentPlot = getPlot(worldName, current);
            if (currentPlot == null) continue;

            if (currentPlot.isMergedE()) {
                PlotId next = new PlotId(current.getX() + 1, current.getZ());
                if (!cluster.contains(next)) {
                    cluster.add(next);
                    queue.add(next);
                }
            }
            if (currentPlot.isMergedW()) {
                PlotId next = new PlotId(current.getX() - 1, current.getZ());
                if (!cluster.contains(next)) {
                    cluster.add(next);
                    queue.add(next);
                }
            }
            if (currentPlot.isMergedN()) {
                PlotId next = new PlotId(current.getX(), current.getZ() - 1);
                if (!cluster.contains(next)) {
                    cluster.add(next);
                    queue.add(next);
                }
            }
            if (currentPlot.isMergedS()) {
                PlotId next = new PlotId(current.getX(), current.getZ() + 1);
                if (!cluster.contains(next)) {
                    cluster.add(next);
                    queue.add(next);
                }
            }
        }
        return cluster;
    }

    public java.util.concurrent.CompletableFuture<Void> unclaimPlotCluster(String worldName, PlotId startId) {
        Plot plot = getPlot(worldName, startId);
        if (plot == null) return java.util.concurrent.CompletableFuture.completedFuture(null);

        Set<PlotId> cluster = getMergedCluster(worldName, startId);
        UUID nilUuid = new UUID(0, 0);

        java.util.concurrent.CompletableFuture<Void> dbFuture = java.util.concurrent.CompletableFuture.completedFuture(null);
        for (PlotId pid : cluster) {
            Plot p = getPlot(worldName, pid);
            if (p != null) {
                p.setOwner(nilUuid);
                p.getAdded().clear();
                p.getTrusted().clear();
                p.getDenied().clear();
                p.getFlags().clear();

                final Plot finalP = p;
                dbFuture = dbFuture.thenCompose(v -> plugin.getDatabaseManager().savePlotOwner(finalP));
            }
        }

        return dbFuture.thenAcceptAsync(v -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                com.simpleplots.generator.PlotWorldConfig cfg = plugin.getWorldConfig(worldName);
                org.bukkit.World world = Bukkit.getWorld(worldName);
                if (world != null && cfg != null) {
                    for (PlotId pid : cluster) {
                        clearPlotBlocks(world, cfg, pid);
                    }

                    // Clear the roads between adjacent plots in the cluster
                    for (PlotId p1 : cluster) {
                        Plot plot1 = getPlot(worldName, p1);
                        if (plot1 == null) continue;
                        if (plot1.isMergedE()) {
                            PlotId p2 = new PlotId(p1.getX() + 1, p1.getZ());
                            if (cluster.contains(p2)) {
                                clearRoadBlocksBetween(world, cfg, p1, p2, "EAST");
                            }
                        }
                        if (plot1.isMergedS()) {
                            PlotId p2 = new PlotId(p1.getX(), p1.getZ() + 1);
                            if (cluster.contains(p2)) {
                                clearRoadBlocksBetween(world, cfg, p1, p2, "SOUTH");
                            }
                        }
                    }

                    // Update borders
                    for (PlotId pid : cluster) {
                        PlotGeometry.updatePlotBorders(worldName, pid);
                    }
                }
            });
        });
    }

    private void clearPlotBlocks(org.bukkit.World world, com.simpleplots.generator.PlotWorldConfig cfg, PlotId plotId) {
        int[] bounds = PlotGeometry.getPlotBounds(world.getName(), plotId);
        int minX = bounds[0];
        int minZ = bounds[1];
        int maxX = bounds[2];
        int maxZ = bounds[3];

        org.bukkit.Material floorMat = cfg.getFloorBlock();
        org.bukkit.Material fillerMat = cfg.getFillerBlock();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setType(x, 0, z, cfg.getBedBlock());
                for (int y = 1; y < cfg.getFloorHeight(); y++) {
                    world.setType(x, y, z, fillerMat);
                }
                world.setType(x, cfg.getFloorHeight(), z, floorMat);
                for (int y = cfg.getFloorHeight() + 1; y < world.getMaxHeight(); y++) {
                    world.setType(x, y, z, org.bukkit.Material.AIR);
                }
            }
        }
    }

    private void clearRoadBlocksBetween(org.bukkit.World world, com.simpleplots.generator.PlotWorldConfig cfg, PlotId currentId, PlotId adjacentId, String dir) {
        int[] currentBounds = PlotGeometry.getPlotBounds(world.getName(), currentId);
        int[] adjacentBounds = PlotGeometry.getPlotBounds(world.getName(), adjacentId);

        int minX, minZ, maxX, maxZ;
        if (dir.equals("EAST") || dir.equals("WEST")) {
            minX = Math.min(currentBounds[2], adjacentBounds[2]) + 1;
            maxX = Math.max(currentBounds[0], adjacentBounds[0]) - 1;
            minZ = Math.min(currentBounds[1], adjacentBounds[1]);
            maxZ = Math.max(currentBounds[3], adjacentBounds[3]);
        } else {
            minX = Math.min(currentBounds[0], adjacentBounds[0]);
            maxX = Math.max(currentBounds[2], adjacentBounds[2]);
            minZ = Math.min(currentBounds[3], adjacentBounds[3]) + 1;
            maxZ = Math.max(currentBounds[1], adjacentBounds[1]) - 1;
        }

        org.bukkit.Material floorMat = cfg.getFloorBlock();
        org.bukkit.Material fillerMat = cfg.getFillerBlock();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setType(x, 0, z, cfg.getBedBlock());
                for (int y = 1; y < cfg.getFloorHeight(); y++) {
                    world.setType(x, y, z, fillerMat);
                }
                world.setType(x, cfg.getFloorHeight(), z, floorMat);
                for (int y = cfg.getFloorHeight() + 1; y < world.getMaxHeight(); y++) {
                    world.setType(x, y, z, org.bukkit.Material.AIR);
                }
            }
        }
    }
}

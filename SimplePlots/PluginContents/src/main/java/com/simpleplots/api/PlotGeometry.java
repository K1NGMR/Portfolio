package com.simpleplots.api;

import com.simpleplots.SimplePlots;
import com.simpleplots.PlotAPI;
import com.simpleplots.generator.PlotWorldConfig;
import org.bukkit.Location;

import java.util.Set;
import java.util.HashSet;
import java.util.Queue;
import java.util.LinkedList;
import java.util.List;

/**
 * Core mathematical engine for plot grids, conversions, and geometry calculations.
 */
public class PlotGeometry {

    /**
     * Translates a Bukkit location into a PlotId.
     * Returns null if the world is not a plot world.
     */
    public static PlotId getPlotId(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return getPlotId(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ());
    }

    /**
     * Translates block coordinates in a world into a PlotId.
     * Returns null if the world is not a plot world.
     */
    public static PlotId getPlotId(String worldName, int blockX, int blockZ) {
        PlotWorldConfig config = SimplePlots.getInstance().getWorldConfig(worldName);
        if (config == null) return null;

        int totalSize = config.getTotalSize();
        int roadWidth = config.getRoadWidth();
        int halfRoad = roadWidth / 2;

        int shiftedX = blockX + halfRoad;
        int shiftedZ = blockZ + halfRoad;

        int plotX = Math.floorDiv(shiftedX, totalSize);
        int plotZ = Math.floorDiv(shiftedZ, totalSize);

        return new PlotId(plotX, plotZ);
    }

    /**
     * Checks if a location is on a road block (taking merges into account).
     */
    public static boolean isRoad(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        return isRoad(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ());
    }

    /**
     * Checks if block coordinates in a world represent a road block (taking merges into account).
     */
    public static boolean isRoad(String worldName, int blockX, int blockZ) {
        PlotWorldConfig config = SimplePlots.getInstance().getWorldConfig(worldName);
        if (config == null) return false;

        int totalSize = config.getTotalSize();
        int roadWidth = config.getRoadWidth();
        int halfRoad = roadWidth / 2;

        int shiftedX = blockX + halfRoad;
        int shiftedZ = blockZ + halfRoad;

        int plotX = Math.floorDiv(shiftedX, totalSize);
        int plotZ = Math.floorDiv(shiftedZ, totalSize);

        int remX = Math.floorMod(shiftedX, totalSize);
        int remZ = Math.floorMod(shiftedZ, totalSize);

        boolean mathPlot = (remX >= roadWidth) && (remZ >= roadWidth);
        if (mathPlot) {
            // It is mathematically inside the plot cell, so it's not a road
            return false;
        }

        // If we reach here, it's mathematically on a road or intersection corner.
        // Let's check for plot merges.
        PlotAPI api = SimplePlots.getInstance().getPlotAPI();
        Plot plot = api.getPlot(worldName, new PlotId(plotX, plotZ));

        // Case 1: Vertical road (remX < roadWidth and remZ >= roadWidth)
        // This road separates Plot(plotX - 1, plotZ) and Plot(plotX, plotZ)
        if (remX < roadWidth && remZ >= roadWidth) {
            Plot westPlot = api.getPlot(worldName, new PlotId(plotX - 1, plotZ));
            if (westPlot != null && westPlot.isMergedE()) {
                return false; // Merged East-West, so road is removed
            }
            return true;
        }

        // Case 2: Horizontal road (remX >= roadWidth and remZ < roadWidth)
        // This road separates Plot(plotX, plotZ - 1) and Plot(plotX, plotZ)
        if (remX >= roadWidth && remZ < roadWidth) {
            Plot northPlot = api.getPlot(worldName, new PlotId(plotX, plotZ - 1));
            if (northPlot != null && northPlot.isMergedS()) {
                return false; // Merged North-South, so road is removed
            }
            return true;
        }

        // Case 3: Intersection corner (remX < roadWidth and remZ < roadWidth)
        // Surrounded by:
        // (plotX - 1, plotZ - 1) [NW], (plotX, plotZ - 1) [NE]
        // (plotX - 1, plotZ)     [SW], (plotX, plotZ)     [SE]
        if (remX < roadWidth && remZ < roadWidth) {
            Plot nw = api.getPlot(worldName, new PlotId(plotX - 1, plotZ - 1));
            Plot ne = api.getPlot(worldName, new PlotId(plotX, plotZ - 1));
            Plot sw = api.getPlot(worldName, new PlotId(plotX - 1, plotZ));
            Plot se = api.getPlot(worldName, new PlotId(plotX, plotZ));

            if (nw != null && ne != null && sw != null && se != null) {
                // If all four surrounders are merged together, the intersection corner is part of the plot
                if (nw.isMergedE() && nw.isMergedS() && ne.isMergedS() && sw.isMergedE()) {
                    return false;
                }
            }
            return true;
        }

        return true;
    }

    /**
     * Checks if a location is inside a plot cell (including merged roads).
     */
    public static boolean isPlotCell(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        return !isRoad(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ());
    }

    /**
     * Returns the bounding box [minX, minZ, maxX, maxZ] of a plot cell.
     */
    public static int[] getPlotBounds(String worldName, PlotId plotId) {
        PlotWorldConfig config = SimplePlots.getInstance().getWorldConfig(worldName);
        if (config == null) return null;

        int totalSize = config.getTotalSize();
        int roadWidth = config.getRoadWidth();
        int halfRoad = roadWidth / 2;

        int minX = plotId.getX() * totalSize + roadWidth - halfRoad;
        int minZ = plotId.getZ() * totalSize + roadWidth - halfRoad;
        int maxX = plotId.getX() * totalSize + totalSize - 1 - halfRoad;
        int maxZ = plotId.getZ() * totalSize + totalSize - 1 - halfRoad;

        return new int[]{minX, minZ, maxX, maxZ};
    }
    
    /**
     * Calculates the full merged bounding box containing all connected plots for a given plot.
     * Retruns [minX, minZ, maxX, maxZ] of the combined plots and their merged roads.
     */
    public static int[] getMergedPlotBounds(String worldName, PlotId plotId) {
        PlotAPI api = SimplePlots.getInstance().getPlotAPI();
        Plot plot = api.getPlot(worldName, plotId);
        if (plot == null) {
            return getPlotBounds(worldName, plotId);
        }

        // Flood fill to find all connected merged plot IDs
        Set<PlotId> visited = new HashSet<>();
        Queue<PlotId> queue = new LinkedList<>();
        queue.add(plotId);
        visited.add(plotId);

        while (!queue.isEmpty()) {
            PlotId current = queue.poll();
            Plot currentPlot = api.getPlot(worldName, current);
            if (currentPlot == null) continue;

            // Check East
            if (currentPlot.isMergedE()) {
                PlotId east = new PlotId(current.getX() + 1, current.getZ());
                if (!visited.contains(east)) {
                    visited.add(east);
                    queue.add(east);
                }
            }
            // Check West
            if (currentPlot.isMergedW()) {
                PlotId west = new PlotId(current.getX() - 1, current.getZ());
                if (!visited.contains(west)) {
                    visited.add(west);
                    queue.add(west);
                }
            }
            // Check South
            if (currentPlot.isMergedS()) {
                PlotId south = new PlotId(current.getX(), current.getZ() + 1);
                if (!visited.contains(south)) {
                    visited.add(south);
                    queue.add(south);
                }
            }
            // Check North
            if (currentPlot.isMergedN()) {
                PlotId north = new PlotId(current.getX(), current.getZ() - 1);
                if (!visited.contains(north)) {
                    visited.add(north);
                    queue.add(north);
                }
            }
        }

        // Calculate cumulative bounds
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (PlotId pid : visited) {
            int[] bounds = getPlotBounds(worldName, pid);
            if (bounds != null) {
                if (bounds[0] < minX) minX = bounds[0];
                if (bounds[1] < minZ) minZ = bounds[1];
                if (bounds[2] > maxX) maxX = bounds[2];
                if (bounds[3] > maxZ) maxZ = bounds[3];
            }
        }

        return new int[]{minX, minZ, maxX, maxZ};
    }

    /**
     * Recalculates and refreshes the border wall blocks for the given plot (and its merged neighbors) in the world.
     */
    public static void updatePlotBorders(String worldName, PlotId plotId) {
        SimplePlots plugin = SimplePlots.getInstance();
        PlotWorldConfig cfg = plugin.getWorldConfig(worldName);
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
        if (cfg == null || world == null) return;

        int totalSize = cfg.getTotalSize();
        int roadWidth = cfg.getRoadWidth();
        int halfRoad = roadWidth / 2;
        int floorHeight = cfg.getFloorHeight();

        org.bukkit.Material wallClaimed = cfg.getWallBlockClaimed();
        org.bukkit.Material wallUnclaimed = cfg.getWallBlockUnclaimed();

        // Bounds of the plot cell to refresh borders around (covering a radius of roadWidth around it)
        int[] bounds = getPlotBounds(worldName, plotId);
        int minX = bounds[0] - roadWidth;
        int maxX = bounds[2] + roadWidth;
        int minZ = bounds[1] - roadWidth;
        int maxZ = bounds[3] + roadWidth;

        PlotAPI api = plugin.getPlotAPI();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int shiftedX = x + halfRoad;
                int shiftedZ = z + halfRoad;
                int remX = Math.floorMod(shiftedX, totalSize);
                int remZ = Math.floorMod(shiftedZ, totalSize);

                boolean isBorderCandidate = (remX == roadWidth - 1) || (remX == 0) || (remZ == roadWidth - 1) || (remZ == 0);
                if (!isBorderCandidate) continue;

                // 1. If this coordinate is NOT a road block anymore (it is part of a merged plot)
                if (!isRoad(worldName, x, z)) {
                    // Set it to AIR above floor height (clears the wall)
                    world.setType(x, floorHeight + 1, z, org.bukkit.Material.AIR);
                    continue;
                }

                // 2. If it is a road block, check if it's adjacent to any plot block (isRoad == false)
                boolean isBorder = false;
                boolean anyClaimed = false;

                // Check 8 neighbors
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dz == 0) continue;
                        int nx = x + dx;
                        int nz = z + dz;

                        if (!isRoad(worldName, nx, nz)) {
                            isBorder = true;
                            PlotId pid = getPlotId(worldName, nx, nz);
                            if (pid != null && api.getPlot(worldName, pid) != null) {
                                anyClaimed = true;
                            }
                        }
                    }
                }

                if (isBorder) {
                    world.setType(x, floorHeight + 1, z, anyClaimed ? wallClaimed : wallUnclaimed);
                } else {
                    world.setType(x, floorHeight + 1, z, org.bukkit.Material.AIR);
                }
            }
        }
    }
}

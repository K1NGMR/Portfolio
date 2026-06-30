package com.simpleplots.api;

import java.util.UUID;

/**
 * Represents a group of plots grouped together as a cluster.
 */
public class PlotCluster {
    private final String name;
    private final String world;
    private final PlotId minPlot;
    private final PlotId maxPlot;
    private UUID owner;

    public PlotCluster(String name, String world, PlotId minPlot, PlotId maxPlot, UUID owner) {
        this.name = name;
        this.world = world;
        this.minPlot = minPlot;
        this.maxPlot = maxPlot;
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public String getWorld() {
        return world;
    }

    public PlotId getMinPlot() {
        return minPlot;
    }

    public PlotId getMaxPlot() {
        return maxPlot;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public boolean contains(PlotId plotId) {
        return plotId.getX() >= minPlot.getX() && plotId.getX() <= maxPlot.getX() &&
               plotId.getZ() >= minPlot.getZ() && plotId.getZ() <= maxPlot.getZ();
    }
}

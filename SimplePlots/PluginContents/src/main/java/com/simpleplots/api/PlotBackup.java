package com.simpleplots.api;

import java.util.UUID;

/**
 * Represents a saved plot backup metadata.
 */
public class PlotBackup {
    private final int id;
    private final PlotId plotId;
    private final String world;
    private final long timestamp;
    private final UUID creator;
    private final String filePath;

    public PlotBackup(int id, PlotId plotId, String world, long timestamp, UUID creator, String filePath) {
        this.id = id;
        this.plotId = plotId;
        this.world = world;
        this.timestamp = timestamp;
        this.creator = creator;
        this.filePath = filePath;
    }

    public int getId() {
        return id;
    }

    public PlotId getPlotId() {
        return plotId;
    }

    public String getWorld() {
        return world;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public UUID getCreator() {
        return creator;
    }

    public String getFilePath() {
        return filePath;
    }
}

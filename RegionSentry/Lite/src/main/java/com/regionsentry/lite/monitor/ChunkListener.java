package com.regionsentry.lite.monitor;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public final class ChunkListener implements Listener {
    private final PerformanceMonitor monitor;

    public ChunkListener(PerformanceMonitor monitor) {
        this.monitor = monitor;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        String worldName = event.getWorld().getName();
        int cx = event.getChunk().getX();
        int cz = event.getChunk().getZ();
        ChunkKey key = new ChunkKey(worldName, cx, cz);
        
        // Find the tracker that owns this chunk key
        for (RegionTracker tracker : monitor.getTrackers()) {
            if (tracker.getChunks().contains(key)) {
                tracker.incrementChunksLoaded();
                if (event.isNewChunk()) {
                    tracker.incrementChunksGenerated();
                }
                return;
            }
        }
    }
}

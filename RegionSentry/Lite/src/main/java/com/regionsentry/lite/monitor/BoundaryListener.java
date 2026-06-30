package com.regionsentry.lite.monitor;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

public final class BoundaryListener implements Listener {
    private final PerformanceMonitor monitor;

    public BoundaryListener(PerformanceMonitor monitor) {
        this.monitor = monitor;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        int fromX = event.getFrom().getBlockX() >> 4;
        int fromZ = event.getFrom().getBlockZ() >> 4;
        int toX = event.getTo().getBlockX() >> 4;
        int toZ = event.getTo().getBlockZ() >> 4;
        String fromWorld = event.getFrom().getWorld().getName();
        String toWorld = event.getTo().getWorld().getName();

        if (fromX != toX || fromZ != toZ || !fromWorld.equals(toWorld)) {
            incrementCrossing(toWorld, toX, toZ);
        }
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        int fromX = event.getFrom().getBlockX() >> 4;
        int fromZ = event.getFrom().getBlockZ() >> 4;
        int toX = event.getTo().getBlockX() >> 4;
        int toZ = event.getTo().getBlockZ() >> 4;
        String fromWorld = event.getFrom().getWorld().getName();
        String toWorld = event.getTo().getWorld().getName();

        if (fromX != toX || fromZ != toZ || !fromWorld.equals(toWorld)) {
            incrementCrossing(toWorld, toX, toZ);
        }
    }

    private void incrementCrossing(String world, int x, int z) {
        for (RegionTracker tracker : monitor.getTrackers()) {
            for (ChunkKey key : tracker.getChunks()) {
                if (key.getX() == x && key.getZ() == z && key.getWorldName().equals(world)) {
                    tracker.incrementBoundaryCrossings();
                    return;
                }
            }
        }
    }
}

package com.regionsentry.pro.listener;

import com.regionsentry.lite.monitor.ChunkKey;
import com.regionsentry.lite.monitor.PerformanceMonitor;
import com.regionsentry.lite.monitor.RegionTracker;
import com.regionsentry.pro.RegionSentryPro;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MitigationListener implements Listener {
    public static final Set<String> HALTED_REGIONS = ConcurrentHashMap.newKeySet();
    private final RegionSentryPro plugin;

    public MitigationListener(RegionSentryPro plugin) {
        this.plugin = plugin;
    }

    public static RegionTracker getTrackerForChunk(PerformanceMonitor monitor, String worldName, int x, int z) {
        if (monitor == null) return null;
        for (RegionTracker tracker : monitor.getTrackers()) {
            for (ChunkKey key : tracker.getChunks()) {
                if (key.getX() == x && key.getZ() == z && key.getWorldName().equals(worldName)) {
                    return tracker;
                }
            }
        }
        return null;
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        org.bukkit.block.Block block = event.getBlock();
        RegionTracker tracker = getTrackerForChunk(plugin.getPerformanceMonitor(), block.getWorld().getName(), block.getChunk().getX(), block.getChunk().getZ());
        if (tracker != null && HALTED_REGIONS.contains(tracker.getRegionId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        org.bukkit.inventory.InventoryHolder sourceHolder = event.getSource().getHolder();
        org.bukkit.inventory.InventoryHolder destHolder = event.getDestination().getHolder();
        if (sourceHolder instanceof org.bukkit.block.Container) {
            org.bukkit.block.Container container = (org.bukkit.block.Container) sourceHolder;
            org.bukkit.Location loc = container.getLocation();
            RegionTracker tracker = getTrackerForChunk(plugin.getPerformanceMonitor(), loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
            if (tracker != null && HALTED_REGIONS.contains(tracker.getRegionId())) {
                event.setCancelled(true);
                return;
            }
        }
        if (destHolder instanceof org.bukkit.block.Container) {
            org.bukkit.block.Container container = (org.bukkit.block.Container) destHolder;
            org.bukkit.Location loc = container.getLocation();
            RegionTracker tracker = getTrackerForChunk(plugin.getPerformanceMonitor(), loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
            if (tracker != null && HALTED_REGIONS.contains(tracker.getRegionId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        org.bukkit.inventory.InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof org.bukkit.block.Container) {
            org.bukkit.block.Container container = (org.bukkit.block.Container) holder;
            org.bukkit.Location loc = container.getLocation();
            RegionTracker tracker = getTrackerForChunk(plugin.getPerformanceMonitor(), loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
            if (tracker != null && HALTED_REGIONS.contains(tracker.getRegionId())) {
                event.setCancelled(true);
            }
        }
    }
}

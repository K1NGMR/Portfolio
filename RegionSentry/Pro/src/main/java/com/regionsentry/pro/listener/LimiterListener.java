package com.regionsentry.pro.listener;

import com.regionsentry.lite.monitor.PerformanceMonitor;
import com.regionsentry.lite.monitor.RegionTracker;
import com.regionsentry.pro.RegionSentryPro;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;

public final class LimiterListener implements Listener {
    private final RegionSentryPro plugin;

    public LimiterListener(RegionSentryPro plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof org.bukkit.entity.Player) return;
        checkLimitAndCancel(entity, event);
    }

    @EventHandler
    public void onVehicleCreate(VehicleCreateEvent event) {
        checkLimitAndCancel(event.getVehicle(), event);
    }

    private void checkLimitAndCancel(Entity entity, org.bukkit.event.Cancellable event) {
        ConfigurationSection limitSection = plugin.getConfig().getConfigurationSection("region-entity-limits");
        if (limitSection == null) return;

        String typeName = entity.getType().name();
        if (!limitSection.contains(typeName)) return;

        int limit = limitSection.getInt(typeName);
        World world = entity.getWorld();
        int cx = entity.getLocation().getBlockX() >> 4;
        int cz = entity.getLocation().getBlockZ() >> 4;

        RegionTracker tracker = MitigationListener.getTrackerForChunk(
                plugin.getPerformanceMonitor(),
                world.getName(),
                cx,
                cz
        );

        if (tracker == null) return;

        int count = 0;
        for (com.regionsentry.lite.monitor.ChunkKey key : tracker.getChunks()) {
            if (world.isChunkLoaded(key.getX(), key.getZ())) {
                Chunk chunk = world.getChunkAt(key.getX(), key.getZ());
                for (Entity e : chunk.getEntities()) {
                    if (e.getType() == entity.getType()) {
                        count++;
                    }
                }
            }
        }

        if (count >= limit) {
            event.setCancelled(true);
            plugin.getLogger().warning("[RegionSentry Pro] Blocked spawn of " + typeName + " in region " + tracker.getRegionId() + " (Limit: " + limit + ", Current: " + count + ")");
        }
    }
}

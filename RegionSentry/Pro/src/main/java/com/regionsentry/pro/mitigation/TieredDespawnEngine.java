package com.regionsentry.pro.mitigation;

import com.regionsentry.lite.monitor.ChunkKey;
import com.regionsentry.lite.monitor.RegionTracker;
import com.regionsentry.pro.RegionSentryPro;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class TieredDespawnEngine {
    private final RegionSentryPro plugin;
    private final Set<String> activeClearingRegions = ConcurrentHashMap.newKeySet();

    public TieredDespawnEngine(RegionSentryPro plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("tiered-despawn.enabled", true)) return;

        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (task) -> {
            checkPerformanceAndClear();
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void checkPerformanceAndClear() {
        double threshold = plugin.getConfig().getDouble("tiered-despawn.phase-1.mspt-threshold", 45.0);

        for (RegionTracker tracker : plugin.getPerformanceMonitor().getTrackers()) {
            if (tracker.getChunks().isEmpty()) continue;
            double mspt = tracker.getAverageMSPT();

            if (mspt >= threshold && !activeClearingRegions.contains(tracker.getRegionId())) {
                activeClearingRegions.add(tracker.getRegionId());
                triggerPhase1(tracker);
            }
        }
    }

    private void triggerPhase1(RegionTracker tracker) {
        ChunkKey rep = tracker.getChunks().iterator().next();
        World world = Bukkit.getWorld(rep.getWorldName());
        if (world == null) {
            activeClearingRegions.remove(tracker.getRegionId());
            return;
        }

        Bukkit.getRegionScheduler().execute(plugin, world, rep.getX(), rep.getZ(), () -> {
            plugin.getLogger().warning("[RegionSentry Pro] Strained region " + tracker.getRegionId() + " detected (" + String.format("%.2f", tracker.getAverageMSPT()) + "ms). Triggering Tiered Despawn Phase 1...");
            
            List<String> phase1Types = plugin.getConfig().getStringList("tiered-despawn.phase-1.entities");
            int removedCount = 0;

            for (ChunkKey key : tracker.getChunks()) {
                if (world.isChunkLoaded(key.getX(), key.getZ())) {
                    Chunk chunk = world.getChunkAt(key.getX(), key.getZ());
                    for (Entity entity : chunk.getEntities()) {
                        if (isPhase1Target(entity, phase1Types)) {
                            entity.remove();
                            removedCount++;
                        }
                    }
                }
            }

            plugin.getLogger().info("[RegionSentry Pro] Phase 1 complete. Purged " + removedCount + " low-value entities in region " + tracker.getRegionId());

            Bukkit.getRegionScheduler().runDelayed(plugin, world, rep.getX(), rep.getZ(), (scheduledTask) -> {
                triggerPhase2(tracker);
            }, 200);
        });
    }

    private void triggerPhase2(RegionTracker tracker) {
        ChunkKey rep = tracker.getChunks().iterator().next();
        World world = Bukkit.getWorld(rep.getWorldName());
        if (world == null) {
            activeClearingRegions.remove(tracker.getRegionId());
            return;
        }

        double threshold = plugin.getConfig().getDouble("tiered-despawn.phase-1.mspt-threshold", 45.0);
        double mspt = tracker.getAverageMSPT();

        if (mspt >= threshold) {
            plugin.getLogger().warning("[RegionSentry Pro] Region " + tracker.getRegionId() + " remains strained after Phase 1 (" + String.format("%.2f", mspt) + "ms). Triggering Tiered Despawn Phase 2...");

            List<String> phase2Types = plugin.getConfig().getStringList("tiered-despawn.phase-2.entities");
            int removedCount = 0;

            for (ChunkKey key : tracker.getChunks()) {
                if (world.isChunkLoaded(key.getX(), key.getZ())) {
                    Chunk chunk = world.getChunkAt(key.getX(), key.getZ());
                    for (Entity entity : chunk.getEntities()) {
                        if (isPhase2Target(entity, phase2Types)) {
                            entity.remove();
                            removedCount++;
                        }
                    }
                }
            }

            plugin.getLogger().warning("[RegionSentry Pro] Phase 2 complete. Purged " + removedCount + " hostile entities in region " + tracker.getRegionId());
        } else {
            plugin.getLogger().info("[RegionSentry Pro] Region " + tracker.getRegionId() + " recovered after Phase 1. Aborting Phase 2.");
        }

        activeClearingRegions.remove(tracker.getRegionId());
    }

    private boolean isPhase1Target(Entity entity, List<String> configList) {
        if (entity instanceof Player) return false;
        if (entity.getCustomName() != null) return false;

        String typeName = entity.getType().name();
        if (configList.contains(typeName)) {
            if (entity instanceof Animals) {
                Animals animal = (Animals) entity;
                if (animal.isLoveMode()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean isPhase2Target(Entity entity, List<String> configList) {
        if (entity instanceof Player) return false;
        if (entity.getCustomName() != null) return false;

        String typeName = entity.getType().name();
        return configList.contains(typeName);
    }
}

package com.regionsentry.pro.mitigation;

import com.regionsentry.lite.monitor.ChunkKey;
import com.regionsentry.lite.monitor.PerformanceMonitor;
import com.regionsentry.lite.monitor.RegionTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AdaptiveTickingManager {
    private final JavaPlugin plugin;
    private final PerformanceMonitor monitor;

    private final Map<UUID, Integer> alteredPlayerSimDistances = new ConcurrentHashMap<>();
    private final Set<UUID> throttledMobs = ConcurrentHashMap.newKeySet();

    public AdaptiveTickingManager(JavaPlugin plugin, PerformanceMonitor monitor) {
        this.plugin = plugin;
        this.monitor = monitor;
    }

    public void start() {
        int checkInterval = plugin.getConfig().getInt("adaptive-ticking.check-interval-seconds", 5);
        if (checkInterval < 1) checkInterval = 1;
        // Run check asynchronously
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (task) -> {
            checkAndMitigate();
        }, checkInterval, checkInterval, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void checkAndMitigate() {
        double simDistThreshold = plugin.getConfig().getDouble("adaptive-ticking.sim-distance-threshold-mspt", 45.0);
        int targetSimDist = plugin.getConfig().getInt("adaptive-ticking.sim-distance-value", 2);
        double brainFreezeThreshold = plugin.getConfig().getDouble("adaptive-ticking.brain-freeze-threshold-mspt", 40.0);
        double recoveryThreshold = plugin.getConfig().getDouble("adaptive-ticking.recovery-threshold-mspt", 30.0);

        // We use the lower of brainFreezeThreshold or simDistThreshold to enter mitigation block
        double entryThreshold = Math.min(simDistThreshold, brainFreezeThreshold);

        for (RegionTracker tracker : monitor.getTrackers()) {
            double mspt = tracker.getAverageMSPT();
            if (tracker.getChunks().isEmpty()) continue;

            ChunkKey rep = tracker.getChunks().iterator().next();
            World world = Bukkit.getWorld(rep.getWorldName());
            if (world == null) continue;

            if (mspt > entryThreshold) {
                // Apply mitigations inside the region thread
                try {
                    Bukkit.getRegionScheduler().execute(plugin, world, rep.getX(), rep.getZ(), () -> {
                        // 1. Dynamic Simulation Distance
                        if (mspt > simDistThreshold) {
                            for (ChunkKey key : tracker.getChunks()) {
                                if (world.isChunkLoaded(key.getX(), key.getZ())) {
                                    Chunk chunk = world.getChunkAt(key.getX(), key.getZ());
                                    for (Entity entity : chunk.getEntities()) {
                                        if (entity instanceof Player) {
                                            Player player = (Player) entity;
                                            int currentDist = player.getSimulationDistance();
                                            if (currentDist > targetSimDist) {
                                                alteredPlayerSimDistances.putIfAbsent(player.getUniqueId(), currentDist);
                                                player.setSimulationDistance(targetSimDist);
                                                player.sendMessage(ChatColor.GOLD + "[RegionSentry] Local thread performance strained (" + String.format("%.1f ms", mspt) + "). Lowering simulation distance to " + targetSimDist + " chunks.");
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Entity AI Throttling / Brain Freeze
                        if (mspt > brainFreezeThreshold) {
                            int freezeCount = 0;
                            for (ChunkKey key : tracker.getChunks()) {
                                if (world.isChunkLoaded(key.getX(), key.getZ())) {
                                    Chunk chunk = world.getChunkAt(key.getX(), key.getZ());
                                    for (Entity entity : chunk.getEntities()) {
                                        if (entity instanceof Mob) {
                                            Mob mob = (Mob) entity;
                                            // Exclude bosses, custom named mobs, or already throttled mobs
                                            if (!(mob instanceof Boss) && mob.getCustomName() == null && mob.isAware()) {
                                                mob.setAware(false);
                                                throttledMobs.add(mob.getUniqueId());
                                                freezeCount++;
                                            }
                                        }
                                    }
                                }
                            }
                            if (freezeCount > 0) {
                                plugin.getLogger().info("Brain Freeze: Stripped AI from " + freezeCount + " mobs in region " + tracker.getThreadId() + " due to " + String.format("%.1f ms", mspt) + " MSPT.");
                            }
                        }
                    });
                } catch (Exception ignored) {}
            } else if (mspt < recoveryThreshold) {
                // Restore values back to normal inside the region thread
                try {
                    Bukkit.getRegionScheduler().execute(plugin, world, rep.getX(), rep.getZ(), () -> {
                        int restoreCount = 0;
                        for (ChunkKey key : tracker.getChunks()) {
                            if (world.isChunkLoaded(key.getX(), key.getZ())) {
                                Chunk chunk = world.getChunkAt(key.getX(), key.getZ());
                                for (Entity entity : chunk.getEntities()) {
                                    // 1. Restore Simulation Distance
                                    if (entity instanceof Player) {
                                        Player player = (Player) entity;
                                        Integer defaultDist = alteredPlayerSimDistances.remove(player.getUniqueId());
                                        if (defaultDist != null) {
                                            player.setSimulationDistance(defaultDist);
                                            player.sendMessage(ChatColor.GREEN + "[RegionSentry] Local thread performance stabilized. Restored simulation distance.");
                                        }
                                    }

                                    // 2. Restore Mob AI
                                    if (entity instanceof Mob) {
                                        Mob mob = (Mob) entity;
                                        if (throttledMobs.remove(mob.getUniqueId())) {
                                            mob.setAware(true);
                                            restoreCount++;
                                        }
                                    }
                                }
                            }
                        }
                        if (restoreCount > 0) {
                            plugin.getLogger().info("Brain Thaw: Restored AI to " + restoreCount + " mobs in region " + tracker.getThreadId() + ".");
                        }
                    });
                } catch (Exception ignored) {}
            }
        }
    }
}

package com.regionsentry.pro.mitigation;

import com.regionsentry.lite.monitor.ChunkKey;
import com.regionsentry.lite.monitor.RegionTracker;
import com.regionsentry.pro.RegionSentryPro;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class AFKTracker implements Listener {
    private final RegionSentryPro plugin;
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final Map<String, Boolean> afkRegions = new ConcurrentHashMap<>();

    public AFKTracker(RegionSentryPro plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        int checkInterval = plugin.getConfig().getInt("afk-optimization.check-interval-minutes", 1);
        if (checkInterval < 1) checkInterval = 1;
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (task) -> {
            checkAFKRegions();
        }, checkInterval, checkInterval, TimeUnit.MINUTES);
    }

    private void updateActivity(Player player) {
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        
        int cx = player.getLocation().getBlockX() >> 4;
        int cz = player.getLocation().getBlockZ() >> 4;
        RegionTracker tracker = com.regionsentry.pro.listener.MitigationListener.getTrackerForChunk(
                plugin.getPerformanceMonitor(),
                player.getWorld().getName(),
                cx,
                cz
        );
        if (tracker != null && Boolean.TRUE.equals(afkRegions.get(tracker.getRegionId()))) {
            afkRegions.put(tracker.getRegionId(), false);
            ChunkKey rep = tracker.getChunks().iterator().next();
            World world = Bukkit.getWorld(rep.getWorldName());
            if (world != null) {
                Bukkit.getRegionScheduler().execute(plugin, world, rep.getX(), rep.getZ(), () -> {
                    plugin.getLogger().info("[RegionSentry Pro] Restoring ticking for active region: " + tracker.getRegionId());
                    int serverSimDistance = world.getSimulationDistance();
                    for (ChunkKey key : tracker.getChunks()) {
                        if (world.isChunkLoaded(key.getX(), key.getZ())) {
                            Chunk chunk = world.getChunkAt(key.getX(), key.getZ());
                            for (Entity entity : chunk.getEntities()) {
                                if (entity instanceof Player) {
                                    ((Player) entity).setSimulationDistance(serverSimDistance);
                                } else if (entity instanceof Mob) {
                                    ((Mob) entity).setAware(true);
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        int fx = event.getFrom().getBlockX() >> 4;
        int fz = event.getFrom().getBlockZ() >> 4;
        int tx = event.getTo().getBlockX() >> 4;
        int tz = event.getTo().getBlockZ() >> 4;
        if (fx != tx || fz != tz) {
            updateActivity(event.getPlayer());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastActivity.remove(event.getPlayer().getUniqueId());
    }

    private void checkAFKRegions() {
        long now = System.currentTimeMillis();
        int thresholdMin = plugin.getConfig().getInt("afk-optimization.afk-threshold-minutes", 30);
        long afkThreshold = (long) thresholdMin * 60 * 1000;
        int afkSimDist = plugin.getConfig().getInt("afk-optimization.afk-sim-distance", 2);
        boolean brainFreeze = plugin.getConfig().getBoolean("afk-optimization.brain-freeze-on-afk", true);

        for (RegionTracker tracker : plugin.getPerformanceMonitor().getTrackers()) {
            if (tracker.getChunks().isEmpty()) continue;

            ChunkKey rep = tracker.getChunks().iterator().next();
            World world = Bukkit.getWorld(rep.getWorldName());
            if (world == null) continue;

            Bukkit.getRegionScheduler().execute(plugin, world, rep.getX(), rep.getZ(), () -> {
                boolean allAFK = true;
                boolean hasPlayers = false;

                for (ChunkKey key : tracker.getChunks()) {
                    if (world.isChunkLoaded(key.getX(), key.getZ())) {
                        Chunk chunk = world.getChunkAt(key.getX(), key.getZ());
                        for (Entity entity : chunk.getEntities()) {
                            if (entity instanceof Player) {
                                hasPlayers = true;
                                Long lastActive = lastActivity.get(entity.getUniqueId());
                                if (lastActive == null || (now - lastActive) <= afkThreshold) {
                                    allAFK = false;
                                    break;
                                }
                            }
                        }
                    }
                    if (!allAFK) break;
                }

                if (hasPlayers && allAFK && !Boolean.TRUE.equals(afkRegions.get(tracker.getRegionId()))) {
                    afkRegions.put(tracker.getRegionId(), true);
                    plugin.getLogger().warning("[RegionSentry Pro] Region " + tracker.getRegionId() + " is idle (all players AFK >" + thresholdMin + "m). Optimizing ticking updates.");
                    
                    for (ChunkKey key : tracker.getChunks()) {
                        if (world.isChunkLoaded(key.getX(), key.getZ())) {
                            Chunk chunk = world.getChunkAt(key.getX(), key.getZ());
                            for (Entity entity : chunk.getEntities()) {
                                if (entity instanceof Player) {
                                    ((Player) entity).setSimulationDistance(afkSimDist);
                                } else if (entity instanceof Mob) {
                                    if (brainFreeze) {
                                        ((Mob) entity).setAware(false);
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }
}

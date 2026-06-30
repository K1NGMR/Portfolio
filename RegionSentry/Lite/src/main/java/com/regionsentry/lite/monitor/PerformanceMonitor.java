package com.regionsentry.lite.monitor;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PerformanceMonitor {
    private final JavaPlugin plugin;
    private final Map<String, RegionTracker> trackers = new ConcurrentHashMap<>();
    private final Set<String> activeRegionIds = ConcurrentHashMap.newKeySet();
    public static final Map<String, String> STITCHED_REGIONS = new ConcurrentHashMap<>();

    public PerformanceMonitor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int scanInterval = plugin.getConfig().getInt("performance-monitor.scan-interval-seconds", 5);
        if (scanInterval < 1) scanInterval = 1;
        // Run mapping scan asynchronously
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (scheduledTask) -> {
            scanRegions();
        }, 1, scanInterval, java.util.concurrent.TimeUnit.SECONDS);

        // Thread-stall watchdog runs asynchronously every 1 second
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (scheduledTask) -> {
            long now = System.currentTimeMillis();
            long watchdogStallMs = plugin.getConfig().getLong("performance-monitor.watchdog-stall-ms", 2000L);
            for (RegionTracker tracker : trackers.values()) {
                long lastTick = tracker.getLastTickTimestamp();
                if (lastTick > 0 && (now - lastTick) > watchdogStallMs) {
                    long threadId = tracker.getThreadId();
                    if (threadId != -1) {
                        Thread matchedThread = null;
                        for (Thread t : Thread.getAllStackTraces().keySet()) {
                            if (t.getId() == threadId) {
                                matchedThread = t;
                                break;
                            }
                        }
                        if (matchedThread != null) {
                            StringBuilder stackDump = new StringBuilder();
                            stackDump.append("[RegionSentry Watchdog] CRITICAL: Region Thread '")
                                     .append(tracker.getThreadName())
                                     .append("' (ID: ").append(threadId)
                                     .append(") has STALLED for ")
                                     .append((now - lastTick) / 1000.0)
                                     .append("s! Stack trace:\n");
                            for (StackTraceElement element : matchedThread.getStackTrace()) {
                                stackDump.append("    at ").append(element.toString()).append("\n");
                            }
                            plugin.getLogger().severe(stackDump.toString());
                        }
                    }
                }
            }
        }, 1, 1, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void stop() {
        trackers.clear();
        activeRegionIds.clear();
    }

    public Collection<RegionTracker> getTrackers() {
        return trackers.values();
    }

    private void scanRegions() {
        Set<String> clearedInThisPass = new HashSet<>();
        // Collect loaded chunks per world
        Map<String, Set<ChunkKey>> worldChunks = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            Set<ChunkKey> chunkKeys = new HashSet<>();
            for (Chunk chunk : world.getLoadedChunks()) {
                chunkKeys.add(new ChunkKey(world.getName(), chunk.getX(), chunk.getZ()));
            }
            worldChunks.put(world.getName(), chunkKeys);
        }

        // Group loaded chunks into physical contiguous regions using BFS
        Set<String> scannedRegionIds = new HashSet<>();
        List<Set<ChunkKey>> regionsList = new ArrayList<>();

        for (Map.Entry<String, Set<ChunkKey>> entry : worldChunks.entrySet()) {
            String worldName = entry.getKey();
            Set<ChunkKey> unvisited = new HashSet<>(entry.getValue());

            while (!unvisited.isEmpty()) {
                ChunkKey start = unvisited.iterator().next();
                unvisited.remove(start);

                Set<ChunkKey> regionGroup = new HashSet<>();
                Queue<ChunkKey> queue = new LinkedList<>();
                queue.add(start);
                regionGroup.add(start);

                while (!queue.isEmpty()) {
                    ChunkKey current = queue.poll();
                    Iterator<ChunkKey> iter = unvisited.iterator();
                    while (iter.hasNext()) {
                        ChunkKey potential = iter.next();
                        // Folia groups chunks within distance of 8 chunks into the same region
                        if (Math.abs(current.getX() - potential.getX()) <= 8 &&
                            Math.abs(current.getZ() - potential.getZ()) <= 8) {
                            queue.add(potential);
                            regionGroup.add(potential);
                            iter.remove();
                        }
                    }
                }
                regionsList.add(regionGroup);
            }
        }

        // Update active trackers for each detected region
        for (Set<ChunkKey> group : regionsList) {
            if (group.isEmpty()) continue;

            // Sort to select a stable representative chunk
            List<ChunkKey> sortedList = new ArrayList<>(group);
            sortedList.sort((a, b) -> {
                int cx = Integer.compare(a.getX(), b.getX());
                if (cx != 0) return cx;
                return Integer.compare(a.getZ(), b.getZ());
            });

            ChunkKey rep = sortedList.get(0);
            String regionId = rep.getWorldName() + ":" + rep.getX() + ":" + rep.getZ();
            
            String finalRegionId = regionId;
            while (STITCHED_REGIONS.containsKey(finalRegionId)) {
                finalRegionId = STITCHED_REGIONS.get(finalRegionId);
            }
            
            scannedRegionIds.add(finalRegionId);

            final String fId = finalRegionId;
            trackers.computeIfAbsent(fId, id -> {
                RegionTracker newTracker = new RegionTracker(id);
                activeRegionIds.add(id);
                World world = Bukkit.getWorld(rep.getWorldName());
                if (world != null) {
                    startTrackerTask(newTracker, world, rep.getX(), rep.getZ());
                }
                return newTracker;
            });

            RegionTracker tracker = trackers.get(fId);
            if (tracker != null) {
                if (!clearedInThisPass.contains(fId)) {
                    tracker.getChunks().clear();
                    clearedInThisPass.add(fId);
                }
                tracker.getChunks().addAll(group);
            }
        }

        // Prune inactive trackers
        for (String id : activeRegionIds) {
            if (!scannedRegionIds.contains(id)) {
                activeRegionIds.remove(id);
                trackers.remove(id);
            }
        }
    }

    private void startTrackerTask(RegionTracker tracker, World world, int cx, int cz) {
        Bukkit.getRegionScheduler().runAtFixedRate(plugin, world, cx, cz, (scheduledTask) -> {
            if (!activeRegionIds.contains(tracker.getRegionId())) {
                scheduledTask.cancel();
                return;
            }

            int entities = 0;
            int players = 0;
            List<Player> activePlayers = new ArrayList<>();

            // Iterate over all chunks registered under this tracker
            for (ChunkKey key : tracker.getChunks()) {
                World w = Bukkit.getWorld(key.getWorldName());
                if (w == null) continue;

                Location loc = new Location(w, key.getX() * 16, 64, key.getZ() * 16);
                if (!Bukkit.isOwnedByCurrentRegion(loc)) {
                    continue;
                }

                if (w.isChunkLoaded(key.getX(), key.getZ())) {
                    Chunk chunk = w.getChunkAt(key.getX(), key.getZ());
                    Entity[] chunkEntities = chunk.getEntities();
                    entities += chunkEntities.length;
                    for (Entity entity : chunkEntities) {
                        if (entity instanceof Player) {
                            players++;
                            activePlayers.add((Player) entity);
                        }
                    }
                }
            }

            // Calculate fragmentation risk and merge warnings
            double maxSpeedTowardsBoundary = 0.0;
            boolean imminentMerge = false;
            for (Player player : activePlayers) {
                Location currentLoc = player.getLocation();
                Location lastLoc = tracker.getLastPlayerLocation(player.getUniqueId());
                tracker.setPlayerLocation(player.getUniqueId(), currentLoc);

                if (lastLoc != null && lastLoc.getWorld().getName().equals(currentLoc.getWorld().getName())) {
                    org.bukkit.util.Vector movement = currentLoc.toVector().subtract(lastLoc.toVector());
                    double distToBoundary = getDistanceToAdjacentRegionBoundary(tracker, currentLoc);
                    if (distToBoundary > 0) {
                        org.bukkit.util.Vector dir = getDirectionToNearestBoundary(tracker, currentLoc);
                        double speedTowardsBoundary = movement.dot(dir);
                        if (speedTowardsBoundary > maxSpeedTowardsBoundary) {
                            maxSpeedTowardsBoundary = speedTowardsBoundary;
                        }
                        double blocksPerSec = speedTowardsBoundary * 4.0;
                        if (blocksPerSec > 0.1 && (distToBoundary / blocksPerSec) < 30.0) {
                            imminentMerge = true;
                        }
                    }
                }
            }

            // Clean up inactive players
            Set<UUID> activeUuids = new java.util.HashSet<>();
            for (Player p : activePlayers) {
                activeUuids.add(p.getUniqueId());
            }
            tracker.cleanUpInactivePlayers(activeUuids);

            if (imminentMerge && tracker.getPlayerCount() > 0) {
                tracker.setFragmentationRisk("HIGH");
                tracker.setMergeWarning(true);
            } else if (maxSpeedTowardsBoundary * 4.0 > 1.5 && tracker.getPlayerCount() > 0) {
                tracker.setFragmentationRisk("MEDIUM");
                tracker.setMergeWarning(false);
            } else {
                tracker.setFragmentationRisk("LOW");
                tracker.setMergeWarning(false);
            }

            // Smart World-Save Throttling
            double autosaveThreshold = plugin.getConfig().getDouble("performance-monitor.smart-autosave-mspt-threshold", 30.0);
            if (tracker.getAverageMSPT() > autosaveThreshold) {
                if (world.isAutoSave()) {
                    world.setAutoSave(false);
                    plugin.getLogger().warning("[RegionSentry Lite] Region " + tracker.getRegionId() + " is Strained/Lagging (" + String.format("%.2f", tracker.getAverageMSPT()) + "ms). Temporarily deferred autosave for world: " + world.getName());
                }
                for (ChunkKey key : tracker.getChunks()) {
                    World w = Bukkit.getWorld(key.getWorldName());
                    if (w != null) {
                        w.addPluginChunkTicket(key.getX(), key.getZ(), plugin);
                    }
                }
            } else {
                for (ChunkKey key : tracker.getChunks()) {
                    World w = Bukkit.getWorld(key.getWorldName());
                    if (w != null) {
                        w.removePluginChunkTicket(key.getX(), key.getZ(), plugin);
                    }
                }
                boolean worldStillStrained = false;
                for (RegionTracker t : trackers.values()) {
                    if (t.getAverageMSPT() > autosaveThreshold) {
                        for (ChunkKey ck : t.getChunks()) {
                            if (ck.getWorldName().equals(world.getName())) {
                                worldStillStrained = true;
                                break;
                            }
                        }
                    }
                    if (worldStillStrained) break;
                }
                if (!worldStillStrained && !world.isAutoSave()) {
                    world.setAutoSave(true);
                    plugin.getLogger().info("[RegionSentry Lite] Region performance stabilized. Re-enabled autosave for world: " + world.getName());
                }
            }

            tracker.setLastTickTimestamp(System.currentTimeMillis());
            tracker.updateMetrics(entities, players);
        }, 1, 5); // every 5 ticks (~250ms)
    }

    private double getDistanceToAdjacentRegionBoundary(RegionTracker tracker, Location loc) {
        double minDistance = Double.MAX_VALUE;
        int px = loc.getBlockX() >> 4;
        int pz = loc.getBlockZ() >> 4;

        for (RegionTracker other : trackers.values()) {
            if (other.getRegionId().equals(tracker.getRegionId())) continue;
            for (ChunkKey key : other.getChunks()) {
                if (key.getWorldName().equals(loc.getWorld().getName())) {
                    int dx = Math.abs(key.getX() - px);
                    int dz = Math.abs(key.getZ() - pz);
                    if (dx <= 12 && dz <= 12) {
                        double dist = Math.sqrt(dx * dx + dz * dz) * 16.0;
                        if (dist < minDistance) {
                            minDistance = dist;
                        }
                    }
                }
            }
        }
        return minDistance == Double.MAX_VALUE ? -1.0 : minDistance;
    }

    private org.bukkit.util.Vector getDirectionToNearestBoundary(RegionTracker tracker, Location loc) {
        int px = loc.getBlockX() >> 4;
        int pz = loc.getBlockZ() >> 4;
        ChunkKey nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (RegionTracker other : trackers.values()) {
            if (other.getRegionId().equals(tracker.getRegionId())) continue;
            for (ChunkKey key : other.getChunks()) {
                if (key.getWorldName().equals(loc.getWorld().getName())) {
                    int dx = key.getX() - px;
                    int dz = key.getZ() - pz;
                    double dist = dx * dx + dz * dz;
                    if (dist < minDistance) {
                        minDistance = dist;
                        nearest = key;
                    }
                }
            }
        }
        if (nearest != null) {
            Location boundaryLoc = new Location(loc.getWorld(), nearest.getX() * 16 + 8, loc.getY(), nearest.getZ() * 16 + 8);
            return boundaryLoc.toVector().subtract(loc.toVector()).normalize();
        }
        return new org.bukkit.util.Vector(0, 0, 0);
    }
}

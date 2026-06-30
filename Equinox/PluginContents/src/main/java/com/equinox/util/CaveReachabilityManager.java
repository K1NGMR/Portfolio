package com.equinox.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CaveReachabilityManager {

    // Thread-safe cache of reachable block keys per player to prevent asynchronous Bukkit API access.
    private static final Map<UUID, Set<Long>> cache = new ConcurrentHashMap<>();

    /**
     * Updates the reachability cache for a given player. Must be run on the main server thread.
     */
    public static void updateReachableBlocks(Player player) {
        if (player == null) return;
        if (!player.isOnline()) {
            cache.remove(player.getUniqueId());
            return;
        }
        Location eye = player.getEyeLocation();
        Location feet = player.getLocation();
        Set<Long> reachable = getReachableBlocks(eye, feet);
        cache.put(player.getUniqueId(), reachable);
    }

    /**
     * Retrieves the cached reachability set for a player. Safe to call asynchronously.
     */
    public static Set<Long> getReachableBlocksCached(UUID uuid) {
        return cache.getOrDefault(uuid, Collections.emptySet());
    }

    /**
     * Removes a player from the cache.
     */
    public static void removePlayer(UUID uuid) {
        cache.remove(uuid);
    }

    /**
     * Finds all reachable positions starting from player eye and feet locations.
     * Only traverses through passable or non-occluding blocks (air, water, glass, slabs, stairs, doors, chests, etc.).
     */
    private static Set<Long> getReachableBlocks(Location eye, Location feet) {
        Set<Long> reachable = new HashSet<>();
        Queue<Location> queue = new ArrayDeque<>();
        
        World world = eye.getWorld();
        if (world == null) return reachable;

        Location startEye = eye.getBlock().getLocation();
        Location startFeet = feet.getBlock().getLocation();

        int startChunkX = startFeet.getBlockX() >> 4;
        int startChunkZ = startFeet.getBlockZ() >> 4;
        if (!world.isChunkLoaded(startChunkX, startChunkZ)) {
            return reachable;
        }

        queue.add(startEye);
        reachable.add(blockLocToKey(startEye.getBlockX(), startEye.getBlockY(), startEye.getBlockZ()));

        long feetKey = blockLocToKey(startFeet.getBlockX(), startFeet.getBlockY(), startFeet.getBlockZ());
        if (reachable.add(feetKey)) {
            queue.add(startFeet);
        }

        int startX = startFeet.getBlockX();
        int startY = startFeet.getBlockY();
        int startZ = startFeet.getBlockZ();
        int radiusSq = 50 * 50;

        int[] dx = {1, -1, 0, 0, 0, 0};
        int[] dy = {0, 0, 1, -1, 0, 0};
        int[] dz = {0, 0, 0, 0, 1, -1};

        // Safety: Limit maximum number of iterations to prevent hanging on huge caves
        int iterations = 0;
        int maxIterations = 20000;

        while (!queue.isEmpty() && iterations++ < maxIterations) {
            Location curr = queue.poll();
            int cx = curr.getBlockX();
            int cy = curr.getBlockY();
            int cz = curr.getBlockZ();

            for (int i = 0; i < 6; i++) {
                int nx = cx + dx[i];
                int ny = cy + dy[i];
                int nz = cz + dz[i];

                // Check distance limit of 50 blocks
                int rx = nx - startX;
                int ry = ny - startY;
                int rz = nz - startZ;
                if (rx * rx + ry * ry + rz * rz > radiusSq) {
                    continue;
                }

                // Check world height boundaries
                if (ny < world.getMinHeight() || ny >= world.getMaxHeight()) {
                    continue;
                }

                // Check if target chunk is loaded before accessing the block to avoid loading chunks synchronously
                int nChunkX = nx >> 4;
                int nChunkZ = nz >> 4;
                if (!world.isChunkLoaded(nChunkX, nChunkZ)) {
                    continue;
                }

                long key = blockLocToKey(nx, ny, nz);
                if (reachable.contains(key)) {
                    continue;
                }

                Block block = world.getBlockAt(nx, ny, nz);
                // Traversal allowed if block is passable or non-occluding
                if (isPassableForBfs(block)) {
                    reachable.add(key);
                    queue.add(new Location(world, nx, ny, nz));
                }
            }
        }

        return reachable;
    }

    private static boolean isPassableForBfs(Block block) {
        Material mat = block.getType();
        // Air and fluids are always passable
        if (mat.isAir() || mat == Material.WATER || mat == Material.LAVA) {
            return true;
        }
        // Fences, glass, chests, slabs, stairs, leaves, trapdoors, doors are not occluding
        // (i.e. they do not completely block vision/light/path connectivity)
        return !mat.isOccluding();
    }

    public static long blockLocToKey(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) | (((long) z & 0x3FFFFFFL) << 26) | (((long) y & 0xFFFL) << 52);
    }
}

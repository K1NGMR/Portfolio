package com.equinox.util;

import org.bukkit.Location;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class LineOfSightUtil {

    public static boolean checkLineOfSight(Location start, Location end) {
        if (!start.getWorld().equals(end.getWorld())) return false;
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        if (distance < 0.1) return true;

        RayTraceResult result = start.getWorld().rayTraceBlocks(
                start,
                direction.normalize(),
                distance,
                org.bukkit.FluidCollisionMode.NEVER,
                true
        );

        if (result == null || result.getHitBlock() == null) {
            return true;
        }

        org.bukkit.block.Block hitBlock = result.getHitBlock();
        return hitBlock.getX() == end.getBlockX() &&
               hitBlock.getY() == end.getBlockY() &&
               hitBlock.getZ() == end.getBlockZ();
    }
}

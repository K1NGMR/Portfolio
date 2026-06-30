package com.itemedit.light.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class BukkitSchedulerProvider implements SchedulerProvider {
    @Override
    public Object runTask(Plugin plugin, Runnable runnable) {
        return Bukkit.getScheduler().runTask(plugin, runnable);
    }

    @Override
    public Object runTaskLater(Plugin plugin, Runnable runnable, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    @Override
    public Object runTaskTimer(Plugin plugin, Runnable runnable, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
    }

    @Override
    public Object runTask(Plugin plugin, Entity entity, Runnable runnable) {
        return runTask(plugin, runnable);
    }

    @Override
    public Object runTaskLater(Plugin plugin, Entity entity, Runnable runnable, long delay) {
        return runTaskLater(plugin, runnable, delay);
    }

    @Override
    public Object runTaskTimer(Plugin plugin, Entity entity, Runnable runnable, long delay, long period) {
        return runTaskTimer(plugin, runnable, delay, period);
    }

    @Override
    public Object runTask(Plugin plugin, Location location, Runnable runnable) {
        return runTask(plugin, runnable);
    }

    @Override
    public Object runTaskLater(Plugin plugin, Location location, Runnable runnable, long delay) {
        return runTaskLater(plugin, runnable, delay);
    }

    @Override
    public Object runTaskTimer(Plugin plugin, Location location, Runnable runnable, long delay, long period) {
        return runTaskTimer(plugin, runnable, delay, period);
    }

    @Override
    public Object runTaskAsynchronously(Plugin plugin, Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public Object runTaskLaterAsynchronously(Plugin plugin, Runnable runnable, long delay) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    @Override
    public Object runTaskTimerAsynchronously(Plugin plugin, Runnable runnable, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
    }

    @Override
    public void cancelTask(Object task) {
        if (task instanceof BukkitTask) {
            ((BukkitTask) task).cancel();
        }
    }
}

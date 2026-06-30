package com.itemedit.full.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import java.util.concurrent.TimeUnit;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public class FoliaSchedulerProvider implements SchedulerProvider {
    @Override
    public Object runTask(Plugin plugin, Runnable runnable) {
        return Bukkit.getGlobalRegionScheduler().run(plugin, t -> runnable.run());
    }

    @Override
    public Object runTaskLater(Plugin plugin, Runnable runnable, long delay) {
        long finalDelay = Math.max(1, delay);
        return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> runnable.run(), finalDelay);
    }

    @Override
    public Object runTaskTimer(Plugin plugin, Runnable runnable, long delay, long period) {
        long finalDelay = Math.max(1, delay);
        long finalPeriod = Math.max(1, period);
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> runnable.run(), finalDelay, finalPeriod);
    }

    @Override
    public Object runTask(Plugin plugin, Entity entity, Runnable runnable) {
        return entity.getScheduler().run(plugin, t -> runnable.run(), null);
    }

    @Override
    public Object runTaskLater(Plugin plugin, Entity entity, Runnable runnable, long delay) {
        long finalDelay = Math.max(1, delay);
        return entity.getScheduler().runDelayed(plugin, t -> runnable.run(), null, finalDelay);
    }

    @Override
    public Object runTaskTimer(Plugin plugin, Entity entity, Runnable runnable, long delay, long period) {
        long finalDelay = Math.max(1, delay);
        long finalPeriod = Math.max(1, period);
        return entity.getScheduler().runAtFixedRate(plugin, t -> runnable.run(), null, finalDelay, finalPeriod);
    }

    @Override
    public Object runTask(Plugin plugin, Location location, Runnable runnable) {
        return Bukkit.getRegionScheduler().run(plugin, location, t -> runnable.run());
    }

    @Override
    public Object runTaskLater(Plugin plugin, Location location, Runnable runnable, long delay) {
        long finalDelay = Math.max(1, delay);
        return Bukkit.getRegionScheduler().runDelayed(plugin, location, t -> runnable.run(), finalDelay);
    }

    @Override
    public Object runTaskTimer(Plugin plugin, Location location, Runnable runnable, long delay, long period) {
        long finalDelay = Math.max(1, delay);
        long finalPeriod = Math.max(1, period);
        return Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, t -> runnable.run(), finalDelay, finalPeriod);
    }

    @Override
    public Object runTaskAsynchronously(Plugin plugin, Runnable runnable) {
        return Bukkit.getAsyncScheduler().runNow(plugin, t -> runnable.run());
    }

    @Override
    public Object runTaskLaterAsynchronously(Plugin plugin, Runnable runnable, long delay) {
        long finalDelay = Math.max(1, delay);
        return Bukkit.getAsyncScheduler().runDelayed(plugin, t -> runnable.run(), finalDelay * 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public Object runTaskTimerAsynchronously(Plugin plugin, Runnable runnable, long delay, long period) {
        long finalDelay = Math.max(1, delay);
        long finalPeriod = Math.max(1, period);
        return Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> runnable.run(), finalDelay * 50, finalPeriod * 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void cancelTask(Object task) {
        if (task instanceof ScheduledTask) {
            ((ScheduledTask) task).cancel();
        }
    }
}

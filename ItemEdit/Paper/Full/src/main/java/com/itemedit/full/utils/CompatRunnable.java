package com.itemedit.full.utils;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public abstract class CompatRunnable implements Runnable {
    private Object task;
    private boolean cancelled = false;

    public final synchronized void cancel() throws IllegalStateException {
        if (cancelled) {
            return;
        }
        cancelled = true;
        if (task != null) {
            SchedulerUtils.cancelTask(task);
            task = null;
        }
    }

    private synchronized void setupTask(Object task) {
        this.task = task;
        if (cancelled) {
            SchedulerUtils.cancelTask(task);
            this.task = null;
        }
    }

    public final synchronized CompatRunnable runTask(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
        setupTask(SchedulerUtils.runTask(plugin, this));
        return this;
    }

    public final synchronized CompatRunnable runTaskLater(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
        setupTask(SchedulerUtils.runTaskLater(plugin, this, delay));
        return this;
    }

    public final synchronized CompatRunnable runTaskTimer(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        setupTask(SchedulerUtils.runTaskTimer(plugin, this, delay, period));
        return this;
    }

    public final synchronized CompatRunnable runTaskAsynchronously(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
        setupTask(SchedulerUtils.runTaskAsynchronously(plugin, this));
        return this;
    }

    public final synchronized CompatRunnable runTaskLaterAsynchronously(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
        setupTask(SchedulerUtils.runTaskLaterAsynchronously(plugin, this, delay));
        return this;
    }

    public final synchronized CompatRunnable runTaskTimerAsynchronously(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        setupTask(SchedulerUtils.runTaskTimerAsynchronously(plugin, this, delay, period));
        return this;
    }

    // Context-bound execution overloads (entity and location context)
    public final synchronized CompatRunnable runTask(Plugin plugin, Entity entity) throws IllegalArgumentException, IllegalStateException {
        setupTask(SchedulerUtils.runTask(plugin, entity, this));
        return this;
    }

    public final synchronized CompatRunnable runTaskLater(Plugin plugin, Entity entity, long delay) throws IllegalArgumentException, IllegalStateException {
        setupTask(SchedulerUtils.runTaskLater(plugin, entity, this, delay));
        return this;
    }

    public final synchronized CompatRunnable runTaskTimer(Plugin plugin, Entity entity, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        setupTask(SchedulerUtils.runTaskTimer(plugin, entity, this, delay, period));
        return this;
    }

    public final synchronized CompatRunnable runTask(Plugin plugin, Location location) throws IllegalArgumentException, IllegalStateException {
        setupTask(SchedulerUtils.runTask(plugin, location, this));
        return this;
    }

    public final synchronized CompatRunnable runTaskLater(Plugin plugin, Location location, long delay) throws IllegalArgumentException, IllegalStateException {
        setupTask(SchedulerUtils.runTaskLater(plugin, location, this, delay));
        return this;
    }

    public final synchronized CompatRunnable runTaskTimer(Plugin plugin, Location location, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        setupTask(SchedulerUtils.runTaskTimer(plugin, location, this, delay, period));
        return this;
    }
}

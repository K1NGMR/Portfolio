package com.itemedit.full.utils;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public interface SchedulerProvider {
    Object runTask(Plugin plugin, Runnable runnable);
    Object runTaskLater(Plugin plugin, Runnable runnable, long delay);
    Object runTaskTimer(Plugin plugin, Runnable runnable, long delay, long period);
    
    Object runTask(Plugin plugin, Entity entity, Runnable runnable);
    Object runTaskLater(Plugin plugin, Entity entity, Runnable runnable, long delay);
    Object runTaskTimer(Plugin plugin, Entity entity, Runnable runnable, long delay, long period);
    
    Object runTask(Plugin plugin, Location location, Runnable runnable);
    Object runTaskLater(Plugin plugin, Location location, Runnable runnable, long delay);
    Object runTaskTimer(Plugin plugin, Location location, Runnable runnable, long delay, long period);
    
    Object runTaskAsynchronously(Plugin plugin, Runnable runnable);
    Object runTaskLaterAsynchronously(Plugin plugin, Runnable runnable, long delay);
    Object runTaskTimerAsynchronously(Plugin plugin, Runnable runnable, long delay, long period);
    
    void cancelTask(Object task);
}

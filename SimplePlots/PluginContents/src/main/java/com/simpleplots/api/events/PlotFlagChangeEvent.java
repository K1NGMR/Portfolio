package com.simpleplots.api.events;

import com.simpleplots.api.Plot;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlotFlagChangeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Plot plot;
    private final String flag;
    private final String oldValue;
    private final String newValue;
    private boolean cancelled;

    public PlotFlagChangeEvent(Plot plot, String flag, String oldValue, String newValue) {
        this.plot = plot;
        this.flag = flag;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Plot getPlot() {
        return plot;
    }

    public String getFlag() {
        return flag;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

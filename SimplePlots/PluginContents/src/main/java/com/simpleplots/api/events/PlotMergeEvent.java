package com.simpleplots.api.events;

import com.simpleplots.api.Plot;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlotMergeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Plot plot1;
    private final Plot plot2;
    private final String direction; // NORTH, SOUTH, EAST, WEST
    private boolean cancelled;

    public PlotMergeEvent(Plot plot1, Plot plot2, String direction) {
        this.plot1 = plot1;
        this.plot2 = plot2;
        this.direction = direction;
    }

    public Plot getPlot1() {
        return plot1;
    }

    public Plot getPlot2() {
        return plot2;
    }

    public String getDirection() {
        return direction;
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

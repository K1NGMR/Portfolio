package com.simpleplots.api.events;

import com.simpleplots.api.Plot;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class PlotClaimEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Plot plot;
    private final UUID claimer;
    private boolean cancelled;

    public PlotClaimEvent(Plot plot, UUID claimer) {
        this.plot = plot;
        this.claimer = claimer;
    }

    public Plot getPlot() {
        return plot;
    }

    public UUID getClaimer() {
        return claimer;
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

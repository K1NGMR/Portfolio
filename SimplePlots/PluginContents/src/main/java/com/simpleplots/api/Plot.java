package com.simpleplots.api;

import java.util.*;

/**
 * Represents a claimed plot with its settings, flags, members, and merge states.
 */
public class Plot {
    private final PlotId id;
    private final String world;
    private UUID owner;
    private final Set<UUID> added = new HashSet<>();
    private final Set<UUID> trusted = new HashSet<>();
    private final Set<UUID> denied = new HashSet<>();
    private final Map<String, String> flags = new HashMap<>();
    
    private final Map<UUID, Integer> ratings = new HashMap<>();
    
    // Merge states: indicate if the borders in the cardinal directions are removed
    private boolean mergedN;
    private boolean mergedS;
    private boolean mergedE;
    private boolean mergedW;

    public Plot(PlotId id, String world, UUID owner) {
        this.id = id;
        this.world = world;
        this.owner = owner;
    }

    public PlotId getId() {
        return id;
    }

    public String getWorld() {
        return world;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Set<UUID> getAdded() {
        return added;
    }

    public Set<UUID> getTrusted() {
        return trusted;
    }

    public Set<UUID> getDenied() {
        return denied;
    }

    public Map<String, String> getFlags() {
        return flags;
    }

    public boolean isMergedN() {
        return mergedN;
    }

    public void setMergedN(boolean mergedN) {
        this.mergedN = mergedN;
    }

    public boolean isMergedS() {
        return mergedS;
    }

    public void setMergedS(boolean mergedS) {
        this.mergedS = mergedS;
    }

    public boolean isMergedE() {
        return mergedE;
    }

    public void setMergedE(boolean mergedE) {
        this.mergedE = mergedE;
    }

    public boolean isMergedW() {
        return mergedW;
    }

    public void setMergedW(boolean mergedW) {
        this.mergedW = mergedW;
    }

    public boolean isMergedWith(PlotId otherId) {
        int dx = otherId.getX() - id.getX();
        int dz = otherId.getZ() - id.getZ();
        if (dx == 1 && dz == 0) return mergedE;
        if (dx == -1 && dz == 0) return mergedW;
        if (dx == 0 && dz == 1) return mergedS;
        if (dx == 0 && dz == -1) return mergedN;
        return false;
    }

    public boolean hasFlag(String flag) {
        return flags.containsKey(flag.toLowerCase());
    }

    public String getFlagValue(String flag) {
        return flags.get(flag.toLowerCase());
    }

    public void setFlag(String flag, String value) {
        flags.put(flag.toLowerCase(), value);
    }

    public void removeFlag(String flag) {
        flags.remove(flag.toLowerCase());
    }

    public boolean isAdded(UUID uuid) {
        return added.contains(uuid);
    }

    public boolean isTrusted(UUID uuid) {
        return trusted.contains(uuid);
    }

    public boolean isDenied(UUID uuid) {
        return denied.contains(uuid);
    }

    public boolean isClaimed() {
        return owner != null && !owner.equals(new UUID(0, 0));
    }

    public Map<UUID, Integer> getRatings() {
        return ratings;
    }

    public double getAverageRating() {
        if (ratings.isEmpty()) return 0.0;
        double sum = 0;
        for (int r : ratings.values()) {
            sum += r;
        }
        return sum / ratings.size();
    }
}

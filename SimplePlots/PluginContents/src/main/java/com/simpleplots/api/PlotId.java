package com.simpleplots.api;

import java.util.Objects;

/**
 * Represents a plot coordinate in the grid system.
 */
public class PlotId {
    private final int x;
    private final int z;

    public PlotId(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public static PlotId fromString(String str) {
        if (str == null || !str.contains(";")) {
            return null;
        }
        try {
            String[] parts = str.split(";");
            int x = Integer.parseInt(parts[0]);
            int z = Integer.parseInt(parts[1]);
            return new PlotId(x, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public String toString() {
        return x + ";" + z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlotId)) return false;
        PlotId plotId = (PlotId) o;
        return x == plotId.x && z == plotId.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}

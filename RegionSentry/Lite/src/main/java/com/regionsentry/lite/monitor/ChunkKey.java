package com.regionsentry.lite.monitor;

import java.util.Objects;

public final class ChunkKey {
    private final String worldName;
    private final int x;
    private final int z;

    public ChunkKey(String worldName, int x, int z) {
        this.worldName = worldName;
        this.x = x;
        this.z = z;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkKey)) return false;
        ChunkKey chunkKey = (ChunkKey) o;
        return x == chunkKey.x && z == chunkKey.z && Objects.equals(worldName, chunkKey.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName, x, z);
    }

    @Override
    public String toString() {
        return worldName + " [" + x + "," + z + "]";
    }
}

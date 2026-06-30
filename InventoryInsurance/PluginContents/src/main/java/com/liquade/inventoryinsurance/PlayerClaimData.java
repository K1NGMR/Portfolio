package com.liquade.inventoryinsurance;

import org.bukkit.inventory.ItemStack;
import java.util.Map;

public class PlayerClaimData {
    private final Map<Integer, ItemStack> items; // Map of slot index -> ItemStack
    private final int xpLevel;
    private final float xpProgress;
    private final long deathTime;
    private final String deathWorld;
    private final double deathX;
    private final double deathY;
    private final double deathZ;

    public PlayerClaimData(Map<Integer, ItemStack> items, int xpLevel, float xpProgress, 
                           long deathTime, String deathWorld, double deathX, double deathY, double deathZ) {
        this.items = items;
        this.xpLevel = xpLevel;
        this.xpProgress = xpProgress;
        this.deathTime = deathTime;
        this.deathWorld = deathWorld;
        this.deathX = deathX;
        this.deathY = deathY;
        this.deathZ = deathZ;
    }

    public Map<Integer, ItemStack> getItems() {
        return items;
    }

    public int getXpLevel() {
        return xpLevel;
    }

    public float getXpProgress() {
        return xpProgress;
    }

    public long getDeathTime() {
        return deathTime;
    }

    public String getDeathWorld() {
        return deathWorld;
    }

    public double getDeathX() {
        return deathX;
    }

    public double getDeathY() {
        return deathY;
    }

    public double getDeathZ() {
        return deathZ;
    }
}

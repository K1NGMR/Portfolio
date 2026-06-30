package com.liquade.inventoryinsurance;

public class TierConfig {
    private final String id;
    private final String displayName;
    private final double price;
    private final int durationDays;
    private final boolean saveHotbar;
    private final boolean saveInventory;
    private final boolean saveArmor;
    private final boolean saveOffhand;
    private final boolean saveXp;
    private final int xpRestorePercentage;
    private final String permission;

    public TierConfig(String id, String displayName, double price, int durationDays,
                      boolean saveHotbar, boolean saveInventory, boolean saveArmor,
                      boolean saveOffhand, boolean saveXp, int xpRestorePercentage, String permission) {
        this.id = id;
        this.displayName = displayName;
        this.price = price;
        this.durationDays = durationDays;
        this.saveHotbar = saveHotbar;
        this.saveInventory = saveInventory;
        this.saveArmor = saveArmor;
        this.saveOffhand = saveOffhand;
        this.saveXp = saveXp;
        this.xpRestorePercentage = xpRestorePercentage;
        this.permission = permission;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getPrice() {
        return price;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public boolean isSaveHotbar() {
        return saveHotbar;
    }

    public boolean isSaveInventory() {
        return saveInventory;
    }

    public boolean isSaveArmor() {
        return saveArmor;
    }

    public boolean isSaveOffhand() {
        return saveOffhand;
    }

    public boolean isSaveXp() {
        return saveXp;
    }

    public int getXpRestorePercentage() {
        return xpRestorePercentage;
    }

    public String getPermission() {
        return permission;
    }
}

package com.simpleplots.generator;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class PlotWorldConfig {
    private final String worldName;
    private final int plotSize;
    private final int roadWidth;
    private final int totalSize;
    private final int floorHeight;
    private final Material floorBlock;
    private final Material fillerBlock;
    private final Material bedBlock;
    private final Material roadBlock;
    private final Material wallBlockClaimed;
    private final Material wallBlockUnclaimed;
    private final Material borderBlock;
    private final boolean schematicOnClaim;
    private final String schematicName;
    private final boolean roadSchematic;
    private final double claimPrice;
    private final double mergePrice;
    private final double sellPrice;
    private final double clearPrice;
    private final String entryMessageClaimed;
    private final String entryMessageUnclaimed;
    private final boolean infiniteSaturation;
    private final boolean doDaylightCycle;
    private final boolean doMobSpawning;

    public PlotWorldConfig(String worldName, ConfigurationSection section) {
        this.worldName = worldName;
        this.plotSize = section.getInt("plot-size", 32);
        this.roadWidth = section.getInt("road-width", 7);
        this.totalSize = this.plotSize + this.roadWidth;
        this.floorHeight = section.getInt("floor-height", 64);
        this.floorBlock = getMaterial(section.getString("floor-block"), Material.GRASS_BLOCK);
        this.fillerBlock = getMaterial(section.getString("filler-block"), Material.DIRT);
        this.bedBlock = getMaterial(section.getString("bed-block"), Material.BEDROCK);
        this.roadBlock = getMaterial(section.getString("road-block"), Material.SMOOTH_STONE);
        this.wallBlockClaimed = getMaterial(section.getString("wall-block-claimed"), Material.OAK_SLAB);
        this.wallBlockUnclaimed = getMaterial(section.getString("wall-block-unclaimed"), Material.STONE_SLAB);
        this.borderBlock = getMaterial(section.getString("border-block"), Material.STONE_BRICKS);
        this.schematicOnClaim = section.getBoolean("schematic-on-claim", false);
        this.schematicName = section.getString("schematic-name", "plot.schem");
        this.roadSchematic = section.getBoolean("road-schematic", false);
        this.claimPrice = section.getDouble("economy.claim-price", 100.0);
        this.mergePrice = section.getDouble("economy.merge-price", 250.0);
        this.sellPrice = section.getDouble("economy.sell-price", 50.0);
        this.clearPrice = section.getDouble("economy.clear-price", 50.0);
        this.entryMessageClaimed = section.getString("entry-message-claimed", "&7Entering {owner} Plot");
        this.entryMessageUnclaimed = section.getString("entry-message-unclaimed", "&7Entering Unclaimed Plot");
        this.infiniteSaturation = section.getBoolean("infinite-saturation", true);
        this.doDaylightCycle = section.getBoolean("do-daylight-cycle", false);
        this.doMobSpawning = section.getBoolean("do-mob-spawning", false);
    }

    private Material getMaterial(String name, Material def) {
        if (name == null) return def;
        Material mat = Material.matchMaterial(name.toUpperCase());
        return mat != null ? mat : def;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getPlotSize() {
        return plotSize;
    }

    public int getRoadWidth() {
        return roadWidth;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public int getFloorHeight() {
        return floorHeight;
    }

    public Material getFloorBlock() {
        return floorBlock;
    }

    public Material getFillerBlock() {
        return fillerBlock;
    }

    public Material getBedBlock() {
        return bedBlock;
    }

    public Material getRoadBlock() {
        return roadBlock;
    }

    public Material getWallBlockClaimed() {
        return wallBlockClaimed;
    }

    public Material getWallBlockUnclaimed() {
        return wallBlockUnclaimed;
    }

    public Material getBorderBlock() {
        return borderBlock;
    }

    public boolean isSchematicOnClaim() {
        return schematicOnClaim;
    }

    public String getSchematicName() {
        return schematicName;
    }

    public boolean isRoadSchematic() {
        return roadSchematic;
    }

    public double getClaimPrice() {
        return claimPrice;
    }

    public double getMergePrice() {
        return mergePrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public double getClearPrice() {
        return clearPrice;
    }

    public String getEntryMessageClaimed() {
        return entryMessageClaimed;
    }

    public String getEntryMessageUnclaimed() {
        return entryMessageUnclaimed;
    }

    public boolean isInfiniteSaturation() {
        return infiniteSaturation;
    }

    public boolean isDoDaylightCycle() {
        return doDaylightCycle;
    }

    public boolean isDoMobSpawning() {
        return doMobSpawning;
    }
}

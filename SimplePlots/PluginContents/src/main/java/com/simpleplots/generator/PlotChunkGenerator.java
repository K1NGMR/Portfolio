package com.simpleplots.generator;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

/**
 * Custom ChunkGenerator to generate a grid-based plot world.
 */
public class PlotChunkGenerator extends ChunkGenerator {
    private final PlotWorldConfig config;

    public PlotChunkGenerator(PlotWorldConfig config) {
        this.config = config;
    }

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        // We generate a flat world, so we don't need noise.
    }

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        int totalSize = config.getTotalSize();
        int roadWidth = config.getRoadWidth();
        int halfRoad = roadWidth / 2;
        int floorHeight = config.getFloorHeight();

        Material floorMat = config.getFloorBlock();
        Material fillerMat = config.getFillerBlock();
        Material roadMat = config.getRoadBlock();
        Material borderMat = config.getBorderBlock();
        Material wallMat = config.getWallBlockUnclaimed();

        int startBlockX = chunkX * 16;
        int startBlockZ = chunkZ * 16;

        for (int x = 0; x < 16; x++) {
            int blockX = startBlockX + x;
            int shiftedX = blockX + halfRoad;
            int remX = Math.floorMod(shiftedX, totalSize);

            for (int z = 0; z < 16; z++) {
                int blockZ = startBlockZ + z;
                int shiftedZ = blockZ + halfRoad;
                int remZ = Math.floorMod(shiftedZ, totalSize);

                // 1. Bedrock layer
                chunkData.setBlock(x, 0, z, config.getBedBlock());

                // 2. Filler layer (dirt)
                for (int y = 1; y < floorHeight; y++) {
                    chunkData.setBlock(x, y, z, fillerMat);
                }

                // 3. Surface layer
                boolean mathPlot = (remX >= roadWidth) && (remZ >= roadWidth);
                if (mathPlot) {
                    // Plot surface
                    chunkData.setBlock(x, floorHeight, z, floorMat);
                } else {
                    // Road surface
                    // Check if it's border
                    boolean isBorder = isBorder(remX, remZ, roadWidth, totalSize);
                    if (isBorder) {
                        chunkData.setBlock(x, floorHeight, z, borderMat);
                        // Wall block on top of border
                        chunkData.setBlock(x, floorHeight + 1, z, wallMat);
                    } else {
                        chunkData.setBlock(x, floorHeight, z, roadMat);
                    }
                }
            }
        }
    }

    @Override
    public void generateBedrock(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        // Bedrock is already handled in generateSurface
    }

    private boolean isBorder(int remX, int remZ, int roadWidth, int totalSize) {
        boolean adjX = (remX == roadWidth - 1) || (remX == 0);
        boolean adjZ = (remZ == roadWidth - 1) || (remZ == 0);

        boolean insideX = (remX >= roadWidth - 1) || (remX == 0);
        boolean insideZ = (remZ >= roadWidth - 1) || (remZ == 0);

        return (adjX && insideZ) || (adjZ && insideX);
    }

    @Override
    public org.bukkit.generator.BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return new org.bukkit.generator.BiomeProvider() {
            @Override
            public org.bukkit.block.Biome getBiome(WorldInfo info, int x, int y, int z) {
                return org.bukkit.block.Biome.PLAINS;
            }

            @Override
            public java.util.List<org.bukkit.block.Biome> getBiomes(WorldInfo info) {
                return java.util.Collections.singletonList(org.bukkit.block.Biome.PLAINS);
            }
        };
    }
}

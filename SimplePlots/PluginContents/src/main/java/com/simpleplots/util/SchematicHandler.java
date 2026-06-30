package com.simpleplots.util;

import com.simpleplots.SimplePlots;
import com.simpleplots.api.PlotGeometry;
import com.simpleplots.api.PlotId;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.CompletableFuture;

/**
 * Handles saving and loading schematics using WorldEdit/FAWE Clipboard API.
 */
public class SchematicHandler {
    private final SimplePlots plugin;

    public SchematicHandler(SimplePlots plugin) {
        this.plugin = plugin;
    }

    private boolean isWorldEditEnabled() {
        return Bukkit.getPluginManager().getPlugin("WorldEdit") != null;
    }

    /**
     * Saves a plot's region (including merged plots) to a schematic file asynchronously.
     */
    public CompletableFuture<File> savePlotSchematic(String worldName, PlotId plotId, String fileName) {
        return savePlotSchematic(worldName, plotId, fileName, true);
    }

    public CompletableFuture<File> savePlotSchematic(String worldName, PlotId plotId, String fileName, boolean copyEntities) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isWorldEditEnabled()) {
                throw new IllegalStateException("WorldEdit is not enabled!");
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                throw new IllegalArgumentException("World " + worldName + " is not loaded!");
            }

            // Get merged bounds to support saving merged plots completely
            int[] bounds = PlotGeometry.getMergedPlotBounds(worldName, plotId);
            if (bounds == null) {
                throw new IllegalArgumentException("Plot bounds not found!");
            }

            File schemDir = new File(plugin.getDataFolder(), "schematics");
            if (!schemDir.exists()) {
                schemDir.mkdirs();
            }
            File outFile = new File(schemDir, fileName);

            BukkitWorld weWorld = new BukkitWorld(world);
            BlockVector3 min = BlockVector3.at(bounds[0], 0, bounds[1]);
            BlockVector3 max = BlockVector3.at(bounds[2], world.getMaxHeight(), bounds[3]);

            CuboidRegion region = new CuboidRegion(weWorld, min, max);
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            clipboard.setOrigin(min);

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                        editSession, region, clipboard, region.getMinimumPoint()
                );
                forwardExtentCopy.setCopyingEntities(copyEntities);
                Operations.complete(forwardExtentCopy);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to copy plot content to clipboard: " + e.getMessage());
                e.printStackTrace();
                return null;
            }

            try (FileOutputStream fos = new FileOutputStream(outFile);
                 ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(fos)) {
                writer.write(clipboard);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save clipboard to schematic: " + e.getMessage());
                e.printStackTrace();
                return null;
            }

            return outFile;
        });
    }

    /**
     * Pastes a schematic file into the plot's min coordinates asynchronously.
     */
    public CompletableFuture<Boolean> pastePlotSchematic(String worldName, PlotId plotId, File schemFile) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isWorldEditEnabled()) {
                throw new IllegalStateException("WorldEdit is not enabled!");
            }

            if (!schemFile.exists()) {
                plugin.getLogger().warning("Schematic file does not exist: " + schemFile.getPath());
                return false;
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                throw new IllegalArgumentException("World " + worldName + " is not loaded!");
            }

            int[] bounds = PlotGeometry.getPlotBounds(worldName, plotId);
            if (bounds == null) {
                return false;
            }

            BukkitWorld weWorld = new BukkitWorld(world);
            Clipboard clipboard;

            ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
            if (format == null) {
                plugin.getLogger().warning("Unknown schematic format for: " + schemFile.getName());
                return false;
            }

            try (FileInputStream fis = new FileInputStream(schemFile);
                 ClipboardReader reader = format.getReader(fis)) {
                clipboard = reader.read();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to read schematic: " + e.getMessage());
                e.printStackTrace();
                return false;
            }

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                // Paste at the min block coordinate of the plot cell at Y=floorHeight (or Y=0 depending on need)
                // Default to min block X, plot floorHeight, min block Z
                BlockVector3 to = BlockVector3.at(bounds[0], plugin.getWorldConfig(worldName).getFloorHeight() + 1, bounds[1]);
                
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(to)
                        .ignoreAirBlocks(false)
                        .build();
                Operations.complete(operation);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to paste schematic: " + e.getMessage());
                e.printStackTrace();
                return false;
            }

            return true;
        });
    }
}

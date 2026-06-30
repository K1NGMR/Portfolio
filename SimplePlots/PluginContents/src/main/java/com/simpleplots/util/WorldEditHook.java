package com.simpleplots.util;

import com.simpleplots.SimplePlots;
import com.simpleplots.api.Plot;
import com.simpleplots.api.PlotGeometry;
import com.simpleplots.api.PlotId;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.WorldEditException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * WorldEdit / FAWE Hook to restrict EditSession operations within plot boundaries.
 */
public class WorldEditHook {
    private final SimplePlots plugin;

    public WorldEditHook(SimplePlots plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null) {
            WorldEdit.getInstance().getEventBus().register(this);
            plugin.getLogger().info("Successfully hooked into WorldEdit/FAWE EditSessionEvent.");
        }
    }

    @Subscribe
    public void onEditSession(EditSessionEvent event) {
        if (event.getActor() == null || !event.getActor().isPlayer()) {
            return;
        }

        UUID playerUuid = event.getActor().getUniqueId();
        String worldName = event.getWorld().getName();

        // Wrap the extent to check block modifications
        event.setExtent(new PlotWEExtent(event.getExtent(), worldName, playerUuid, plugin));
    }

    private static class PlotWEExtent extends AbstractDelegateExtent {
        private final String worldName;
        private final UUID playerUuid;
        private final SimplePlots plugin;

        public PlotWEExtent(Extent delegate, String worldName, UUID playerUuid, SimplePlots plugin) {
            super(delegate);
            this.worldName = worldName;
            this.playerUuid = playerUuid;
            this.plugin = plugin;
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) throws WorldEditException {
            Player player = Bukkit.getPlayer(playerUuid);
            
            // Check if WorldEdit is toggled off for the player
            if (player != null && player.hasMetadata("sp_we_disabled")) {
                return false;
            }

            // Check bypass permissions
            if (player != null && (player.hasPermission("plots.admin") || player.hasPermission("plots.admin.bypass") || player.hasPermission("plots.admin.bypass.worldedit"))) {
                return super.setBlock(position, block);
            }

            // Check if the world is a plot world
            if (plugin.getWorldConfig(worldName) == null) {
                return super.setBlock(position, block);
            }

            // Check if block is road
            if (PlotGeometry.isRoad(worldName, position.getX(), position.getZ())) {
                return false;
            }

            // Check plot ownership and membership
            PlotId plotId = PlotGeometry.getPlotId(worldName, position.getX(), position.getZ());
            if (plotId != null) {
                Plot plot = plugin.getPlotAPI().getPlot(worldName, plotId);
                if (plot == null) {
                    return false; // Unclaimed plot cell
                }

                // Verify permissions
                boolean allowed = false;
                if (plot.getOwner().equals(playerUuid)) {
                    allowed = true;
                } else if (plot.isTrusted(playerUuid)) {
                    allowed = true;
                } else if (plot.isAdded(playerUuid)) {
                    Player owner = Bukkit.getPlayer(plot.getOwner());
                    if (owner != null && owner.isOnline()) {
                        allowed = true;
                    }
                }

                if (!allowed) {
                    return false;
                }
            }

            return super.setBlock(position, block);
        }
    }
}

package com.simpleplots.commands;

import com.simpleplots.SimplePlots;
import com.simpleplots.api.Plot;
import com.simpleplots.api.PlotBackup;
import com.simpleplots.api.PlotGeometry;
import com.simpleplots.api.PlotId;
import com.simpleplots.generator.PlotWorldConfig;
import com.simpleplots.util.DownloadHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main command executor for /plot.
 */
public class PlotCommand implements CommandExecutor {
    private final SimplePlots plugin;
    private final Map<UUID, PendingAction> confirmations = new ConcurrentHashMap<>();

    private enum ActionType {
        CLEAR, DELETE, MERGE, UNLINK
    }

    private static class PendingAction {
        final ActionType type;
        final PlotId plotId;
        final String world;
        final long expires;
        final Map<String, Object> metadata = new HashMap<>();

        PendingAction(ActionType type, String world, PlotId plotId, long timeoutMs) {
            this.type = type;
            this.world = world;
            this.plotId = plotId;
            this.expires = System.currentTimeMillis() + timeoutMs;
        }
    }

    public PlotCommand(SimplePlots plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by players.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            com.simpleplots.gui.PlotGUI.openMainMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (!plugin.hasPermissionForSubcommand(player, sub)) {
            com.simpleplots.util.Messages.send(player, "commands.no-permission");
            return true;
        }
        switch (sub) {
            case "help":
                sendHelp(player);
                break;
            case "gui":
            case "menu":
                com.simpleplots.gui.PlotGUI.openMainMenu(player);
                break;
            case "claim":
                handleClaim(player);
                break;
            case "auto":
                handleAuto(player);
                break;
            case "set":
                handleSet(player, args);
                break;
            case "add":
                handleMemberAdd(player, args, "added");
                break;
            case "trust":
                handleMemberAdd(player, args, "trusted");
                break;
            case "deny":
            case "block":
                handleMemberAdd(player, args, "denied");
                break;
            case "remove":
                handleMemberRemove(player, args, "added");
                break;
            case "untrust":
                handleMemberRemove(player, args, "trusted");
                break;
            case "undeny":
            case "unblock":
                handleMemberRemove(player, args, "denied");
                break;
            case "flag":
                handleFlag(player, args);
                break;
            case "biome":
                handleBiome(player, args);
                break;
            case "merge":
                handleMerge(player, args);
                break;
            case "unlink":
            case "unmerge":
                handleUnlink(player);
                break;
            case "clear":
                handleClear(player);
                break;
            case "delete":
            case "unclaim":
                handleDelete(player);
                break;
            case "backup":
                handleBackup(player, args);
                break;
            case "download":
                handleDownload(player);
                break;
            case "delete-download":
                handleDeleteDownload(player, args);
                break;
            case "chat":
                handleChatToggle(player);
                break;
            case "toggle":
                handleToggle(player, args);
                break;
            case "setup":
                handleSetup(player, args);
                break;
            case "worldtp":
                handleWorldTp(player, args);
                break;
            case "condense":
                handleCondense(player, args);
                break;
            case "trim":
                handleTrim(player);
                break;
            case "database":
                handleDatabase(player, args);
                break;
            case "transfer":
                handleTransfer(player, args);
                break;
            case "visit":
                handleVisit(player, args);
                break;
            case "home":
                handleHome(player, args);
                break;
            case "near":
                handleNear(player);
                break;
            case "stats":
                handleStats(player);
                break;
            case "admin":
                handleAdminSuite(player, args);
                break;
            case "list":
                handleList(player, args);
                break;
            case "setowner":
                handleSetOwnerForcibly(player, args);
                break;
            case "reload":
                handleReload(player);
                break;
            case "massunclaim":
                handleMassUnclaim(player, args);
                break;
            case "rate":
                handleRate(player, args);
                break;
            case "comment":
            case "comments":
                handleComments(player, args);
                break;
            case "copy":
                handleCopy(player);
                break;
            case "paste":
                handlePaste(player);
                break;
            case "middle":
                handleMiddle(player);
                break;
            case "joinsupportdc":
                handleJoinSupportDc(player);
                break;
            case "chest":
                handlePlotChest(player);
                break;
            case "confirm":
                handleConfirm(player);
                break;
            case "info":
                handleInfo(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Type /plot for help.");
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        com.simpleplots.util.Messages.send(player, "commands.help-header");
        com.simpleplots.util.Messages.send(player, "commands.help-claim");
        com.simpleplots.util.Messages.send(player, "commands.help-auto");
        com.simpleplots.util.Messages.send(player, "commands.help-info");
        com.simpleplots.util.Messages.send(player, "commands.help-add");
        com.simpleplots.util.Messages.send(player, "commands.help-remove");
        com.simpleplots.util.Messages.send(player, "commands.help-flag");
        com.simpleplots.util.Messages.send(player, "commands.help-biome");
        com.simpleplots.util.Messages.send(player, "commands.help-merge");
        com.simpleplots.util.Messages.send(player, "commands.help-unlink");
        com.simpleplots.util.Messages.send(player, "commands.help-clear");
        com.simpleplots.util.Messages.send(player, "commands.help-delete");
        com.simpleplots.util.Messages.send(player, "commands.help-chat");
        com.simpleplots.util.Messages.send(player, "commands.help-backup");
        com.simpleplots.util.Messages.send(player, "commands.help-download");
        com.simpleplots.util.Messages.send(player, "commands.help-transfer");
        com.simpleplots.util.Messages.send(player, "commands.help-visit");
        com.simpleplots.util.Messages.send(player, "commands.help-home");
        com.simpleplots.util.Messages.send(player, "commands.help-sethome");
        com.simpleplots.util.Messages.send(player, "commands.help-near");
        com.simpleplots.util.Messages.send(player, "commands.help-stats");
        com.simpleplots.util.Messages.send(player, "commands.help-rate");
        com.simpleplots.util.Messages.send(player, "commands.help-comments");
        com.simpleplots.util.Messages.send(player, "commands.help-copy");
        com.simpleplots.util.Messages.send(player, "commands.help-paste");
        com.simpleplots.util.Messages.send(player, "commands.help-middle");
        com.simpleplots.util.Messages.send(player, "commands.help-block");
        com.simpleplots.util.Messages.send(player, "commands.help-unblock");
        com.simpleplots.util.Messages.send(player, "commands.help-joinsupportdc");
        com.simpleplots.util.Messages.send(player, "commands.help-chest");

        if (player.hasPermission("plots.admin")) {
            com.simpleplots.util.Messages.send(player, "commands.help-admin-header");
            com.simpleplots.util.Messages.send(player, "commands.help-setup");
            com.simpleplots.util.Messages.send(player, "commands.help-condense");
            com.simpleplots.util.Messages.send(player, "commands.help-trim");
            com.simpleplots.util.Messages.send(player, "commands.help-database");
        }
    }

    private void handleClaim(Player player) {
        if (!checkPermission(player, "plots.claim")) return;

        Location loc = player.getLocation();
        if (plugin.getWorldConfig(loc.getWorld().getName()) == null) {
            com.simpleplots.util.Messages.send(player, "commands.not-in-plot-world");
            return;
        }

        if (PlotGeometry.isRoad(loc)) {
            com.simpleplots.util.Messages.send(player, "commands.not-standing-in-plot");
            return;
        }

        PlotId plotId = PlotGeometry.getPlotId(loc);
        if (plotId == null) return;

        Plot existing = plugin.getPlotAPI().getPlot(loc.getWorld().getName(), plotId);
        if (existing != null && existing.isClaimed()) {
            com.simpleplots.util.Messages.send(player, "commands.already-claimed", "{owner}", plugin.getUuidCache().getName(existing.getOwner()));
            return;
        }

        // Check claim limit
        if (!hasClaimLimit(player)) {
            com.simpleplots.util.Messages.send(player, "commands.too-many-plots", "{limit}", String.valueOf(getClaimLimit(player)));
            return;
        }

        PlotWorldConfig worldConfig = plugin.getWorldConfig(loc.getWorld().getName());
        String worldName = loc.getWorld().getName();
        Set<PlotId> cluster = getMergedCluster(worldName, plotId);

        claimPlotCluster(worldName, plotId, player.getUniqueId()).thenRun(() -> {
            com.simpleplots.util.Messages.send(player, "commands.plot-claimed", "{id}", plotId.toString());
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (PlotId pid : cluster) {
                    PlotGeometry.updatePlotBorders(worldName, pid);
                }
            });

            // Schematic-on-claim
            if (worldConfig.isSchematicOnClaim() && plugin.getSchematicHandler() != null) {
                File schemFile = new File(plugin.getDataFolder(), "schematics/" + worldConfig.getSchematicName());
                if (schemFile.exists()) {
                    for (PlotId pid : cluster) {
                        plugin.getSchematicHandler().pastePlotSchematic(worldName, pid, schemFile);
                    }
                }
            }
        });
    }

    private void handleAuto(Player player) {
        if (!checkPermission(player, "plots.auto")) return;

        Location loc = player.getLocation();
        if (plugin.getWorldConfig(loc.getWorld().getName()) == null) {
            com.simpleplots.util.Messages.send(player, "commands.not-in-plot-world");
            return;
        }

        if (!hasClaimLimit(player)) {
            com.simpleplots.util.Messages.send(player, "commands.too-many-plots", "{limit}", String.valueOf(getClaimLimit(player)));
            return;
        }

        // Find nearest unclaimed plot radiating outwards from 0,0
        String worldName = loc.getWorld().getName();
        PlotId unclaimed = findNearestUnclaimedPlot(worldName);
        if (unclaimed == null) {
            com.simpleplots.util.Messages.send(player, "commands.no-plots-available");
            return;
        }

        PlotWorldConfig worldConfig = plugin.getWorldConfig(worldName);
        Set<PlotId> cluster = getMergedCluster(worldName, unclaimed);

        claimPlotCluster(worldName, unclaimed, player.getUniqueId()).thenRun(() -> {
            // Teleport player
            int[] bounds = PlotGeometry.getPlotBounds(worldName, unclaimed);
            double targetX = bounds[0] + (bounds[2] - bounds[0]) / 2.0;
            double targetZ = bounds[1] + (bounds[3] - bounds[1]) / 2.0;
            Location tpLoc = new Location(loc.getWorld(), targetX, worldConfig.getFloorHeight() + 1, targetZ);
            
            // Teleport on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.teleport(tpLoc);
                for (PlotId pid : cluster) {
                    PlotGeometry.updatePlotBorders(worldName, pid);
                }
                com.simpleplots.util.Messages.send(player, "commands.plot-claimed", "{id}", unclaimed.toString());
            });

            // Schematic-on-claim
            if (worldConfig.isSchematicOnClaim() && plugin.getSchematicHandler() != null) {
                File schemFile = new File(plugin.getDataFolder(), "schematics/" + worldConfig.getSchematicName());
                if (schemFile.exists()) {
                    for (PlotId pid : cluster) {
                        plugin.getSchematicHandler().pastePlotSchematic(worldName, pid, schemFile);
                    }
                }
            }
        });
    }

    private PlotId findNearestUnclaimedPlot(String worldName) {
        // Spiral coordinate search from (0,0)
        int x = 0;
        int z = 0;
        int dx = 0;
        int dz = -1;
        int maxSegment = 1;
        int segmentCount = 0;
        int segmentPasses = 0;

        for (int i = 0; i < 5000; i++) { // Limit search iterations to avoid crash
            PlotId pid = new PlotId(x, z);
            Plot p = plugin.getPlotAPI().getPlot(worldName, pid);
            if (p == null || !p.isClaimed()) {
                return pid;
            }

            if (segmentCount == maxSegment) {
                segmentCount = 0;
                int temp = dx;
                dx = -dz;
                dz = temp;
                segmentPasses++;
                if (segmentPasses == 2) {
                    segmentPasses = 0;
                    maxSegment++;
                }
            }
            x += dx;
            z += dz;
            segmentCount++;
        }
        return null;
    }

    private Set<PlotId> getMergedCluster(String worldName, PlotId startId) {
        Set<PlotId> cluster = new HashSet<>();
        Queue<PlotId> queue = new LinkedList<>();
        queue.add(startId);
        cluster.add(startId);

        while (!queue.isEmpty()) {
            PlotId current = queue.poll();
            Plot currentPlot = plugin.getPlotAPI().getPlot(worldName, current);
            if (currentPlot == null) continue;

            if (currentPlot.isMergedE()) {
                PlotId next = new PlotId(current.getX() + 1, current.getZ());
                if (!cluster.contains(next)) {
                    cluster.add(next);
                    queue.add(next);
                }
            }
            if (currentPlot.isMergedW()) {
                PlotId next = new PlotId(current.getX() - 1, current.getZ());
                if (!cluster.contains(next)) {
                    cluster.add(next);
                    queue.add(next);
                }
            }
            if (currentPlot.isMergedN()) {
                PlotId next = new PlotId(current.getX(), current.getZ() - 1);
                if (!cluster.contains(next)) {
                    cluster.add(next);
                    queue.add(next);
                }
            }
            if (currentPlot.isMergedS()) {
                PlotId next = new PlotId(current.getX(), current.getZ() + 1);
                if (!cluster.contains(next)) {
                    cluster.add(next);
                    queue.add(next);
                }
            }
        }
        return cluster;
    }

    private CompletableFuture<Void> claimPlotCluster(String worldName, PlotId startId, UUID owner) {
        Set<PlotId> cluster = getMergedCluster(worldName, startId);
        
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        for (PlotId pid : cluster) {
            Plot p = plugin.getPlotAPI().getPlot(worldName, pid);
            if (p == null) {
                p = new Plot(pid, worldName, owner);
                plugin.getPlotAPI().addPlotToCache(p);
            } else {
                p.setOwner(owner);
            }
            final Plot finalP = p;
            future = future.thenCompose(v -> plugin.getDatabaseManager().savePlotOwner(finalP));
        }
        return future;
    }

    private int getPlayerClusterCount(UUID uuid) {
        Set<PlotId> owned = new HashSet<>();
        String worldName = null;
        for (Plot plot : plugin.getPlotAPI().getAllPlots()) {
            if (plot.getOwner() != null && plot.getOwner().equals(uuid)) {
                owned.add(plot.getId());
                if (worldName == null) worldName = plot.getWorld();
            }
        }
        if (owned.isEmpty()) return 0;
        
        int clusterCount = 0;
        Set<PlotId> visited = new HashSet<>();
        for (PlotId pid : owned) {
            if (!visited.contains(pid)) {
                clusterCount++;
                Queue<PlotId> queue = new LinkedList<>();
                queue.add(pid);
                visited.add(pid);
                while (!queue.isEmpty()) {
                    PlotId current = queue.poll();
                    Plot currentPlot = plugin.getPlotAPI().getPlot(worldName, current);
                    if (currentPlot == null) continue;
                    
                    if (currentPlot.isMergedE()) {
                        PlotId next = new PlotId(current.getX() + 1, current.getZ());
                        if (owned.contains(next) && !visited.contains(next)) {
                            visited.add(next);
                            queue.add(next);
                        }
                    }
                    if (currentPlot.isMergedW()) {
                        PlotId next = new PlotId(current.getX() - 1, current.getZ());
                        if (owned.contains(next) && !visited.contains(next)) {
                            visited.add(next);
                            queue.add(next);
                        }
                    }
                    if (currentPlot.isMergedN()) {
                        PlotId next = new PlotId(current.getX(), current.getZ() - 1);
                        if (owned.contains(next) && !visited.contains(next)) {
                            visited.add(next);
                            queue.add(next);
                        }
                    }
                    if (currentPlot.isMergedS()) {
                        PlotId next = new PlotId(current.getX(), current.getZ() + 1);
                        if (owned.contains(next) && !visited.contains(next)) {
                            visited.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return clusterCount;
    }

    private void clearPlotBlocks(World world, PlotWorldConfig cfg, PlotId plotId) {
        int[] bounds = PlotGeometry.getPlotBounds(world.getName(), plotId);
        int minX = bounds[0];
        int minZ = bounds[1];
        int maxX = bounds[2];
        int maxZ = bounds[3];

        Material floorMat = cfg.getFloorBlock();
        Material fillerMat = cfg.getFillerBlock();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                // Biome
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y += 4) { // Biomes are 4x4x4 in newer versions, setting per block or step 4 is fast and correct
                    world.setBiome(x, y, z, org.bukkit.block.Biome.PLAINS);
                }
                // Bedrock
                world.setType(x, 0, z, cfg.getBedBlock());
                // Filler
                for (int y = 1; y < cfg.getFloorHeight(); y++) {
                    world.setType(x, y, z, fillerMat);
                }
                // Floor
                world.setType(x, cfg.getFloorHeight(), z, floorMat);
                // Air above
                for (int y = cfg.getFloorHeight() + 1; y < world.getMaxHeight(); y++) {
                    world.setType(x, y, z, Material.AIR);
                }
            }
        }
    }

    private void clearRoadBlocksBetween(World world, PlotWorldConfig cfg, PlotId currentId, PlotId adjacentId, String dir) {
        int[] currentBounds = PlotGeometry.getPlotBounds(world.getName(), currentId);
        int[] adjacentBounds = PlotGeometry.getPlotBounds(world.getName(), adjacentId);

        int minX, minZ, maxX, maxZ;
        if (dir.equals("EAST") || dir.equals("WEST")) {
            minX = Math.min(currentBounds[2], adjacentBounds[2]) + 1;
            maxX = Math.max(currentBounds[0], adjacentBounds[0]) - 1;
            minZ = Math.min(currentBounds[1], adjacentBounds[1]);
            maxZ = Math.max(currentBounds[3], adjacentBounds[3]);
        } else {
            minX = Math.min(currentBounds[0], adjacentBounds[0]);
            maxX = Math.max(currentBounds[2], adjacentBounds[2]);
            minZ = Math.min(currentBounds[3], adjacentBounds[3]) + 1;
            maxZ = Math.max(currentBounds[1], adjacentBounds[1]) - 1;
        }

        Material floorMat = cfg.getFloorBlock();
        Material fillerMat = cfg.getFillerBlock();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setType(x, 0, z, cfg.getBedBlock());
                for (int y = 1; y < cfg.getFloorHeight(); y++) {
                    world.setType(x, y, z, fillerMat);
                }
                world.setType(x, cfg.getFloorHeight(), z, floorMat);
                for (int y = cfg.getFloorHeight() + 1; y < world.getMaxHeight(); y++) {
                    world.setType(x, y, z, Material.AIR);
                }
            }
        }
    }

    private int getClaimLimit(Player player) {
        if (player.hasPermission("plots.admin") || player.hasPermission("plots.plot.infinite")) {
            return -1;
        }
        int limitFromConfig = plugin.getConfig().getInt("max-plots-per-player", 3);
        int max = -1;
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("plots.plot." + i)) {
                max = i;
                break;
            }
        }
        if (max == -1) {
            return limitFromConfig;
        }
        return max;
    }

    private boolean hasClaimLimit(Player player) {
        int limit = getClaimLimit(player);
        if (limit == -1) return true;
        return plugin.getPlotAPI().getPlayerPlots(player.getUniqueId()).size() < limit;
    }

    private void handleSet(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /plot set owner <player> | /plot set home");
            return;
        }
        if (args[1].equalsIgnoreCase("home")) {
            handleSetHome(player);
            return;
        }
        if (args[1].equalsIgnoreCase("owner")) {
            handleSetOwner(player, args);
            return;
        }
        player.sendMessage(ChatColor.RED + "Usage: /plot set owner <player> | /plot set home");
    }

    private void handleSetHome(Player player) {
        if (!checkPermission(player, "plots.sethome")) return;

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            com.simpleplots.util.Messages.send(player, "commands.not-standing-in-plot");
            return;
        }

        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            com.simpleplots.util.Messages.send(player, "commands.not-owner");
            return;
        }

        Location loc = player.getLocation();
        plot.setFlag("teleport-x", String.valueOf(loc.getX()));
        plot.setFlag("teleport-y", String.valueOf(loc.getY()));
        plot.setFlag("teleport-z", String.valueOf(loc.getZ()));
        plot.setFlag("teleport-yaw", String.valueOf(loc.getYaw()));
        plot.setFlag("teleport-pitch", String.valueOf(loc.getPitch()));

        plugin.getDatabaseManager().savePlotFlag(plot.getWorld(), plot.getId(), "teleport-x", String.valueOf(loc.getX()));
        plugin.getDatabaseManager().savePlotFlag(plot.getWorld(), plot.getId(), "teleport-y", String.valueOf(loc.getY()));
        plugin.getDatabaseManager().savePlotFlag(plot.getWorld(), plot.getId(), "teleport-z", String.valueOf(loc.getZ()));
        plugin.getDatabaseManager().savePlotFlag(plot.getWorld(), plot.getId(), "teleport-yaw", String.valueOf(loc.getYaw()));
        plugin.getDatabaseManager().savePlotFlag(plot.getWorld(), plot.getId(), "teleport-pitch", String.valueOf(loc.getPitch()));

        player.sendMessage(ChatColor.GREEN + "Custom teleport home location set successfully!");
    }

    private void handleSetOwner(Player player, String[] args) {
        if (!player.hasPermission("plots.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /plot set owner <player>");
            return;
        }

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "You are not standing on a claimed plot.");
            return;
        }

        String targetName = args[2];
        UUID targetUuid = plugin.getUuidCache().getUUID(targetName);
        if (targetUuid == null) {
            player.sendMessage(ChatColor.RED + "Player " + targetName + " not found.");
            return;
        }

        plot.setOwner(targetUuid);
        plugin.getDatabaseManager().savePlotOwner(plot).thenRun(() -> {
            player.sendMessage(ChatColor.GREEN + "Plot owner updated to " + targetName + ".");
        });
    }

    private void handleMemberAdd(Player player, String[] args, String tier) {
        if (tier.equals("added") && !checkPermission(player, "plots.add")) return;
        if (tier.equals("trusted") && !checkPermission(player, "plots.trust")) return;
        if (tier.equals("denied") && !checkPermission(player, "plots.deny")) return;

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /plot " + (tier.equals("added") ? "add" : tier) + " <player>");
            return;
        }

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            com.simpleplots.util.Messages.send(player, "commands.not-standing-in-plot");
            return;
        }

        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            com.simpleplots.util.Messages.send(player, "commands.not-owner");
            return;
        }

        String targetName = args[1];
        if (targetName.equalsIgnoreCase(player.getName())) {
            com.simpleplots.util.Messages.send(player, "commands.cannot-target-self");
            return;
        }

        UUID targetUuid = plugin.getUuidCache().getUUID(targetName);
        if (targetUuid == null) {
            com.simpleplots.util.Messages.send(player, "commands.player-not-found", "{name}", targetName);
            return;
        }

        if (tier.equals("added")) {
            plot.getAdded().add(targetUuid);
        } else if (tier.equals("trusted")) {
            plot.getTrusted().add(targetUuid);
        } else {
            plot.getDenied().add(targetUuid);
        }

        plugin.getDatabaseManager().addPlotMember(plot.getWorld(), plot.getId(), targetUuid, tier).thenRun(() -> {
            if (tier.equals("added")) {
                com.simpleplots.util.Messages.send(player, "commands.added-player", "{player}", targetName);
            } else if (tier.equals("trusted")) {
                com.simpleplots.util.Messages.send(player, "commands.trusted-player", "{player}", targetName);
            } else {
                com.simpleplots.util.Messages.send(player, "commands.denied-player", "{player}", targetName);
            }
        });
    }

    private void handleMemberRemove(Player player, String[] args, String tier) {
        if (tier.equals("added") && !checkPermission(player, "plots.remove")) return;
        if (tier.equals("trusted") && !checkPermission(player, "plots.untrust")) return;
        if (tier.equals("denied") && !checkPermission(player, "plots.undeny")) return;

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /plot " + (tier.equals("added") ? "remove" : "undeny") + " <player>");
            return;
        }

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            com.simpleplots.util.Messages.send(player, "commands.not-standing-in-plot");
            return;
        }

        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            com.simpleplots.util.Messages.send(player, "commands.not-owner");
            return;
        }

        String targetName = args[1];
        UUID targetUuid = plugin.getUuidCache().getUUID(targetName);
        if (targetUuid == null) {
            com.simpleplots.util.Messages.send(player, "commands.player-not-found", "{name}", targetName);
            return;
        }

        boolean removed = false;
        if (tier.equals("added")) {
            removed = plot.getAdded().remove(targetUuid);
        } else if (tier.equals("trusted")) {
            removed = plot.getTrusted().remove(targetUuid);
        } else {
            removed = plot.getDenied().remove(targetUuid);
        }

        if (removed) {
            plugin.getDatabaseManager().removePlotMember(plot.getWorld(), plot.getId(), targetUuid, tier).thenRun(() -> {
                if (tier.equals("denied")) {
                    com.simpleplots.util.Messages.send(player, "commands.undenied-player", "{player}", targetName);
                } else {
                    com.simpleplots.util.Messages.send(player, "commands.removed-player", "{player}", targetName);
                }
            });
        } else {
            player.sendMessage(ChatColor.RED + targetName + " is not in that list.");
        }
    }

    private void handleFlag(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /plot flag set/remove <flag> [value]");
            return;
        }

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "You are not standing on a claimed plot.");
            return;
        }

        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            player.sendMessage(ChatColor.RED + "You do not own this plot.");
            return;
        }

        String action = args[1].toLowerCase();
        String flag = args[2].toLowerCase();

        if (action.equals("set")) {
            if (args.length < 4) {
                player.sendMessage(ChatColor.RED + "Please specify a flag value.");
                return;
            }
            StringBuilder valBuilder = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                valBuilder.append(args[i]).append(" ");
            }
            String value = valBuilder.toString().trim();

            plot.setFlag(flag, value);
            plugin.getDatabaseManager().savePlotFlag(plot.getWorld(), plot.getId(), flag, value).thenRun(() -> {
                player.sendMessage(ChatColor.GREEN + "Set flag " + flag + " to: " + value);
            });
        } else if (action.equals("remove")) {
            plot.removeFlag(flag);
            plugin.getDatabaseManager().removePlotFlag(plot.getWorld(), plot.getId(), flag).thenRun(() -> {
                player.sendMessage(ChatColor.GREEN + "Removed flag " + flag + ".");
            });
        }
    }

    private void handleBiome(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /plot biome <biomeName>");
            return;
        }

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "You are not standing on a claimed plot.");
            return;
        }

        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            player.sendMessage(ChatColor.RED + "You do not own this plot.");
            return;
        }

        String biomeStr = args[1].toUpperCase();
        Biome biome;
        try {
            biome = Biome.valueOf(biomeStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Unknown biome name.");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Setting biome to " + biome.name() + " asynchronously...");

        Bukkit.getScheduler().runTask(plugin, () -> {
            int[] bounds = PlotGeometry.getMergedPlotBounds(plot.getWorld(), plot.getId());
            World world = Bukkit.getWorld(plot.getWorld());
            if (world == null) return;

            for (int x = bounds[0]; x <= bounds[2]; x++) {
                for (int z = bounds[1]; z <= bounds[3]; z++) {
                    for (int y = world.getMinHeight(); y < world.getMaxHeight(); y += 4) { // setting biome in 3D grid
                        world.setBiome(x, y, z, biome);
                    }
                }
            }

            // Resend chunks
            int minChunkX = bounds[0] >> 4;
            int maxChunkX = bounds[2] >> 4;
            int minChunkZ = bounds[1] >> 4;
            int maxChunkZ = bounds[3] >> 4;

            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    world.refreshChunk(cx, cz);
                }
            }
            player.sendMessage(ChatColor.GREEN + "Biome successfully updated to " + biome.name() + "!");
        });
    }

    private void handleMerge(Player player, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("accept")) {
            // Accept merge request
            SimplePlots.MergeRequest found = null;
            for (SimplePlots.MergeRequest req : plugin.getPendingMergeRequests()) {
                if (req.receiver.equals(player.getUniqueId())) {
                    found = req;
                    break;
                }
            }
            
            if (found == null) {
                player.sendMessage(ChatColor.RED + "You do not have any pending merge requests.");
                return;
            }
            
            plugin.getPendingMergeRequests().remove(found);
            plugin.saveMergeRequests();
            
            executeSharedMerge(player, found);
            return;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("deny")) {
            // Deny merge request
            SimplePlots.MergeRequest found = null;
            for (SimplePlots.MergeRequest req : plugin.getPendingMergeRequests()) {
                if (req.receiver.equals(player.getUniqueId())) {
                    found = req;
                    break;
                }
            }
            
            if (found == null) {
                player.sendMessage(ChatColor.RED + "You do not have any pending merge requests.");
                return;
            }
            
            plugin.getPendingMergeRequests().remove(found);
            plugin.saveMergeRequests();
            
            player.sendMessage(ChatColor.GREEN + "Merge request denied.");
            Player sender = Bukkit.getPlayer(found.sender);
            if (sender != null && sender.isOnline()) {
                sender.sendMessage(ChatColor.RED + player.getName() + " has denied your merge request.");
            }
            return;
        }

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "You are not standing on a claimed plot.");
            return;
        }

        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            player.sendMessage(ChatColor.RED + "You must own this plot to merge it.");
            return;
        }

        // Determine direction player is facing
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) yaw += 360;
        
        String direction;
        PlotId adjacentId;
        if (yaw >= 315 || yaw < 45) {
            direction = "SOUTH";
            adjacentId = new PlotId(plot.getId().getX(), plot.getId().getZ() + 1);
        } else if (yaw >= 45 && yaw < 135) {
            direction = "WEST";
            adjacentId = new PlotId(plot.getId().getX() - 1, plot.getId().getZ());
        } else if (yaw >= 135 && yaw < 225) {
            direction = "NORTH";
            adjacentId = new PlotId(plot.getId().getX(), plot.getId().getZ() - 1);
        } else {
            direction = "EAST";
            adjacentId = new PlotId(plot.getId().getX() + 1, plot.getId().getZ());
        }

        Plot adjacent = plugin.getPlotAPI().getPlot(plot.getWorld(), adjacentId);
        if (adjacent == null) {
            player.sendMessage(ChatColor.RED + "The adjacent plot in direction " + direction + " is unclaimed.");
            return;
        }

        // If owned by someone else
        if (!adjacent.getOwner().equals(plot.getOwner())) {
            UUID receiver = adjacent.getOwner();
            // Check if there is already an active request
            for (SimplePlots.MergeRequest req : plugin.getPendingMergeRequests()) {
                if (req.sender.equals(player.getUniqueId()) && req.receiver.equals(receiver) &&
                    req.senderPlot.equals(plot.getId()) && req.receiverPlot.equals(adjacentId)) {
                    player.sendMessage(ChatColor.YELLOW + "You have already sent a merge request to the owner of this plot!");
                    return;
                }
            }
            
            SimplePlots.MergeRequest request = new SimplePlots.MergeRequest(
                player.getUniqueId(), receiver, plot.getWorld(), plot.getId(), adjacentId, System.currentTimeMillis()
            );
            plugin.getPendingMergeRequests().add(request);
            plugin.saveMergeRequests();
            
            player.sendMessage(ChatColor.GREEN + "Merge request sent to " + plugin.getUuidCache().getName(receiver) + "!");
            
            Player receiverPlayer = Bukkit.getPlayer(receiver);
            if (receiverPlayer != null && receiverPlayer.isOnline()) {
                receiverPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&a&l[SimplePlots] &e" + player.getName() + " &7wants to merge their plot &a" + plot.getId() + 
                    " &7with your plot &a" + adjacentId + "!"));
                receiverPlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&a&l[SimplePlots] &7Type &e/plot merge accept &aor &c/plot merge deny &7to respond."));
            }
            return;
        }

        PendingAction pa = new PendingAction(ActionType.MERGE, plot.getWorld(), plot.getId(), 30000);
        pa.metadata.put("adjacent", adjacentId);
        pa.metadata.put("direction", direction);

        confirmations.put(player.getUniqueId(), pa);
        player.sendMessage(ChatColor.YELLOW + "Are you sure you want to merge with your plot to the " + direction + "?");
        player.sendMessage(ChatColor.RED + "WARNING: All blocks/builds in the road area being merged will be removed. It is highly recommended to keep any special things in your inventory first!");
        player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GOLD + "/plot confirm" + ChatColor.YELLOW + " to proceed (expires in 30s).");
    }

    private void executeMerge(Player player, PendingAction action) {
        PlotId currentId = action.plotId;
        PlotId adjacentId = (PlotId) action.metadata.get("adjacent");
        String dir = (String) action.metadata.get("direction");

        Plot plot = plugin.getPlotAPI().getPlot(action.world, currentId);
        Plot adjacent = plugin.getPlotAPI().getPlot(action.world, adjacentId);

        if (plot == null || adjacent == null) return;

        // Set merge states
        if (dir.equals("EAST")) {
            plot.setMergedE(true);
            adjacent.setMergedW(true);
        } else if (dir.equals("WEST")) {
            plot.setMergedW(true);
            adjacent.setMergedE(true);
        } else if (dir.equals("SOUTH")) {
            plot.setMergedS(true);
            adjacent.setMergedN(true);
        } else {
            plot.setMergedN(true);
            adjacent.setMergedS(true);
        }

        // Auto-merge any adjacent plots within the same cluster to prevent left-over roads (e.g. merging 4th plot into an L-shape)
        java.util.Set<PlotId> cluster = plugin.getPlotAPI().getMergedCluster(action.world, currentId);
        for (PlotId pid : cluster) {
            Plot p = plugin.getPlotAPI().getPlot(action.world, pid);
            if (p == null) continue;

            // Check East neighbor
            PlotId eastId = new PlotId(pid.getX() + 1, pid.getZ());
            if (cluster.contains(eastId)) {
                Plot eastPlot = plugin.getPlotAPI().getPlot(action.world, eastId);
                if (eastPlot != null) {
                    p.setMergedE(true);
                    eastPlot.setMergedW(true);
                }
            }

            // Check South neighbor
            PlotId southId = new PlotId(pid.getX(), pid.getZ() + 1);
            if (cluster.contains(southId)) {
                Plot southPlot = plugin.getPlotAPI().getPlot(action.world, southId);
                if (southPlot != null) {
                    p.setMergedS(true);
                    southPlot.setMergedN(true);
                }
            }
        }

        // Save all updated plots in the cluster to the database
        for (PlotId pid : cluster) {
            Plot p = plugin.getPlotAPI().getPlot(action.world, pid);
            if (p != null) {
                plugin.getDatabaseManager().savePlotOwner(p);
            }
        }

        player.sendMessage(ChatColor.GREEN + "Plots merged successfully. Clearing road async...");

        // Clear road blocks within the entire cluster bounds on the main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlotWorldConfig cfg = plugin.getWorldConfig(action.world);
            if (cfg == null) return;
            int totalSize = cfg.getTotalSize();
            int roadWidth = cfg.getRoadWidth();
            int halfRoad = roadWidth / 2;
            int floorHeight = cfg.getFloorHeight();

            // Calculate bounding box of the ENTIRE merged cluster
            int minPlotX = Integer.MAX_VALUE, minPlotZ = Integer.MAX_VALUE;
            int maxPlotX = Integer.MIN_VALUE, maxPlotZ = Integer.MIN_VALUE;
            for (PlotId pid : cluster) {
                if (pid.getX() < minPlotX) minPlotX = pid.getX();
                if (pid.getZ() < minPlotZ) minPlotZ = pid.getZ();
                if (pid.getX() > maxPlotX) maxPlotX = pid.getX();
                if (pid.getZ() > maxPlotZ) maxPlotZ = pid.getZ();
            }

            int[] minBounds = PlotGeometry.getPlotBounds(action.world, new PlotId(minPlotX, minPlotZ));
            int[] maxBounds = PlotGeometry.getPlotBounds(action.world, new PlotId(maxPlotX, maxPlotZ));
            if (minBounds == null || maxBounds == null) return;

            int minX = minBounds[0] - roadWidth;
            int maxX = maxBounds[2] + roadWidth;
            int minZ = minBounds[1] - roadWidth;
            int maxZ = maxBounds[3] + roadWidth;

            World world = Bukkit.getWorld(action.world);
            if (world == null) return;

            Material floor = cfg.getFloorBlock();
            Material filler = cfg.getFillerBlock();

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int shiftedX = x + halfRoad;
                    int shiftedZ = z + halfRoad;
                    int remX = Math.floorMod(shiftedX, totalSize);
                    int remZ = Math.floorMod(shiftedZ, totalSize);
                    boolean originalRoad = (remX < roadWidth) || (remZ < roadWidth);

                    if (originalRoad && !PlotGeometry.isRoad(action.world, x, z)) {
                        // Scan for containers to transfer items to Plot Chest
                        List<ItemStack> itemsToSave = new ArrayList<>();
                        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                            Block block = world.getBlockAt(x, y, z);
                            if (block.getType() != Material.AIR) {
                                org.bukkit.block.BlockState state = block.getState();
                                if (state instanceof org.bukkit.block.Container) {
                                    org.bukkit.block.Container container = (org.bukkit.block.Container) state;
                                    ItemStack[] contents = container.getInventory().getContents();
                                    for (ItemStack item : contents) {
                                        if (item != null && item.getType() != Material.AIR) {
                                            itemsToSave.add(item.clone());
                                        }
                                    }
                                    container.getInventory().clear();
                                }
                            }
                        }
                        if (!itemsToSave.isEmpty()) {
                            plugin.saveToPlotChest(plot.getOwner(), itemsToSave);
                        }

                        // Bedrock
                        world.setType(x, 0, z, cfg.getBedBlock());
                        
                        // Filler
                        for (int y = 1; y < floorHeight; y++) {
                            world.setType(x, y, z, filler);
                        }
                        
                        // Surface (replace only if it's default road/border/wall/air/dirt)
                        Material typeAtFloor = world.getBlockAt(x, floorHeight, z).getType();
                        if (typeAtFloor == cfg.getRoadBlock() || typeAtFloor == cfg.getBorderBlock() ||
                            typeAtFloor == cfg.getWallBlockUnclaimed() || typeAtFloor == cfg.getWallBlockClaimed() ||
                            typeAtFloor == Material.AIR || typeAtFloor == cfg.getFillerBlock()) {
                            
                            world.setType(x, floorHeight, z, floor);
                        }
                        
                        // Clear walls/border blocks above the floor (replace only if they are default wall blocks)
                        for (int y = floorHeight + 1; y < world.getMaxHeight(); y++) {
                            Block b = world.getBlockAt(x, y, z);
                            Material type = b.getType();
                            if (type == cfg.getWallBlockUnclaimed() || type == cfg.getWallBlockClaimed()) {
                                world.setType(x, y, z, Material.AIR);
                            }
                        }
                    }
                }
            }

            // Also clear border wall tiles for the entire cluster that are no longer borders
            for (PlotId pid : cluster) {
                PlotGeometry.updatePlotBorders(action.world, pid);
            }
            player.sendMessage(ChatColor.GREEN + "Road removal completed!");
        });
    }

    private void handleUnlink(Player player) {
        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "You are not standing on a claimed plot.");
            return;
        }

        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            player.sendMessage(ChatColor.RED + "You must own this plot to unlink.");
            return;
        }

        PendingAction pa = new PendingAction(ActionType.UNLINK, plot.getWorld(), plot.getId(), 30000);
        confirmations.put(player.getUniqueId(), pa);

        player.sendMessage(ChatColor.YELLOW + "Are you sure you want to unlink this plot from its adjacent merged plots?");
        player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GOLD + "/plot confirm" + ChatColor.YELLOW + " to proceed (expires in 30s).");
    }

    private void executeUnlink(Player player, PendingAction action) {
        Plot plot = plugin.getPlotAPI().getPlot(action.world, action.plotId);
        if (plot == null) return;

        // Reset merges
        plot.setMergedN(false);
        plot.setMergedS(false);
        plot.setMergedE(false);
        plot.setMergedW(false);

        plugin.getDatabaseManager().savePlotOwner(plot);

        // Notify and refill road blocks
        player.sendMessage(ChatColor.GREEN + "Plot unlinked! Refilling roads...");

        // Unlink adjacent plots merges that link to this one
        for (PlotId adj : new PlotId[]{
                new PlotId(plot.getId().getX() + 1, plot.getId().getZ()),
                new PlotId(plot.getId().getX() - 1, plot.getId().getZ()),
                new PlotId(plot.getId().getX(), plot.getId().getZ() + 1),
                new PlotId(plot.getId().getX(), plot.getId().getZ() - 1)
        }) {
            Plot adjPlot = plugin.getPlotAPI().getPlot(plot.getWorld(), adj);
            if (adjPlot != null) {
                if (adj.getX() > plot.getId().getX()) adjPlot.setMergedW(false);
                if (adj.getX() < plot.getId().getX()) adjPlot.setMergedE(false);
                if (adj.getZ() > plot.getId().getZ()) adjPlot.setMergedN(false);
                if (adj.getZ() < plot.getId().getZ()) adjPlot.setMergedS(false);
                plugin.getDatabaseManager().savePlotOwner(adjPlot);
            }
        }

        // Refill roads on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlotWorldConfig cfg = plugin.getWorldConfig(action.world);
            World world = Bukkit.getWorld(action.world);
            if (world == null || cfg == null) return;

            // Simple clearing/resetting of the surrounding blocks based on PlotChunkGenerator rules
            int totalSize = cfg.getTotalSize();
            int roadWidth = cfg.getRoadWidth();
            int halfRoad = roadWidth / 2;
            int floorHeight = cfg.getFloorHeight();

            // Run regenerator across chunk regions surrounding plot
            int[] bounds = PlotGeometry.getPlotBounds(action.world, action.plotId);
            int minX = bounds[0] - roadWidth;
            int maxX = bounds[2] + roadWidth;
            int minZ = bounds[1] - roadWidth;
            int maxZ = bounds[3] + roadWidth;

            Material roadMat = cfg.getRoadBlock();
            Material borderMat = cfg.getBorderBlock();
            Material wallMat = cfg.getWallBlockUnclaimed();

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (PlotGeometry.isRoad(action.world, x, z)) {
                        int sx = x + halfRoad;
                        int sz = z + halfRoad;
                        int rx = Math.floorMod(sx, totalSize);
                        int rz = Math.floorMod(sz, totalSize);

                        boolean isBorder = isBorder(rx, rz, roadWidth, totalSize);

                        if (isBorder) {
                            world.setType(x, floorHeight, z, borderMat);
                            for (int y = floorHeight + 1; y < world.getMaxHeight(); y++) {
                                if (y == floorHeight + 1) {
                                    world.setType(x, y, z, wallMat);
                                } else {
                                    world.setType(x, y, z, Material.AIR);
                                }
                            }
                        } else {
                            world.setType(x, floorHeight, z, roadMat);
                            for (int y = floorHeight + 1; y < world.getMaxHeight(); y++) {
                                world.setType(x, y, z, Material.AIR);
                            }
                        }
                    }
                }
            }
            PlotGeometry.updatePlotBorders(action.world, action.plotId);
            PlotGeometry.updatePlotBorders(action.world, new PlotId(action.plotId.getX() + 1, action.plotId.getZ()));
            PlotGeometry.updatePlotBorders(action.world, new PlotId(action.plotId.getX() - 1, action.plotId.getZ()));
            PlotGeometry.updatePlotBorders(action.world, new PlotId(action.plotId.getX(), action.plotId.getZ() + 1));
            PlotGeometry.updatePlotBorders(action.world, new PlotId(action.plotId.getX(), action.plotId.getZ() - 1));
            player.sendMessage(ChatColor.GREEN + "Roads restored!");
        });
    }

    private void handleClear(Player player) {
        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "You are not standing on a claimed plot.");
            return;
        }

        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            player.sendMessage(ChatColor.RED + "You must own this plot to clear it.");
            return;
        }

        PendingAction pa = new PendingAction(ActionType.CLEAR, plot.getWorld(), plot.getId(), 30000);
        confirmations.put(player.getUniqueId(), pa);

        player.sendMessage(ChatColor.YELLOW + "Are you sure you want to clear this plot? All block changes will be reset!");
        player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GOLD + "/plot confirm" + ChatColor.YELLOW + " to proceed (expires in 30s).");
    }

    private void executeClear(Player player, PendingAction action) {
        Plot plot = plugin.getPlotAPI().getPlot(action.world, action.plotId);
        if (plot == null) return;

        player.sendMessage(ChatColor.YELLOW + "Clearing plot blocks asynchronously...");

        Set<PlotId> cluster = getMergedCluster(action.world, action.plotId);

        // Clear plot blocks on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlotWorldConfig cfg = plugin.getWorldConfig(action.world);
            World world = Bukkit.getWorld(action.world);
            if (world == null || cfg == null) return;

            for (PlotId pid : cluster) {
                clearPlotBlocks(world, cfg, pid);
            }

            // Clear the roads between adjacent plots in the cluster
            for (PlotId p1 : cluster) {
                Plot plot1 = plugin.getPlotAPI().getPlot(action.world, p1);
                if (plot1 == null) continue;
                if (plot1.isMergedE()) {
                    PlotId p2 = new PlotId(p1.getX() + 1, p1.getZ());
                    if (cluster.contains(p2)) {
                        clearRoadBlocksBetween(world, cfg, p1, p2, "EAST");
                    }
                }
                if (plot1.isMergedS()) {
                    PlotId p2 = new PlotId(p1.getX(), p1.getZ() + 1);
                    if (cluster.contains(p2)) {
                        clearRoadBlocksBetween(world, cfg, p1, p2, "SOUTH");
                    }
                }
            }

            // Update borders
            for (PlotId pid : cluster) {
                PlotGeometry.updatePlotBorders(action.world, pid);
            }
            player.sendMessage(ChatColor.GREEN + "Plot successfully cleared!");
        });
    }

    private void handleDelete(Player player) {
        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "You are not standing on a claimed plot.");
            return;
        }

        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            player.sendMessage(ChatColor.RED + "You must own this plot to delete it.");
            return;
        }

        PendingAction pa = new PendingAction(ActionType.DELETE, plot.getWorld(), plot.getId(), 30000);
        confirmations.put(player.getUniqueId(), pa);

        player.sendMessage(ChatColor.YELLOW + "Are you sure you want to delete this plot? All blocks will be cleared and ownership removed!");
        player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GOLD + "/plot confirm" + ChatColor.YELLOW + " to proceed (expires in 30s).");
    }

    private void executeDelete(Player player, PendingAction action) {
        Plot plot = plugin.getPlotAPI().getPlot(action.world, action.plotId);
        if (plot == null) return;

        player.sendMessage(ChatColor.YELLOW + "Deleting plot ownership and clearing blocks...");

        plugin.getPlotAPI().unclaimPlotCluster(action.world, action.plotId).thenRun(() -> {
            player.sendMessage(ChatColor.GREEN + "Plot cluster successfully unclaimed and cleared!");
        });
    }


    private void handleBackup(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /plot backup save | list | load <index>");
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            player.sendMessage(ChatColor.RED + "WorldEdit/FAWE is not installed. Backup features are disabled.");
            return;
        }

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "You are not standing on a claimed plot.");
            return;
        }

        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            player.sendMessage(ChatColor.RED + "You do not own this plot.");
            return;
        }

        String sub = args[1].toLowerCase();
        if (sub.equals("save")) {
            if (!player.hasPermission("plots.admin") && plugin.getConfig().getBoolean("copy-paste.prevent-container-dupes", true)) {
                int[] bounds = PlotGeometry.getMergedPlotBounds(plot.getWorld(), plot.getId());
                if (hasContainerWithItems(player.getWorld(), bounds)) {
                    player.sendMessage(ChatColor.RED + "You cannot backup a plot containing chests or containers with items! Please empty them first.");
                    return;
                }
            }

            player.sendMessage(ChatColor.YELLOW + "Saving plot backup schematic...");
            String schemName = "backup_" + plot.getId().toString().replace(";", "_") + "_" + System.currentTimeMillis() + ".schem";
            boolean copyEntities = player.hasPermission("plots.admin") || plugin.getConfig().getBoolean("copy-paste.copy-entities", false);

            plugin.getSchematicHandler().savePlotSchematic(plot.getWorld(), plot.getId(), schemName, copyEntities).thenAccept(file -> {
                if (file == null) {
                    player.sendMessage(ChatColor.RED + "Failed to create schematic backup.");
                    return;
                }
                PlotBackup backup = new PlotBackup(0, plot.getId(), plot.getWorld(), System.currentTimeMillis(), player.getUniqueId(), file.getAbsolutePath());
                plugin.getDatabaseManager().saveBackup(backup).thenRun(() -> {
                    player.sendMessage(ChatColor.GREEN + "Plot backup saved successfully!");
                });
            });
        } else if (sub.equals("list")) {
            plugin.getDatabaseManager().getBackups(plot.getWorld(), plot.getId()).thenAccept(backups -> {
                if (backups.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "No backups found for this plot.");
                    return;
                }
                player.sendMessage(ChatColor.GOLD + "=== Backup List ===");
                for (int i = 0; i < backups.size(); i++) {
                    PlotBackup b = backups.get(i);
                    Date date = new Date(b.getTimestamp());
                    player.sendMessage(ChatColor.YELLOW + "[" + i + "] " + ChatColor.WHITE + date + " - " + plugin.getUuidCache().getName(b.getCreator()));
                }
            });
        } else if (sub.equals("load")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Usage: /plot backup load <index>");
                return;
            }
            int index;
            try {
                index = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid index.");
                return;
            }

            plugin.getDatabaseManager().getBackups(plot.getWorld(), plot.getId()).thenAccept(backups -> {
                if (index < 0 || index >= backups.size()) {
                    player.sendMessage(ChatColor.RED + "Index out of bounds.");
                    return;
                }
                PlotBackup backup = backups.get(index);
                player.sendMessage(ChatColor.YELLOW + "Pasting backup schematic asynchronously...");
                plugin.getSchematicHandler().pastePlotSchematic(plot.getWorld(), plot.getId(), new File(backup.getFilePath())).thenAccept(success -> {
                    if (success) {
                        player.sendMessage(ChatColor.GREEN + "Backup loaded successfully!");
                    } else {
                        player.sendMessage(ChatColor.RED + "Failed to paste backup schematic.");
                    }
                });
            });
        }
    }

    private void handleDownload(Player player) {
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            player.sendMessage(ChatColor.RED + "WorldEdit/FAWE is not installed. Download features are disabled.");
            return;
        }

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "You are not standing on a claimed plot.");
            return;
        }

        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            player.sendMessage(ChatColor.RED + "You do not own this plot.");
            return;
        }

        if (!player.hasPermission("plots.admin") && plugin.getConfig().getBoolean("copy-paste.prevent-container-dupes", true)) {
            int[] bounds = PlotGeometry.getMergedPlotBounds(plot.getWorld(), plot.getId());
            if (hasContainerWithItems(player.getWorld(), bounds)) {
                player.sendMessage(ChatColor.RED + "You cannot download a plot containing chests or containers with items! Please empty them first.");
                return;
            }
        }

        player.sendMessage(ChatColor.YELLOW + "Generating schematic for download...");
        String schemName = "download_" + player.getUniqueId() + ".schem";
        boolean copyEntities = player.hasPermission("plots.admin") || plugin.getConfig().getBoolean("copy-paste.copy-entities", false);

        plugin.getSchematicHandler().savePlotSchematic(plot.getWorld(), plot.getId(), schemName, copyEntities).thenAccept(file -> {
            if (file == null) {
                player.sendMessage(ChatColor.RED + "Failed to create schematic.");
                return;
            }

            player.sendMessage(ChatColor.YELLOW + "Uploading to remote server...");
            plugin.getDownloadHandler().uploadSchematic(file).thenAccept(res -> {
                if (res != null) {
                    player.sendMessage(ChatColor.GREEN + "=== Download Link ===");
                    player.sendMessage(ChatColor.GOLD + "URL: " + ChatColor.UNDERLINE + res.downloadUrl);
                    player.sendMessage(ChatColor.YELLOW + "Deletion Key: " + ChatColor.LIGHT_PURPLE + res.deletionKey);
                    player.sendMessage(ChatColor.WHITE + "Use /plot delete-download <key> to remove it.");
                } else {
                    player.sendMessage(ChatColor.RED + "Upload failed. Check remote endpoint connection.");
                }
            });
        });
    }

    private void handleDeleteDownload(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /plot delete-download <key>");
            return;
        }

        String key = args[1];
        player.sendMessage(ChatColor.YELLOW + "Sending deletion request...");
        plugin.getDownloadHandler().deleteUploadedSchematic(key).thenAccept(success -> {
            if (success) {
                player.sendMessage(ChatColor.GREEN + "Schematic deleted from remote host.");
            } else {
                player.sendMessage(ChatColor.RED + "Failed to delete schematic. Key may be invalid.");
            }
        });
    }

    private void handleChatToggle(Player player) {
        if (plugin.getPlotChatEnabled().contains(player.getUniqueId())) {
            plugin.getPlotChatEnabled().remove(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Plot chat mode disabled. You are now speaking in public chat.");
        } else {
            plugin.getPlotChatEnabled().add(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Plot chat mode enabled. Messages will only be heard by players on the same plot.");
        }
    }

    private void handleToggle(Player player, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("worldedit")) {
            player.sendMessage(ChatColor.RED + "Usage: /plot toggle worldedit");
            return;
        }

        if (player.hasMetadata("sp_we_disabled")) {
            player.removeMetadata("sp_we_disabled", plugin);
            player.sendMessage(ChatColor.GREEN + "WorldEdit operations enabled inside plot bounds.");
        } else {
            player.setMetadata("sp_we_disabled", new FixedMetadataValue(plugin, true));
            player.sendMessage(ChatColor.GREEN + "WorldEdit operations disabled inside plot bounds.");
        }
    }

    public static class SetupSession {
        public int step = 0;
        public String worldName;
        public int plotSize = 32;
        public int roadWidth = 7;
        public Material floorBlock = Material.GRASS_BLOCK;
        public Material roadBlock = Material.SMOOTH_STONE;
        public Material wallBlockClaimed = Material.OAK_SLAB;
        public Material wallBlockUnclaimed = Material.STONE_SLAB;
    }

    private final Map<UUID, SetupSession> activeSetups = new ConcurrentHashMap<>();

    public boolean handleSetupChat(Player player, String message) {
        SetupSession session = activeSetups.get(player.getUniqueId());
        if (session == null) return false;

        message = message.trim();
        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("exit")) {
            activeSetups.remove(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "Setup wizard aborted.");
            return true;
        }

        switch (session.step) {
            case 0: // World Name
                if (message.isEmpty() || message.contains(" ")) {
                    player.sendMessage(ChatColor.RED + "World name cannot contain spaces. Please enter again:");
                    return true;
                }
                if (Bukkit.getWorld(message) != null) {
                    player.sendMessage(ChatColor.RED + "A world named '" + message + "' already exists! Please enter a different name:");
                    return true;
                }
                session.worldName = message;
                session.step = 1;
                player.sendMessage(ChatColor.GREEN + "World Name set to: " + session.worldName);
                player.sendMessage(ChatColor.YELLOW + "Step 2: Enter Plot Size (e.g. 32, or type 'default'):");
                break;

            case 1: // Plot Size
                if (message.equalsIgnoreCase("default") || message.equalsIgnoreCase("d")) {
                    session.plotSize = 32;
                } else {
                    try {
                        session.plotSize = Integer.parseInt(message);
                        if (session.plotSize <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid number. Please enter a positive integer for Plot Size:");
                        return true;
                    }
                }
                session.step = 2;
                player.sendMessage(ChatColor.GREEN + "Plot Size set to: " + session.plotSize);
                player.sendMessage(ChatColor.YELLOW + "Step 3: Enter Road Width (e.g. 7, or type 'default'):");
                break;

            case 2: // Road Width
                if (message.equalsIgnoreCase("default") || message.equalsIgnoreCase("d")) {
                    session.roadWidth = 7;
                } else {
                    try {
                        session.roadWidth = Integer.parseInt(message);
                        if (session.roadWidth <= 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid number. Please enter a positive integer for Road Width:");
                        return true;
                    }
                }
                session.step = 3;
                player.sendMessage(ChatColor.GREEN + "Road Width set to: " + session.roadWidth);
                player.sendMessage(ChatColor.YELLOW + "Step 4: Enter Floor Block material (e.g. GRASS_BLOCK, or type 'default'):");
                break;

            case 3: // Floor Block
                if (message.equalsIgnoreCase("default") || message.equalsIgnoreCase("d")) {
                    session.floorBlock = Material.GRASS_BLOCK;
                } else {
                    Material mat = Material.matchMaterial(message.toUpperCase());
                    if (mat == null) {
                        player.sendMessage(ChatColor.RED + "Unknown material. Please enter a valid Bukkit Material:");
                        return true;
                    }
                    session.floorBlock = mat;
                }
                session.step = 4;
                player.sendMessage(ChatColor.GREEN + "Floor Block set to: " + session.floorBlock.name());
                player.sendMessage(ChatColor.YELLOW + "Step 5: Enter Road Block material (e.g. SMOOTH_STONE, or type 'default'):");
                break;

            case 4: // Road Block
                if (message.equalsIgnoreCase("default") || message.equalsIgnoreCase("d")) {
                    session.roadBlock = Material.SMOOTH_STONE;
                } else {
                    Material mat = Material.matchMaterial(message.toUpperCase());
                    if (mat == null) {
                        player.sendMessage(ChatColor.RED + "Unknown material. Please enter a valid Bukkit Material:");
                        return true;
                    }
                    session.roadBlock = mat;
                }
                session.step = 5;
                player.sendMessage(ChatColor.GREEN + "Road Block set to: " + session.roadBlock.name());
                player.sendMessage(ChatColor.YELLOW + "Step 6: Enter Claimed Plot Wall/Barrier Block (e.g. OAK_SLAB, or type 'default'):");
                break;

            case 5: // Wall Block Claimed
                if (message.equalsIgnoreCase("default") || message.equalsIgnoreCase("d")) {
                    session.wallBlockClaimed = Material.OAK_SLAB;
                } else {
                    Material mat = Material.matchMaterial(message.toUpperCase());
                    if (mat == null) {
                        player.sendMessage(ChatColor.RED + "Unknown material. Please enter a valid Bukkit Material:");
                        return true;
                    }
                    session.wallBlockClaimed = mat;
                }
                session.step = 6;
                player.sendMessage(ChatColor.GREEN + "Claimed Wall Block set to: " + session.wallBlockClaimed.name());
                player.sendMessage(ChatColor.YELLOW + "Step 7: Enter Unclaimed Plot Wall/Barrier Block (e.g. STONE_SLAB, or type 'default'):");
                break;

            case 6: // Wall Block Unclaimed
                if (message.equalsIgnoreCase("default") || message.equalsIgnoreCase("d")) {
                    session.wallBlockUnclaimed = Material.STONE_SLAB;
                } else {
                    Material mat = Material.matchMaterial(message.toUpperCase());
                    if (mat == null) {
                        player.sendMessage(ChatColor.RED + "Unknown material. Please enter a valid Bukkit Material:");
                        return true;
                    }
                    session.wallBlockUnclaimed = mat;
                }
                session.step = 7;
                player.sendMessage(ChatColor.GREEN + "Unclaimed Wall Block set to: " + session.wallBlockUnclaimed.name());
                player.sendMessage(ChatColor.GOLD + "=== Setup Summary ===");
                player.sendMessage(ChatColor.YELLOW + "World Name: " + ChatColor.WHITE + session.worldName);
                player.sendMessage(ChatColor.YELLOW + "Plot Size: " + ChatColor.WHITE + session.plotSize);
                player.sendMessage(ChatColor.YELLOW + "Road Width: " + ChatColor.WHITE + session.roadWidth);
                player.sendMessage(ChatColor.YELLOW + "Floor Block: " + ChatColor.WHITE + session.floorBlock.name());
                player.sendMessage(ChatColor.YELLOW + "Road Block: " + ChatColor.WHITE + session.roadBlock.name());
                player.sendMessage(ChatColor.YELLOW + "Claimed Wall: " + ChatColor.WHITE + session.wallBlockClaimed.name());
                player.sendMessage(ChatColor.YELLOW + "Unclaimed Wall: " + ChatColor.WHITE + session.wallBlockUnclaimed.name());
                player.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GREEN + "confirm" + ChatColor.YELLOW + " to generate the world, or " + ChatColor.RED + "cancel" + ChatColor.YELLOW + " to exit.");
                break;

            case 7: // Confirmation
                if (message.equalsIgnoreCase("confirm")) {
                    activeSetups.remove(player.getUniqueId());
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        generatePlotWorld(player, session.worldName, session.plotSize, session.roadWidth, session.floorBlock, session.roadBlock, session.wallBlockClaimed, session.wallBlockUnclaimed);
                    });
                } else {
                    player.sendMessage(ChatColor.RED + "Invalid confirmation. Type 'confirm' to generate the world, or 'cancel' to exit.");
                }
                break;
        }

        return true;
    }

    private void handleSetup(Player player, String[] args) {
        if (!player.hasPermission("plots.admin.command.setup")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }

        if (args.length >= 2) {
            String worldName = args[1];
            generatePlotWorld(player, worldName, 32, 7, Material.GRASS_BLOCK, Material.SMOOTH_STONE, Material.OAK_SLAB, Material.STONE_SLAB);
            return;
        }

        SetupSession session = new SetupSession();
        activeSetups.put(player.getUniqueId(), session);
        player.sendMessage(ChatColor.GOLD + "=== SimplePlots World Setup Wizard ===");
        player.sendMessage(ChatColor.YELLOW + "Step 1: Enter World Name (or type 'cancel' to exit):");
    }

    private void generatePlotWorld(Player player, String worldName, int plotSize, int roadWidth, Material floorBlock, Material roadBlock, Material wallBlockClaimed, Material wallBlockUnclaimed) {
        if (Bukkit.getWorld(worldName) != null) {
            player.sendMessage(ChatColor.RED + "Cannot generate world: A world named '" + worldName + "' already exists or is loaded!");
            return;
        }
        player.sendMessage(ChatColor.YELLOW + "Creating plot world: " + worldName + "...");

        // Setup worlds.yml
        plugin.getWorldsFile().set("worlds." + worldName + ".plot-size", plotSize);
        plugin.getWorldsFile().set("worlds." + worldName + ".road-width", roadWidth);
        plugin.getWorldsFile().set("worlds." + worldName + ".floor-height", 64);
        plugin.getWorldsFile().set("worlds." + worldName + ".floor-block", floorBlock.name());
        plugin.getWorldsFile().set("worlds." + worldName + ".filler-block", "DIRT");
        plugin.getWorldsFile().set("worlds." + worldName + ".bed-block", "BEDROCK");
        plugin.getWorldsFile().set("worlds." + worldName + ".road-block", roadBlock.name());
        plugin.getWorldsFile().set("worlds." + worldName + ".wall-block-claimed", wallBlockClaimed.name());
        plugin.getWorldsFile().set("worlds." + worldName + ".wall-block-unclaimed", wallBlockUnclaimed.name());
        plugin.getWorldsFile().set("worlds." + worldName + ".border-block", "STONE_BRICKS");
        plugin.getWorldsFile().set("worlds." + worldName + ".entry-message-claimed", "&7Entering {owner} Plot");
        plugin.getWorldsFile().set("worlds." + worldName + ".entry-message-unclaimed", "&7Entering Unclaimed Plot");
        plugin.getWorldsFile().set("worlds." + worldName + ".infinite-saturation", true);
        plugin.getWorldsFile().set("worlds." + worldName + ".do-daylight-cycle", false);
        plugin.getWorldsFile().set("worlds." + worldName + ".do-mob-spawning", false);


        try {
            plugin.getWorldsFile().save(new File(plugin.getDataFolder(), "worlds.yml"));
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save worlds.yml: " + e.getMessage());
        }

        // Initialize configuration
        PlotWorldConfig cfg = new PlotWorldConfig(worldName, plugin.getWorldsFile().getConfigurationSection("worlds." + worldName));
        plugin.addWorldConfig(worldName, cfg);

        // Load / Create Bukkit world
        WorldCreator creator = new WorldCreator(worldName);
        creator.generator(new com.simpleplots.generator.PlotChunkGenerator(cfg));
        creator.generateStructures(false); // Disable structures by default
        World world = Bukkit.createWorld(creator);

        if (world != null) {
            world.setKeepSpawnInMemory(false);
            world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false); // Disable natural mob spawning
            player.sendMessage(ChatColor.GREEN + "World " + worldName + " generated and loaded successfully!");
            player.teleport(new Location(world, 0, cfg.getFloorHeight() + 1, 0));
        } else {
            player.sendMessage(ChatColor.RED + "Failed to generate world.");
        }
    }

    private void handleCondense(Player player, String[] args) {
        if (!player.hasPermission("plots.admin.command.condense")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            player.sendMessage(ChatColor.RED + "WorldEdit/FAWE is not installed. Condense feature is disabled.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /plot condense <worldName>");
            return;
        }

        String worldName = args[1];
        player.sendMessage(ChatColor.YELLOW + "Condensing plots in world " + worldName + "...");
        // Collect all claimed plots in world
        List<Plot> worldPlots = new ArrayList<>();
        for (Plot p : plugin.getPlotAPI().getAllPlots()) {
            if (p.getWorld().equalsIgnoreCase(worldName)) {
                worldPlots.add(p);
            }
        }

        if (worldPlots.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No plots found in world.");
            return;
        }

        // Relocate plots close to 0,0 spiral outwards
        player.sendMessage(ChatColor.YELLOW + "Relocating " + worldPlots.size() + " plots close to 0,0...");

        CompletableFuture.runAsync(() -> {
            int x = 0;
            int z = 0;
            int dx = 0;
            int dz = -1;
            int maxSegment = 1;
            int segmentCount = 0;
            int segmentPasses = 0;

            for (Plot plot : worldPlots) {
                PlotId originalId = plot.getId();
                PlotId newId = new PlotId(x, z);

                if (!originalId.equals(newId)) {
                    // Update cache and DB
                    plugin.getPlotAPI().removePlotFromCache(worldName, originalId);
                    plugin.getPlotAPI().addPlotToCache(new Plot(newId, worldName, plot.getOwner()));
                    
                    // Run Schematic copy-paste
                    String tempSchem = "temp_condense_" + originalId.toString().replace(";", "_") + ".schem";
                    plugin.getSchematicHandler().savePlotSchematic(worldName, originalId, tempSchem).thenAccept(file -> {
                        if (file != null) {
                            plugin.getSchematicHandler().pastePlotSchematic(worldName, newId, file).thenRun(() -> {
                                file.delete(); // cleanup
                            });
                        }
                    });
                }

                // Spiral coordinates increment
                if (segmentCount == maxSegment) {
                    segmentCount = 0;
                    int temp = dx;
                    dx = -dz;
                    dz = temp;
                    segmentPasses++;
                    if (segmentPasses == 2) {
                        segmentPasses = 0;
                        maxSegment++;
                    }
                }
                x += dx;
                z += dz;
                segmentCount++;
            }
            player.sendMessage(ChatColor.GREEN + "Condensing task completed!");
        });
    }

    private void handleTrim(Player player) {
        if (!player.hasPermission("plots.admin.command.trim")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Trimming unused chunks in plot worlds...");

        CompletableFuture.runAsync(() -> {
            for (World world : Bukkit.getWorlds()) {
                if (plugin.getWorldConfig(world.getName()) != null) {
                    File regionDir = new File(world.getWorldFolder(), "region");
                    if (!regionDir.exists()) continue;

                    File[] files = regionDir.listFiles((dir, name) -> name.endsWith(".mca"));
                    if (files == null) continue;

                    for (File mcaFile : files) {
                        // Region file: r.X.Z.mca
                        String[] parts = mcaFile.getName().split("\\.");
                        try {
                            int rx = Integer.parseInt(parts[1]);
                            int rz = Integer.parseInt(parts[2]);

                            // Bounding block box for this region
                            int minX = rx * 512;
                            int minZ = rz * 512;
                            int maxX = (rx + 1) * 512 - 1;
                            int maxZ = (rz + 1) * 512 - 1;

                            // Check if any claimed plot overlaps this region
                            boolean hasPlot = false;
                            for (Plot p : plugin.getPlotAPI().getAllPlots()) {
                                if (p.getWorld().equalsIgnoreCase(world.getName())) {
                                    int[] bounds = PlotGeometry.getPlotBounds(world.getName(), p.getId());
                                    if (bounds != null) {
                                        // Overlap check
                                        if (!(bounds[2] < minX || bounds[0] > maxX || bounds[3] < minZ || bounds[1] > maxZ)) {
                                            hasPlot = true;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (!hasPlot) {
                                // Safe to delete! Let's unload chunks in region first
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    int chunkMinX = minX >> 4;
                                    int chunkMinZ = minZ >> 4;
                                    for (int cx = chunkMinX; cx < chunkMinX + 32; cx++) {
                                        for (int cz = chunkMinZ; cz < chunkMinZ + 32; cz++) {
                                            if (world.isChunkLoaded(cx, cz)) {
                                                world.unloadChunk(cx, cz, true);
                                            }
                                        }
                                    }
                                    // Delete region file
                                    mcaFile.delete();
                                });
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            player.sendMessage(ChatColor.GREEN + "Trimming task complete!");
        });
    }

    private void handleDatabase(Player player, String[] args) {
        if (!player.hasPermission("plots.admin.command.database")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /plot database convert sqlite|mysql or /plot database import");
            return;
        }

        String sub = args[1].toLowerCase();
        if (sub.equals("convert")) {
            String type = args[2].toLowerCase();
            if (!type.equals("sqlite") && !type.equals("mysql")) {
                player.sendMessage(ChatColor.RED + "Invalid database type.");
                return;
            }

            player.sendMessage(ChatColor.YELLOW + "Converting database to " + type + "...");
            plugin.getDatabaseManager().convertDatabase(type).thenAccept(success -> {
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "Database converted successfully!");
                } else {
                    player.sendMessage(ChatColor.RED + "Database conversion failed.");
                }
            });
        }
    }

    private void handleConfirm(Player player) {
        PendingAction action = confirmations.get(player.getUniqueId());
        if (action == null) {
            player.sendMessage(ChatColor.RED + "You do not have any pending confirmation.");
            return;
        }

        if (System.currentTimeMillis() > action.expires) {
            player.sendMessage(ChatColor.RED + "Confirmation expired.");
            confirmations.remove(player.getUniqueId());
            return;
        }

        confirmations.remove(player.getUniqueId());

        switch (action.type) {
            case CLEAR:
                executeClear(player, action);
                break;
            case DELETE:
                executeDelete(player, action);
                break;
            case MERGE:
                executeMerge(player, action);
                break;
            case UNLINK:
                executeUnlink(player, action);
                break;
        }
    }

    private void handleInfo(Player player) {
        if (!checkPermission(player, "plots.info")) return;

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            com.simpleplots.util.Messages.send(player, "commands.not-standing-in-plot");
            return;
        }

        String ownerName = plugin.getUuidCache().getName(plot.getOwner());
        String trustedList = getMembersNames(plot.getTrusted());
        String addedList = getMembersNames(plot.getAdded());
        String deniedList = getMembersNames(plot.getDenied());
        String flagsList = plot.getFlags().isEmpty() ? com.simpleplots.util.Messages.get("commands.info-none") : plot.getFlags().toString();

        com.simpleplots.util.Messages.send(player, "commands.info-title", "{id}", plot.getId().toString());
        if (plot.hasFlag("display-name")) {
            player.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.RESET + plot.getFlagValue("display-name"));
        }
        com.simpleplots.util.Messages.send(player, "commands.info-owner", "{owner}", ownerName);
        com.simpleplots.util.Messages.send(player, "commands.info-trusted", "{players}", trustedList);
        com.simpleplots.util.Messages.send(player, "commands.info-added", "{players}", addedList);
        com.simpleplots.util.Messages.send(player, "commands.info-denied", "{players}", deniedList);
        com.simpleplots.util.Messages.send(player, "commands.info-flags", "{flags}", flagsList);

        double avg = plot.getAverageRating();
        int count = plot.getRatings().size();
        String ratingStr = count == 0 ? "No ratings yet" : String.format("%.1f/5 (%d ratings)", avg, count);
        player.sendMessage(ChatColor.YELLOW + "Rating: " + ChatColor.GOLD + ratingStr);
    }

    private String getMembersNames(Set<UUID> uuids) {
        if (uuids.isEmpty()) return com.simpleplots.util.Messages.get("commands.info-none");
        StringBuilder sb = new StringBuilder();
        for (UUID u : uuids) {
            sb.append(plugin.getUuidCache().getName(u)).append(", ");
        }
        return sb.substring(0, sb.length() - 2);
    }

    private void handleWorldTp(Player player, String[] args) {
        if (!player.hasPermission("plots.admin.command.worldtp")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /plot worldtp <worldName>");
            return;
        }

        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "World '" + worldName + "' is not loaded or does not exist.");
            return;
        }

        player.teleport(world.getSpawnLocation());
        player.sendMessage(ChatColor.GREEN + "Teleported to world spawn of '" + worldName + "'.");
    }

    private List<PlotId> getPlayerClusterHeads(UUID uuid) {
        List<Set<PlotId>> clusters = new ArrayList<>();
        Set<PlotId> owned = new HashSet<>();
        String worldName = null;
        for (Plot plot : plugin.getPlotAPI().getAllPlots()) {
            if (plot.getOwner() != null && plot.getOwner().equals(uuid)) {
                owned.add(plot.getId());
                if (worldName == null) worldName = plot.getWorld();
            }
        }
        if (owned.isEmpty()) return Collections.emptyList();
        
        Set<PlotId> visited = new HashSet<>();
        for (PlotId pid : owned) {
            if (!visited.contains(pid)) {
                Set<PlotId> cluster = new HashSet<>();
                Queue<PlotId> queue = new LinkedList<>();
                queue.add(pid);
                visited.add(pid);
                cluster.add(pid);
                while (!queue.isEmpty()) {
                    PlotId current = queue.poll();
                    Plot currentPlot = plugin.getPlotAPI().getPlot(worldName, current);
                    if (currentPlot == null) continue;
                    
                    if (currentPlot.isMergedE()) {
                        PlotId next = new PlotId(current.getX() + 1, current.getZ());
                        if (owned.contains(next) && !visited.contains(next)) {
                            visited.add(next);
                            cluster.add(next);
                            queue.add(next);
                        }
                    }
                    if (currentPlot.isMergedW()) {
                        PlotId next = new PlotId(current.getX() - 1, current.getZ());
                        if (owned.contains(next) && !visited.contains(next)) {
                            visited.add(next);
                            cluster.add(next);
                            queue.add(next);
                        }
                    }
                    if (currentPlot.isMergedN()) {
                        PlotId next = new PlotId(current.getX(), current.getZ() - 1);
                        if (owned.contains(next) && !visited.contains(next)) {
                            visited.add(next);
                            cluster.add(next);
                            queue.add(next);
                        }
                    }
                    if (currentPlot.isMergedS()) {
                        PlotId next = new PlotId(current.getX(), current.getZ() + 1);
                        if (owned.contains(next) && !visited.contains(next)) {
                            visited.add(next);
                            cluster.add(next);
                            queue.add(next);
                        }
                    }
                }
                clusters.add(cluster);
            }
        }
        
        clusters.sort((c1, c2) -> {
            PlotId p1 = c1.iterator().next();
            PlotId p2 = c2.iterator().next();
            if (p1.getX() != p2.getX()) return Integer.compare(p1.getX(), p2.getX());
            return Integer.compare(p1.getZ(), p2.getZ());
        });
        
        List<PlotId> heads = new ArrayList<>();
        for (Set<PlotId> c : clusters) {
            heads.add(c.iterator().next());
        }
        return heads;
    }

    private Location getPlotHomeLocation(Plot plot) {
        PlotWorldConfig worldConfig = plugin.getWorldConfig(plot.getWorld());
        World world = Bukkit.getWorld(plot.getWorld());
        if (world == null || worldConfig == null) return null;

        if (plot.hasFlag("teleport-x")) {
            try {
                double x = Double.parseDouble(plot.getFlagValue("teleport-x"));
                double y = Double.parseDouble(plot.getFlagValue("teleport-y"));
                double z = Double.parseDouble(plot.getFlagValue("teleport-z"));
                float yaw = Float.parseFloat(plot.getFlagValue("teleport-yaw"));
                float pitch = Float.parseFloat(plot.getFlagValue("teleport-pitch"));
                return new Location(world, x, y, z, yaw, pitch);
            } catch (NumberFormatException ignored) {}
        }

        int[] bounds = PlotGeometry.getPlotBounds(plot.getWorld(), plot.getId());
        double targetX = bounds[0] + (bounds[2] - bounds[0]) / 2.0;
        double targetZ = bounds[1] + (bounds[3] - bounds[1]) / 2.0;
        return new Location(world, targetX, worldConfig.getFloorHeight() + 1, targetZ);
    }

    private void handleTransfer(Player player, String[] args) {
        if (!checkPermission(player, "plots.transfer")) return;

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /plot transfer <player>");
            return;
        }

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            com.simpleplots.util.Messages.send(player, "commands.not-standing-in-plot");
            return;
        }

        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            com.simpleplots.util.Messages.send(player, "commands.not-owner");
            return;
        }

        String targetName = args[1];
        UUID targetUuid = plugin.getUuidCache().getUUID(targetName);
        if (targetUuid == null) {
            com.simpleplots.util.Messages.send(player, "commands.player-not-found", "{name}", targetName);
            return;
        }

        if (targetUuid.equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot transfer a plot to yourself.");
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        int limit = plugin.getConfig().getInt("max-plots-per-player", 3);
        if (targetPlayer != null) {
            if (targetPlayer.hasPermission("plots.plot.infinite") || targetPlayer.hasPermission("plots.admin")) {
                limit = -1;
            } else {
                for (int i = 100; i >= 1; i--) {
                    if (targetPlayer.hasPermission("plots.plot." + i)) {
                        limit = i;
                        break;
                    }
                }
            }
        }
        
        if (limit != -1) {
            int currentClusters = getPlayerClusterCount(targetUuid);
            if (currentClusters >= limit) {
                player.sendMessage(ChatColor.RED + targetName + " has reached their plot limit of " + limit + " plots.");
                return;
            }
        }

        Set<PlotId> cluster = getMergedCluster(plot.getWorld(), plot.getId());
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        for (PlotId pid : cluster) {
            Plot p = plugin.getPlotAPI().getPlot(plot.getWorld(), pid);
            if (p != null) {
                p.setOwner(targetUuid);
                final Plot finalP = p;
                future = future.thenCompose(v -> plugin.getDatabaseManager().savePlotOwner(finalP));
            }
        }

        future.thenRun(() -> {
            player.sendMessage(ChatColor.GREEN + "Successfully transferred plot cluster to " + targetName + "!");
            if (targetPlayer != null) {
                targetPlayer.sendMessage(ChatColor.GREEN + player.getName() + " transferred a plot cluster to you!");
            }
        });
    }

    private void handleVisit(Player player, String[] args) {
        if (!checkPermission(player, "plots.visit")) return;

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /plot visit <player> [#]");
            return;
        }

        String targetName = args[1];
        UUID targetUuid = plugin.getUuidCache().getUUID(targetName);
        if (targetUuid == null) {
            com.simpleplots.util.Messages.send(player, "commands.player-not-found", "{name}", targetName);
            return;
        }

        List<PlotId> heads = getPlayerClusterHeads(targetUuid);
        if (heads.isEmpty()) {
            player.sendMessage(ChatColor.RED + targetName + " does not own any plots.");
            return;
        }

        int index = 0;
        if (args.length >= 3) {
            try {
                index = Integer.parseInt(args[2]) - 1;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid index. Teleporting to first plot instead.");
            }
        }

        if (index < 0 || index >= heads.size()) {
            player.sendMessage(ChatColor.RED + "Plot index out of range. Target only has " + heads.size() + " plot clusters.");
            return;
        }

        PlotId targetId = heads.get(index);
        Plot plot = plugin.getPlotAPI().getPlot(player.getWorld().getName(), targetId);
        if (plot == null) {
            for (Plot p : plugin.getPlotAPI().getAllPlots()) {
                if (p.getOwner().equals(targetUuid)) {
                    plot = p;
                    break;
                }
            }
        }

        if (plot != null) {
            Location homeLoc = getPlotHomeLocation(plot);
            if (homeLoc != null) {
                player.teleport(homeLoc);
                player.sendMessage(ChatColor.GREEN + "Teleported to " + targetName + "'s plot #" + (index + 1) + ".");
            }
        }
    }

    private void handleHome(Player player, String[] args) {
        if (!checkPermission(player, "plots.home")) return;

        List<PlotId> heads = getPlayerClusterHeads(player.getUniqueId());
        if (heads.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not own any plots. Use /plot claim to get one.");
            return;
        }

        int index = 0;
        if (args.length >= 2) {
            try {
                index = Integer.parseInt(args[1]) - 1;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid index. Teleporting to your first plot instead.");
            }
        }

        if (index < 0 || index >= heads.size()) {
            player.sendMessage(ChatColor.RED + "Plot index out of range. You only own " + heads.size() + " plot clusters.");
            return;
        }

        PlotId targetId = heads.get(index);
        Plot plot = plugin.getPlotAPI().getPlot(player.getWorld().getName(), targetId);
        if (plot == null) {
            for (Plot p : plugin.getPlotAPI().getAllPlots()) {
                if (p.getOwner().equals(player.getUniqueId())) {
                    plot = p;
                    break;
                }
            }
        }

        if (plot != null) {
            Location homeLoc = getPlotHomeLocation(plot);
            if (homeLoc != null) {
                player.teleport(homeLoc);
                player.sendMessage(ChatColor.GREEN + "Teleported to your plot #" + (index + 1) + ".");
            }
        }
    }

    private void handleNear(Player player) {
        if (!checkPermission(player, "plots.near")) return;

        if (plugin.getWorldConfig(player.getWorld().getName()) == null) {
            com.simpleplots.util.Messages.send(player, "commands.not-in-plot-world");
            return;
        }

        PlotId center = PlotGeometry.getPlotId(player.getLocation());
        if (center == null) {
            double blockX = player.getLocation().getX();
            double blockZ = player.getLocation().getZ();
            PlotWorldConfig cfg = plugin.getWorldConfig(player.getWorld().getName());
            int totalSize = cfg.getTotalSize();
            int x = (int) Math.floor(blockX / totalSize);
            int z = (int) Math.floor(blockZ / totalSize);
            center = new PlotId(x, z);
        }

        List<String> nearby = new ArrayList<>();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                PlotId pid = new PlotId(center.getX() + dx, center.getZ() + dz);
                Plot p = plugin.getPlotAPI().getPlot(player.getWorld().getName(), pid);
                if (p != null && p.isClaimed()) {
                    Location homeLoc = getPlotHomeLocation(p);
                    if (homeLoc != null) {
                        double distance = player.getLocation().distance(homeLoc);
                        String ownerName = plugin.getUuidCache().getName(p.getOwner());
                        nearby.add(ChatColor.YELLOW + "- Plot " + pid + " (Owner: " + ownerName + ") - " + (int) distance + " blocks away");
                    }
                }
            }
        }

        if (nearby.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No claimed plots found within 3 plots radius.");
        } else {
            player.sendMessage(ChatColor.GOLD + "=== Nearby Claimed Plots ===");
            for (String line : nearby) {
                player.sendMessage(line);
            }
        }
    }

    private void handleStats(Player player) {
        if (!checkPermission(player, "plots.stats")) return;

        UUID uuid = player.getUniqueId();
        List<Plot> ownedPlots = new ArrayList<>();
        for (Plot p : plugin.getPlotAPI().getAllPlots()) {
            if (p.isClaimed() && p.getOwner().equals(uuid)) {
                ownedPlots.add(p);
            }
        }

        if (ownedPlots.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You do not own any plots.");
            return;
        }

        int totalCells = ownedPlots.size();
        int clusterCount = getPlayerClusterCount(uuid);
        int merges = 0;
        Set<String> activeFlags = new HashSet<>();

        PlotWorldConfig worldConfig = plugin.getWorldConfig(player.getWorld().getName());
        if (worldConfig == null && !ownedPlots.isEmpty()) {
            worldConfig = plugin.getWorldConfig(ownedPlots.get(0).getWorld());
        }

        long totalArea = 0;
        if (worldConfig != null) {
            long plotSize = worldConfig.getPlotSize();
            totalArea = totalCells * plotSize * plotSize;
        }

        for (Plot p : ownedPlots) {
            if (p.isMergedE()) merges++;
            if (p.isMergedW()) merges++;
            if (p.isMergedN()) merges++;
            if (p.isMergedS()) merges++;
            activeFlags.addAll(p.getFlags().keySet());
        }
        merges = merges / 2;

        player.sendMessage(ChatColor.GOLD + "=== Plot Statistics ===");
        player.sendMessage(ChatColor.YELLOW + "Plots Owned (Cells): " + ChatColor.WHITE + totalCells);
        player.sendMessage(ChatColor.YELLOW + "Distinct Clusters: " + ChatColor.WHITE + clusterCount);
        player.sendMessage(ChatColor.YELLOW + "Merged Road Connections: " + ChatColor.WHITE + merges);
        player.sendMessage(ChatColor.YELLOW + "Total Claimed Area: " + ChatColor.WHITE + totalArea + " sq blocks");
        player.sendMessage(ChatColor.YELLOW + "Active Flags count: " + ChatColor.WHITE + activeFlags.size() + " (" + activeFlags + ")");
    }

    private void handleSetName(Player player, String[] args) {
        if (!checkPermission(player, "plots.setname")) return;

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /plot set name <name>");
            return;
        }

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            com.simpleplots.util.Messages.send(player, "commands.not-standing-in-plot");
            return;
        }

        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            com.simpleplots.util.Messages.send(player, "commands.not-owner");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        String rawName = sb.toString().trim();
        String coloredName = ChatColor.translateAlternateColorCodes('&', rawName);

        plot.setFlag("display-name", coloredName);
        plugin.getDatabaseManager().savePlotFlag(plot.getWorld(), plot.getId(), "display-name", coloredName).thenRun(() -> {
            player.sendMessage(ChatColor.GREEN + "Plot display name set to: " + ChatColor.RESET + coloredName);
        });
    }

    private void handleAdminSuite(Player player, String[] args) {
        if (!player.hasPermission("plots.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /plot admin <claim|unclaim|clear|trust|untrust|deny|undeny> [args]");
            return;
        }

        String action = args[1].toLowerCase();
        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());

        switch (action) {
            case "panel":
                handleAdminPanel(player);
                break;
            case "claim":
                if (plugin.getWorldConfig(player.getWorld().getName()) == null) {
                    player.sendMessage(ChatColor.RED + "Not in a plot world.");
                    return;
                }
                PlotId plotId = PlotGeometry.getPlotId(player.getLocation());
                if (plotId == null || PlotGeometry.isRoad(player.getLocation())) {
                    player.sendMessage(ChatColor.RED + "Must be standing on a plot cell.");
                    return;
                }
                UUID claimTarget = player.getUniqueId();
                if (args.length >= 3) {
                    UUID targetUuid = plugin.getUuidCache().getUUID(args[2]);
                    if (targetUuid == null) {
                        player.sendMessage(ChatColor.RED + "Player " + args[2] + " not found.");
                        return;
                    }
                    claimTarget = targetUuid;
                }
                Plot claimPlot = plugin.getPlotAPI().getPlot(player.getWorld().getName(), plotId);
                if (claimPlot == null) {
                    claimPlot = new Plot(plotId, player.getWorld().getName(), claimTarget);
                    plugin.getPlotAPI().addPlotToCache(claimPlot);
                } else {
                    claimPlot.setOwner(claimTarget);
                }
                UUID finalClaimTarget = claimTarget;
                Plot finalClaimPlot = claimPlot;
                plugin.getDatabaseManager().savePlotOwner(claimPlot).thenRun(() -> {
                    player.sendMessage(ChatColor.GREEN + "Admin claimed plot " + plotId + " for " + plugin.getUuidCache().getName(finalClaimTarget));
                    PlotGeometry.updatePlotBorders(finalClaimPlot.getWorld(), finalClaimPlot.getId());
                });
                break;

            case "unclaim":
            case "delete":
                if (plot == null) {
                    player.sendMessage(ChatColor.RED + "Must be standing inside a claimed plot.");
                    return;
                }
                plugin.getPlotAPI().unclaimPlotCluster(plot.getWorld(), plot.getId()).thenRun(() -> {
                    player.sendMessage(ChatColor.GREEN + "Admin forcibly unclaimed and cleared plot cluster.");
                });
                break;

            case "clear":
                if (plot == null) {
                    player.sendMessage(ChatColor.RED + "Must be standing inside a claimed plot.");
                    return;
                }
                Set<PlotId> cluster = plugin.getPlotAPI().getMergedCluster(plot.getWorld(), plot.getId());
                World world = Bukkit.getWorld(plot.getWorld());
                com.simpleplots.generator.PlotWorldConfig cfg = plugin.getWorldConfig(plot.getWorld());
                if (world != null && cfg != null) {
                    for (PlotId pid : cluster) {
                        clearPlotBlocks(world, cfg, pid);
                    }
                    player.sendMessage(ChatColor.GREEN + "Admin forcibly cleared plot blocks.");
                }
                break;

            case "trust":
            case "add":
                if (plot == null) {
                    player.sendMessage(ChatColor.RED + "Must be standing inside a claimed plot.");
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /plot admin trust <player>");
                    return;
                }
                UUID trustUuid = plugin.getUuidCache().getUUID(args[2]);
                if (trustUuid == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return;
                }
                plot.getTrusted().add(trustUuid);
                plugin.getDatabaseManager().addPlotMember(plot.getWorld(), plot.getId(), trustUuid, "trusted").thenRun(() -> {
                    player.sendMessage(ChatColor.GREEN + "Admin added trust for " + args[2] + " on this plot.");
                });
                break;

            case "untrust":
            case "remove":
                if (plot == null) {
                    player.sendMessage(ChatColor.RED + "Must be standing inside a claimed plot.");
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /plot admin untrust <player>");
                    return;
                }
                UUID untrustUuid = plugin.getUuidCache().getUUID(args[2]);
                if (untrustUuid == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return;
                }
                plot.getTrusted().remove(untrustUuid);
                plot.getAdded().remove(untrustUuid);
                plugin.getDatabaseManager().removePlotMember(plot.getWorld(), plot.getId(), untrustUuid, "trusted")
                      .thenCompose(v -> plugin.getDatabaseManager().removePlotMember(plot.getWorld(), plot.getId(), untrustUuid, "added"))
                      .thenRun(() -> {
                          player.sendMessage(ChatColor.GREEN + "Admin removed trust/add for " + args[2] + " on this plot.");
                      });
                break;

            case "deny":
                if (plot == null) {
                    player.sendMessage(ChatColor.RED + "Must be standing inside a claimed plot.");
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /plot admin deny <player>");
                    return;
                }
                UUID denyUuid = plugin.getUuidCache().getUUID(args[2]);
                if (denyUuid == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return;
                }
                plot.getDenied().add(denyUuid);
                plugin.getDatabaseManager().addPlotMember(plot.getWorld(), plot.getId(), denyUuid, "denied").thenRun(() -> {
                    player.sendMessage(ChatColor.GREEN + "Admin denied " + args[2] + " on this plot.");
                });
                break;

            case "undeny":
                if (plot == null) {
                    player.sendMessage(ChatColor.RED + "Must be standing inside a claimed plot.");
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /plot admin undeny <player>");
                    return;
                }
                UUID undenyUuid = plugin.getUuidCache().getUUID(args[2]);
                if (undenyUuid == null) {
                    player.sendMessage(ChatColor.RED + "Player not found.");
                    return;
                }
                plot.getDenied().remove(undenyUuid);
                plugin.getDatabaseManager().removePlotMember(plot.getWorld(), plot.getId(), undenyUuid, "denied").thenRun(() -> {
                    player.sendMessage(ChatColor.GREEN + "Admin undenied " + args[2] + " on this plot.");
                });
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown admin action.");
                break;
        }
    }

    private void handleList(Player player, String[] args) {
        if (!checkPermission(player, "plots.list")) return;

        String filter = "all";
        int page = 1;

        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                filter = args[1].toLowerCase();
            }
        }
        if (args.length >= 3) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (NumberFormatException ignored) {}
        }

        List<String> entries = new ArrayList<>();

        if (filter.equalsIgnoreCase("unclaimed")) {
            if (plugin.getWorldConfig(player.getWorld().getName()) == null) {
                player.sendMessage(ChatColor.RED + "You must stand in a plot world to search for unclaimed plots.");
                return;
            }
            PlotId center = PlotGeometry.getPlotId(player.getLocation());
            if (center == null) center = new PlotId(0, 0);
            
            int radius = 1;
            while (entries.size() < 50 && radius < 25) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.abs(dx) == radius || Math.abs(dz) == radius) {
                            PlotId pid = new PlotId(center.getX() + dx, center.getZ() + dz);
                            Plot p = plugin.getPlotAPI().getPlot(player.getWorld().getName(), pid);
                            if (p == null || !p.isClaimed()) {
                                entries.add(ChatColor.YELLOW + "Plot " + pid + " (Unclaimed)");
                            }
                        }
                    }
                }
                radius++;
            }
        } else if (filter.equalsIgnoreCase("all")) {
            for (Plot p : plugin.getPlotAPI().getAllPlots()) {
                if (p.isClaimed()) {
                    String ownerName = plugin.getUuidCache().getName(p.getOwner());
                    String nameText = p.hasFlag("display-name") ? " (\"" + p.getFlagValue("display-name") + "\")" : "";
                    entries.add(ChatColor.YELLOW + "- Plot " + p.getId() + " in " + p.getWorld() + " (Owner: " + ownerName + ")" + nameText);
                }
            }
        } else {
            UUID targetUuid = plugin.getUuidCache().getUUID(filter);
            if (targetUuid == null) {
                player.sendMessage(ChatColor.RED + "Player " + filter + " not found or invalid filter.");
                return;
            }
            for (Plot p : plugin.getPlotAPI().getAllPlots()) {
                if (p.isClaimed() && p.getOwner().equals(targetUuid)) {
                    String nameText = p.hasFlag("display-name") ? " (\"" + p.getFlagValue("display-name") + "\")" : "";
                    entries.add(ChatColor.YELLOW + "- Plot " + p.getId() + " in " + p.getWorld() + nameText);
                }
            }
        }

        if (entries.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No plots found matching your criteria.");
            return;
        }

        int pageSize = 10;
        int totalPages = (int) Math.ceil((double) entries.size() / pageSize);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        player.sendMessage(ChatColor.GOLD + "=== Plots List (Page " + page + " of " + totalPages + ") ===");
        int startIdx = (page - 1) * pageSize;
        int endIdx = Math.min(startIdx + pageSize, entries.size());
        for (int i = startIdx; i < endIdx; i++) {
            player.sendMessage(entries.get(i));
        }
    }

    private void handleSetOwnerForcibly(Player player, String[] args) {
        if (!player.hasPermission("plots.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /plot setowner <player> | /plot setowner <world> <x> <z> <player>");
            return;
        }

        Plot plot = null;
        UUID targetUuid = null;
        String targetName = null;

        if (args.length == 2) {
            plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
            if (plot == null) {
                player.sendMessage(ChatColor.RED + "You must stand inside a claimed plot to change its owner.");
                return;
            }
            targetName = args[1];
            targetUuid = plugin.getUuidCache().getUUID(targetName);
        } else if (args.length >= 5) {
            String worldName = args[1];
            try {
                int x = Integer.parseInt(args[2]);
                int z = Integer.parseInt(args[3]);
                plot = plugin.getPlotAPI().getPlot(worldName, new PlotId(x, z));
                if (plot == null || !plot.isClaimed()) {
                    player.sendMessage(ChatColor.RED + "No claimed plot found at " + worldName + " (" + x + "," + z + ").");
                    return;
                }
                targetName = args[4];
                targetUuid = plugin.getUuidCache().getUUID(targetName);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid coordinate numbers.");
                return;
            }
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /plot setowner <player> | /plot setowner <world> <x> <z> <player>");
            return;
        }

        if (targetUuid == null) {
            player.sendMessage(ChatColor.RED + "Player " + targetName + " not found.");
            return;
        }

        Set<PlotId> cluster = getMergedCluster(plot.getWorld(), plot.getId());
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        for (PlotId pid : cluster) {
            Plot p = plugin.getPlotAPI().getPlot(plot.getWorld(), pid);
            if (p != null) {
                p.setOwner(targetUuid);
                final Plot finalP = p;
                future = future.thenCompose(v -> plugin.getDatabaseManager().savePlotOwner(finalP));
            }
        }

        UUID finalTargetUuid = targetUuid;
        String finalTargetName = targetName;
        String finalWorld = plot.getWorld();
        PlotId finalId = plot.getId();
        future.thenRun(() -> {
            player.sendMessage(ChatColor.GREEN + "Forcibly reassigned ownership of plot cluster starting at " + 
                               finalWorld + " " + finalId + " to " + finalTargetName + " safely!");
            Player target = Bukkit.getPlayer(finalTargetUuid);
            if (target != null) {
                target.sendMessage(ChatColor.GREEN + "An administrator has reassigned a plot cluster to you.");
            }
        });
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("plots.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return;
        }
        plugin.reloadPlugin();
        player.sendMessage(ChatColor.GREEN + "SimplePlots configuration and messages hot-reloaded successfully!");
    }

    private void handleMassUnclaim(Player player, String[] args) {
        if (!player.hasPermission("plots.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /plot massunclaim <player>");
            return;
        }

        String targetName = args[1];
        UUID targetUuid = plugin.getUuidCache().getUUID(targetName);
        if (targetUuid == null) {
            com.simpleplots.util.Messages.send(player, "commands.player-not-found", "{name}", targetName);
            return;
        }

        List<PlotId> heads = getPlayerClusterHeads(targetUuid);
        if (heads.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + targetName + " does not own any plots.");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Sweeping and unclaiming all plots owned by " + targetName + " (" + heads.size() + " clusters)...");

        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        String worldName = player.getWorld().getName();
        for (Plot p : plugin.getPlotAPI().getAllPlots()) {
            if (p.getOwner().equals(targetUuid)) {
                worldName = p.getWorld();
                break;
            }
        }

        final String finalWorldName = worldName;
        for (PlotId pid : heads) {
            future = future.thenCompose(v -> plugin.getPlotAPI().unclaimPlotCluster(finalWorldName, pid));
        }

        future.thenRun(() -> {
            player.sendMessage(ChatColor.GREEN + "Successfully mass-unclaimed and cleared " + heads.size() + " plot clusters owned by " + targetName + "!");
        });
    }

    private void handleRate(Player player, String[] args) {
        if (!checkPermission(player, "plots.rate")) return;

        int minRating = plugin.getConfig().getInt("ratings.min-rating", 1);
        int maxRating = plugin.getConfig().getInt("ratings.max-rating", 5);

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /plot rate <" + minRating + "-" + maxRating + ">");
            return;
        }

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            com.simpleplots.util.Messages.send(player, "commands.not-standing-in-plot");
            return;
        }

        if (plot.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot rate your own plot.");
            return;
        }

        int rating;
        try {
            rating = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Rating must be an integer between " + minRating + " and " + maxRating + ".");
            return;
        }

        if (rating < minRating || rating > maxRating) {
            player.sendMessage(ChatColor.RED + "Rating must be between " + minRating + " and " + maxRating + ".");
            return;
        }

        UUID uuid = player.getUniqueId();
        plugin.getDatabaseManager().savePlotRating(plot.getWorld(), plot.getId(), uuid, rating).thenRun(() -> {
            plot.getRatings().put(uuid, rating);
            player.sendMessage(ChatColor.GREEN + "Successfully rated this plot " + rating + "/" + maxRating + "!");
        });
    }

    private void handleComments(Player player, String[] args) {
        if (!checkPermission(player, "plots.comments")) return;

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            com.simpleplots.util.Messages.send(player, "commands.not-standing-in-plot");
            return;
        }

        String sub = args.length >= 2 ? args[1].toLowerCase() : "read";
        int maxLength = plugin.getConfig().getInt("comments.max-length", 100);
        int pageSize = plugin.getConfig().getInt("comments.page-size", 5);

        if (sub.equals("add")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Usage: /plot comments add <message>");
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            String text = sb.toString().trim();
            if (text.length() > maxLength) {
                player.sendMessage(ChatColor.RED + "Comments cannot be longer than " + maxLength + " characters.");
                return;
            }

            plugin.getDatabaseManager().addPlotComment(plot.getWorld(), plot.getId(), player.getUniqueId(), player.getName(), text).thenRun(() -> {
                player.sendMessage(ChatColor.GREEN + "Comment added successfully!");
            });
        } else if (sub.equals("clear")) {
            if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
                com.simpleplots.util.Messages.send(player, "commands.not-owner");
                return;
            }
            plugin.getDatabaseManager().clearPlotComments(plot.getWorld(), plot.getId()).thenRun(() -> {
                player.sendMessage(ChatColor.GREEN + "All comments for this plot cleared successfully!");
            });
        } else {
            int page = 1;
            if (args.length >= 2 && !args[1].equalsIgnoreCase("read")) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored) {}
            } else if (args.length >= 3) {
                try {
                    page = Integer.parseInt(args[2]);
                } catch (NumberFormatException ignored) {}
            }

            final int finalPage = page;
            plugin.getDatabaseManager().getPlotComments(plot.getWorld(), plot.getId()).thenAccept(comments -> {
                if (comments.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "This plot has no comments yet.");
                    return;
                }

                int totalPages = (int) Math.ceil((double) comments.size() / pageSize);
                int currentPage = Math.max(1, Math.min(finalPage, totalPages));

                player.sendMessage(ChatColor.GOLD + "=== Comments for Plot " + plot.getId() + " (Page " + currentPage + " of " + totalPages + ") ===");
                int start = (currentPage - 1) * pageSize;
                int end = Math.min(start + pageSize, comments.size());

                for (int i = start; i < end; i++) {
                    com.simpleplots.api.PlotComment c = comments.get(i);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
                    String date = sdf.format(new java.util.Date(c.getTimestamp()));
                    player.sendMessage(ChatColor.GRAY + "[" + date + "] " + ChatColor.YELLOW + c.getCommenterName() + ": " + ChatColor.WHITE + c.getCommentText());
                }
            });
        }
    }

    private void handleCopy(Player player) {
        if (!checkPermission(player, "plots.copy")) return;

        if (plugin.getServer().getPluginManager().getPlugin("WorldEdit") == null) {
            player.sendMessage(ChatColor.RED + "WorldEdit is required to copy/paste plots.");
            return;
        }

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            com.simpleplots.util.Messages.send(player, "commands.not-standing-in-plot");
            return;
        }

        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            com.simpleplots.util.Messages.send(player, "commands.not-owner");
            return;
        }

        if (!player.hasPermission("plots.admin") && plugin.getConfig().getBoolean("copy-paste.prevent-container-dupes", true)) {
            int[] bounds = PlotGeometry.getMergedPlotBounds(plot.getWorld(), plot.getId());
            if (hasContainerWithItems(player.getWorld(), bounds)) {
                player.sendMessage(ChatColor.RED + "You cannot copy a plot containing chests or containers with items! Please empty them first.");
                return;
            }
        }

        player.sendMessage(ChatColor.YELLOW + "Copying plot layout...");
        String schemName = "copy_" + player.getUniqueId() + ".schem";
        boolean copyEntities = player.hasPermission("plots.admin") || plugin.getConfig().getBoolean("copy-paste.copy-entities", false);

        plugin.getSchematicHandler().savePlotSchematic(plot.getWorld(), plot.getId(), schemName, copyEntities).thenAccept(file -> {
            if (file == null) {
                player.sendMessage(ChatColor.RED + "Failed to copy plot blocks. Make sure WorldEdit is enabled.");
            } else {
                player.sendMessage(ChatColor.GREEN + "Plot layout copied successfully! Stand on another plot you own and type /plot paste.");
            }
        });
    }

    private void handlePaste(Player player) {
        if (!checkPermission(player, "plots.paste")) return;

        if (plugin.getServer().getPluginManager().getPlugin("WorldEdit") == null) {
            player.sendMessage(ChatColor.RED + "WorldEdit is required to copy/paste plots.");
            return;
        }

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            com.simpleplots.util.Messages.send(player, "commands.not-standing-in-plot");
            return;
        }

        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            com.simpleplots.util.Messages.send(player, "commands.not-owner");
            return;
        }

        File schemFolder = new File(plugin.getDataFolder(), "schematics");
        File schemFile = new File(schemFolder, "copy_" + player.getUniqueId() + ".schem");
        if (!schemFile.exists()) {
            player.sendMessage(ChatColor.RED + "You haven't copied a plot yet! Use /plot copy first.");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Pasting plot layout...");
        plugin.getSchematicHandler().pastePlotSchematic(plot.getWorld(), plot.getId(), schemFile).thenRun(() -> {
            player.sendMessage(ChatColor.GREEN + "Plot layout pasted successfully!");
        });
    }

    private void handleMiddle(Player player) {
        if (!checkPermission(player, "plots.middle")) return;

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            com.simpleplots.util.Messages.send(player, "commands.not-standing-in-plot");
            return;
        }

        Location middleLoc = getClusterCenterLocation(plot.getWorld(), plot.getId());
        if (middleLoc != null) {
            player.teleport(middleLoc);
            player.sendMessage(ChatColor.GREEN + "Teleported to the exact center of the plot cluster!");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to calculate center location.");
        }
    }

    private Location getClusterCenterLocation(String worldName, PlotId startId) {
        Set<PlotId> cluster = getMergedCluster(worldName, startId);
        int minPlotX = Integer.MAX_VALUE, minPlotZ = Integer.MAX_VALUE;
        int maxPlotX = Integer.MIN_VALUE, maxPlotZ = Integer.MIN_VALUE;
        for (PlotId pid : cluster) {
            if (pid.getX() < minPlotX) minPlotX = pid.getX();
            if (pid.getZ() < minPlotZ) minPlotZ = pid.getZ();
            if (pid.getX() > maxPlotX) maxPlotX = pid.getX();
            if (pid.getZ() > maxPlotZ) maxPlotZ = pid.getZ();
        }
        
        int[] minBounds = PlotGeometry.getPlotBounds(worldName, new PlotId(minPlotX, minPlotZ));
        int[] maxBounds = PlotGeometry.getPlotBounds(worldName, new PlotId(maxPlotX, maxPlotZ));
        
        double centerX = (minBounds[0] + maxBounds[2]) / 2.0;
        double centerZ = (minBounds[1] + maxBounds[3]) / 2.0;
        
        PlotWorldConfig worldConfig = plugin.getWorldConfig(worldName);
        World world = Bukkit.getWorld(worldName);
        if (world == null || worldConfig == null) return null;
        
        return new Location(world, centerX, worldConfig.getFloorHeight() + 1, centerZ);
    }

    private static final Set<Material> CONTAINER_MATERIALS = new HashSet<>(Arrays.asList(
        Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
        Material.DISPENSER, Material.DROPPER, Material.HOPPER,
        Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
        Material.BREWING_STAND, Material.SHULKER_BOX,
        Material.BLACK_SHULKER_BOX, Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX,
        Material.CYAN_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.GREEN_SHULKER_BOX,
        Material.LIGHT_BLUE_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX, Material.LIME_SHULKER_BOX,
        Material.MAGENTA_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.PINK_SHULKER_BOX,
        Material.PURPLE_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.WHITE_SHULKER_BOX,
        Material.YELLOW_SHULKER_BOX
    ));

    private boolean hasContainerWithItems(World world, int[] bounds) {
        for (int x = bounds[0]; x <= bounds[2]; x++) {
            for (int z = bounds[1]; z <= bounds[3]; z++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    Material type = world.getBlockAt(x, y, z).getType();
                    if (CONTAINER_MATERIALS.contains(type)) {
                        org.bukkit.block.BlockState state = world.getBlockAt(x, y, z).getState();
                        if (state instanceof org.bukkit.block.Container) {
                            org.bukkit.block.Container container = (org.bukkit.block.Container) state;
                            if (!container.getInventory().isEmpty()) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private void handleJoinSupportDc(Player player) {
        if (!checkPermission(player, "plots.joinsupportdc")) return;
        
        String url = plugin.getConfig().getString("discord.invite-url", "https://discord.gg/Sz3xJpCGKF");
        String hoverText = plugin.getConfig().getString("discord.hover-text", "&eClick to join our Discord server!");
        String clickMsg = plugin.getConfig().getString("discord.click-message", "&aClick here to join our Support Discord: &b{url}");
        
        String formattedMsg = ChatColor.translateAlternateColorCodes('&', clickMsg.replace("{url}", url));
        String formattedHover = ChatColor.translateAlternateColorCodes('&', hoverText);
        
        net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(formattedMsg);
        msg.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
            net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, url
        ));
        msg.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
            net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
            new net.md_5.bungee.api.chat.hover.content.Text(formattedHover)
        ));
        
        player.spigot().sendMessage(msg);
    }

    public void handlePlotChest(Player player) {
        List<ItemStack> items = plugin.loadPlotChest(player.getUniqueId());
        if (items.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Your plot chest is empty or all items have expired.");
            return;
        }

        SimplePlots.PlotChestSession session = new SimplePlots.PlotChestSession(items, 1);
        plugin.getActiveChestSessions().put(player.getUniqueId(), session);

        Inventory inv = createPlotChestInventory(player, session);
        player.openInventory(inv);
        player.sendMessage(ChatColor.GREEN + "Opening your Plot Chest... Items will be saved automatically when you close it.");
    }

    public Inventory createPlotChestInventory(Player player, SimplePlots.PlotChestSession session) {
        int totalPages = (int) Math.ceil((double) session.items.size() / 45.0);
        if (totalPages == 0) totalPages = 1;
        
        long expiryHours = plugin.getConfig().getLong("plot-chest.expiry-hours", 168);
        long expiryDays = expiryHours / 24;
        String title = ChatColor.translateAlternateColorCodes('&', 
            "&6Plot Chest (" + expiryDays + "d expiry) - Pg " + session.page + "/" + totalPages);
        
        Inventory inv = Bukkit.createInventory(player, 54, title);
        
        int startIndex = (session.page - 1) * 45;
        for (int i = 0; i < 45; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex < session.items.size()) {
                inv.setItem(i, session.items.get(itemIndex));
            }
        }
        
        // Build control bar (slots 45-53)
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        for (int i = 45; i <= 53; i++) {
            inv.setItem(i, pane);
        }
        
        // Prev button
        if (session.page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            org.bukkit.inventory.meta.ItemMeta m = prev.getItemMeta();
            if (m != null) {
                m.setDisplayName(ChatColor.YELLOW + "<- Previous Page");
                prev.setItemMeta(m);
            }
            inv.setItem(45, prev);
        }
        
        // Next button
        if (startIndex + 45 < session.items.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            org.bukkit.inventory.meta.ItemMeta m = next.getItemMeta();
            if (m != null) {
                m.setDisplayName(ChatColor.YELLOW + "Next Page ->");
                next.setItemMeta(m);
            }
            inv.setItem(53, next);
        }
        
        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        org.bukkit.inventory.meta.ItemMeta cm = close.getItemMeta();
        if (cm != null) {
            cm.setDisplayName(ChatColor.RED + "Close Plot Chest");
            close.setItemMeta(cm);
        }
        inv.setItem(49, close);
        
        return inv;
    }

    public void handleAdminPanel(Player player) {
        boolean buildEnabled = plugin.getConfig().getBoolean("merged-roads.build-enabled", true);
        
        String title = ChatColor.translateAlternateColorCodes('&', "&cAdmin settings panel");
        Inventory inv = Bukkit.createInventory(player, 27, title);
        
        // Filler
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane);
        }
        
        // Toggle Button
        ItemStack toggle = new ItemStack(Material.OAK_SIGN);
        org.bukkit.inventory.meta.ItemMeta tm = toggle.getItemMeta();
        if (tm != null) {
            tm.setDisplayName(ChatColor.YELLOW + "Build on Merged Roads");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Allows players who merged their plots");
            lore.add(ChatColor.GRAY + "to build, decorate, etc. on the roads.");
            lore.add("");
            lore.add(ChatColor.GRAY + "Current Status: " + (buildEnabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to toggle!");
            tm.setLore(lore);
            toggle.setItemMeta(tm);
        }
        inv.setItem(11, toggle);
        
        // Close Button
        ItemStack close = new ItemStack(Material.BARRIER);
        org.bukkit.inventory.meta.ItemMeta cm = close.getItemMeta();
        if (cm != null) {
            cm.setDisplayName(ChatColor.RED + "Close");
            close.setItemMeta(cm);
        }
        inv.setItem(22, close);
        
        player.openInventory(inv);
    }

    private boolean isBorder(int remX, int remZ, int roadWidth, int totalSize) {
        boolean adjX = (remX == roadWidth - 1) || (remX == 0);
        boolean adjZ = (remZ == roadWidth - 1) || (remZ == 0);

        boolean insideX = (remX >= roadWidth - 1) || (remX == 0);
        boolean insideZ = (remZ >= roadWidth - 1) || (remZ == 0);

        return (adjX && insideZ) || (adjZ && insideX);
    }

    private void executeSharedMerge(Player receiverPlayer, SimplePlots.MergeRequest request) {
        Plot plot = plugin.getPlotAPI().getPlot(request.world, request.senderPlot);
        Plot adjacent = plugin.getPlotAPI().getPlot(request.world, request.receiverPlot);

        if (plot == null || adjacent == null) return;

        // Determine direction from senderPlot to receiverPlot
        PlotId currentId = request.senderPlot;
        PlotId adjacentId = request.receiverPlot;
        
        int dx = adjacentId.getX() - currentId.getX();
        int dz = adjacentId.getZ() - currentId.getZ();
        
        if (dx == 1) {
            plot.setMergedE(true);
            adjacent.setMergedW(true);
        } else if (dx == -1) {
            plot.setMergedW(true);
            adjacent.setMergedE(true);
        } else if (dz == 1) {
            plot.setMergedS(true);
            adjacent.setMergedN(true);
        } else if (dz == -1) {
            plot.setMergedN(true);
            adjacent.setMergedS(true);
        }

        // Auto-merge any adjacent plots within the same cluster to prevent left-over roads
        java.util.Set<PlotId> cluster = plugin.getPlotAPI().getMergedCluster(request.world, currentId);
        for (PlotId pid : cluster) {
            Plot p = plugin.getPlotAPI().getPlot(request.world, pid);
            if (p == null) continue;

            // Check East neighbor
            PlotId eastId = new PlotId(pid.getX() + 1, pid.getZ());
            if (cluster.contains(eastId)) {
                Plot eastPlot = plugin.getPlotAPI().getPlot(request.world, eastId);
                if (eastPlot != null) {
                    p.setMergedE(true);
                    eastPlot.setMergedW(true);
                }
            }

            // Check South neighbor
            PlotId southId = new PlotId(pid.getX(), pid.getZ() + 1);
            if (cluster.contains(southId)) {
                Plot southPlot = plugin.getPlotAPI().getPlot(request.world, southId);
                if (southPlot != null) {
                    p.setMergedS(true);
                    southPlot.setMergedN(true);
                }
            }
        }

        // Save all updated plots in the cluster to the database
        for (PlotId pid : cluster) {
            Plot p = plugin.getPlotAPI().getPlot(request.world, pid);
            if (p != null) {
                plugin.getDatabaseManager().savePlotOwner(p);
            }
        }

        receiverPlayer.sendMessage(ChatColor.GREEN + "Plots merged successfully. Clearing road async...");
        Player senderPlayer = Bukkit.getPlayer(request.sender);
        if (senderPlayer != null && senderPlayer.isOnline()) {
            senderPlayer.sendMessage(ChatColor.GREEN + "Your merge request has been accepted! Clearing road async...");
        }

        // Clear road blocks within the entire cluster bounds on the main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            com.simpleplots.generator.PlotWorldConfig cfg = plugin.getWorldConfig(request.world);
            if (cfg == null) return;
            int totalSize = cfg.getTotalSize();
            int roadWidth = cfg.getRoadWidth();
            int halfRoad = roadWidth / 2;
            int floorHeight = cfg.getFloorHeight();

            // Calculate bounding box of the ENTIRE merged cluster
            int minPlotX = Integer.MAX_VALUE, minPlotZ = Integer.MAX_VALUE;
            int maxPlotX = Integer.MIN_VALUE, maxPlotZ = Integer.MIN_VALUE;
            for (PlotId pid : cluster) {
                if (pid.getX() < minPlotX) minPlotX = pid.getX();
                if (pid.getZ() < minPlotZ) minPlotZ = pid.getZ();
                if (pid.getX() > maxPlotX) maxPlotX = pid.getX();
                if (pid.getZ() > maxPlotZ) maxPlotZ = pid.getZ();
            }

            int[] minBounds = PlotGeometry.getPlotBounds(request.world, new PlotId(minPlotX, minPlotZ));
            int[] maxBounds = PlotGeometry.getPlotBounds(request.world, new PlotId(maxPlotX, maxPlotZ));

            int minX = minBounds[0] - roadWidth;
            int maxX = maxBounds[2] + roadWidth;
            int minZ = minBounds[1] - roadWidth;
            int maxZ = maxBounds[3] + roadWidth;

            World world = Bukkit.getWorld(request.world);
            if (world == null) return;

            Material filler = cfg.getFillerBlock();

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int shiftedX = x + halfRoad;
                    int shiftedZ = z + halfRoad;
                    int remX = Math.floorMod(shiftedX, totalSize);
                    int remZ = Math.floorMod(shiftedZ, totalSize);
                    boolean originalRoad = (remX < roadWidth) || (remZ < roadWidth);

                    if (originalRoad && !PlotGeometry.isRoad(request.world, x, z)) {
                        // Scan for containers to transfer items to Plot Chest of the respective cell owner
                        List<ItemStack> itemsToSave = new ArrayList<>();
                        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                            Block block = world.getBlockAt(x, y, z);
                            if (block.getType() != Material.AIR) {
                                org.bukkit.block.BlockState state = block.getState();
                                if (state instanceof org.bukkit.block.Container) {
                                    org.bukkit.block.Container container = (org.bukkit.block.Container) state;
                                    ItemStack[] contents = container.getInventory().getContents();
                                    for (ItemStack item : contents) {
                                        if (item != null && item.getType() != Material.AIR) {
                                            itemsToSave.add(item.clone());
                                        }
                                    }
                                    container.getInventory().clear();
                                }
                            }
                        }
                        
                        // Find closest plot cell in cluster to this road block, and save items to its owner's chest
                        if (!itemsToSave.isEmpty()) {
                            PlotId closestId = getClosestPlotId(pid -> PlotGeometry.getPlotBounds(request.world, pid), cluster, x, z);
                            Plot closestPlot = plugin.getPlotAPI().getPlot(request.world, closestId);
                            UUID targetOwner = closestPlot != null ? closestPlot.getOwner() : plot.getOwner();
                            plugin.saveToPlotChest(targetOwner, itemsToSave);
                        }

                        // Bedrock
                        world.setType(x, 0, z, cfg.getBedBlock());
                        
                        // Filler
                        for (int y = 1; y < floorHeight; y++) {
                            world.setType(x, y, z, filler);
                        }
                        
                        // Surface (replace only if it's default road/border/wall/air/dirt)
                        Material typeAtFloor = world.getBlockAt(x, floorHeight, z).getType();
                        if (typeAtFloor == cfg.getRoadBlock() || typeAtFloor == cfg.getBorderBlock() || 
                            typeAtFloor == cfg.getWallBlockUnclaimed() || typeAtFloor == Material.AIR || 
                            typeAtFloor == Material.DIRT || typeAtFloor == Material.GRASS_BLOCK) {
                            world.setType(x, floorHeight, z, cfg.getFloorBlock());
                        }

                        // Clear air above
                        for (int y = floorHeight + 1; y < world.getMaxHeight(); y++) {
                            Material type = world.getBlockAt(x, y, z).getType();
                            if (type == cfg.getWallBlockUnclaimed() || type == Material.AIR) {
                                world.setType(x, y, z, Material.AIR);
                            }
                        }
                    }
                }
            }
            PlotGeometry.updatePlotBorders(request.world, currentId);
            PlotGeometry.updatePlotBorders(request.world, adjacentId);
        });
    }

    private PlotId getClosestPlotId(java.util.function.Function<PlotId, int[]> boundsFunc, java.util.Set<PlotId> cluster, int x, int z) {
        PlotId closest = null;
        double minDist = Double.MAX_VALUE;
        for (PlotId pid : cluster) {
            int[] bounds = boundsFunc.apply(pid);
            double cx = (bounds[0] + bounds[2]) / 2.0;
            double cz = (bounds[1] + bounds[3]) / 2.0;
            double dist = Math.pow(x - cx, 2) + Math.pow(z - cz, 2);
            if (dist < minDist) {
                minDist = dist;
                closest = pid;
            }
        }
        return closest;
    }

    private boolean checkPermission(Player player, String permission) {
        if (player.hasPermission("plots.admin")) {
            return true;
        }
        
        String sub = permission;
        if (sub.startsWith("plots.")) {
            sub = sub.substring(6);
        }
        
        if (plugin.hasPermissionForSubcommand(player, sub)) {
            return true;
        }
        
        com.simpleplots.util.Messages.send(player, "commands.no-permission");
        return false;
    }
}

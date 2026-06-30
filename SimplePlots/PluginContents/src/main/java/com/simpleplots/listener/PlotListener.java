package com.simpleplots.listener;

import com.simpleplots.SimplePlots;
import com.simpleplots.api.Plot;
import com.simpleplots.api.PlotGeometry;
import com.simpleplots.api.PlotId;
import com.simpleplots.commands.PlotCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WeatherType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Main event listener for enforcing plot permissions and flag effects.
 */
public class PlotListener implements Listener {
    private final SimplePlots plugin;
    private final Map<UUID, PlotId> lastPlotLocation = new HashMap<>();

    public PlotListener(SimplePlots plugin) {
        this.plugin = plugin;
    }

    private boolean isPlotWorld(String worldName) {
        return plugin.getWorldConfig(worldName) != null;
    }

    private boolean isPhysicallyRoad(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        com.simpleplots.generator.PlotWorldConfig config = plugin.getWorldConfig(loc.getWorld().getName());
        if (config == null) return false;
        int totalSize = config.getTotalSize();
        int roadWidth = config.getRoadWidth();
        int halfRoad = roadWidth / 2;
        int shiftedX = loc.getBlockX() + halfRoad;
        int shiftedZ = loc.getBlockZ() + halfRoad;
        int remX = Math.floorMod(shiftedX, totalSize);
        int remZ = Math.floorMod(shiftedZ, totalSize);
        return (remX < roadWidth) || (remZ < roadWidth);
    }
    private boolean isSameCluster(String worldName, PlotId id1, PlotId id2) {
        if (id1 == null || id2 == null) return false;
        if (id1.equals(id2)) return true;
        java.util.Set<PlotId> cluster = plugin.getPlotAPI().getMergedCluster(worldName, id1);
        return cluster.contains(id2);
    }
    private boolean checkBuildPermission(Player player, Location loc) {
        if (!isPlotWorld(loc.getWorld().getName())) return true;
        
        // Admin bypass
        if (player.hasPermission("plots.admin") || player.hasPermission("plots.admin.bypass")) {
            return true;
        }

        // Road check
        if (PlotGeometry.isRoad(loc)) {
            return false;
        }

        // Check if building on merged roads is disabled by admin
        if (isPhysicallyRoad(loc)) {
            boolean buildOnMergedRoadsEnabled = plugin.getConfig().getBoolean("merged-roads.build-enabled", true);
            if (!buildOnMergedRoadsEnabled) {
                return false;
            }
        }

        PlotId plotId = PlotGeometry.getPlotId(loc);
        if (plotId == null) return false;

        Plot plot = plugin.getPlotAPI().getPlot(loc.getWorld().getName(), plotId);
        if (plot == null) {
            return false; // Unclaimed plot cell
        }

        // Check if player is owner, trusted, or added in any plot within the merged cluster
        java.util.Set<PlotId> cluster = plugin.getPlotAPI().getMergedCluster(loc.getWorld().getName(), plotId);
        for (PlotId pid : cluster) {
            Plot p = plugin.getPlotAPI().getPlot(loc.getWorld().getName(), pid);
            if (p != null) {
                if (p.getOwner().equals(player.getUniqueId())) {
                    return true;
                }
                if (p.isTrusted(player.getUniqueId())) {
                    return true;
                }
                if (p.isAdded(player.getUniqueId())) {
                    Player owner = Bukkit.getPlayer(p.getOwner());
                    if (owner != null && owner.isOnline()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!checkBuildPermission(event.getPlayer(), event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to build here.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!checkBuildPermission(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to build here.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) return;
        
        Location loc = event.getClickedBlock().getLocation();
        if (!isPlotWorld(loc.getWorld().getName())) return;

        // Physical interactions (trampling crops, pressure plates)
        if (event.getAction() == Action.PHYSICAL) {
            if (!checkBuildPermission(player, loc)) {
                event.setCancelled(true);
            }
            return;
        }

        // Run general build check for container interactions / placement
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Material type = event.getClickedBlock().getType();
            if (type == Material.LECTERN && plotHasFlag(loc, "lectern-read-book", "false")) {
                if (!player.hasPermission("plots.admin.bypass.flags")) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Reading books from lecterns is disabled on this plot.");
                    return;
                }
            }

            // Normal container/interact protections
            if (!checkBuildPermission(player, loc)) {
                // Allow simple non-container items unless it's block-changing
                if (type.isInteractable()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getRightClicked().getLocation();
        if (!isPlotWorld(loc.getWorld().getName())) return;

        if (event.getRightClicked() instanceof Villager && plotHasFlag(loc, "villager-interact", "false")) {
            if (!player.hasPermission("plots.admin.bypass.flags")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Interacting with villagers is disabled on this plot.");
                return;
            }
        }

        if (!checkBuildPermission(player, loc)) {
            event.setCancelled(true);
        }
    }

    private boolean plotHasFlag(Location loc, String flagName, String expectedValue) {
        PlotId pid = PlotGeometry.getPlotId(loc);
        if (pid == null) return false;
        Plot plot = plugin.getPlotAPI().getPlot(loc.getWorld().getName(), pid);
        if (plot == null) return false;
        String val = plot.getFlagValue(flagName);
        return val != null && val.equalsIgnoreCase(expectedValue);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        
        if (to.getBlockX() == from.getBlockX() && to.getBlockY() == from.getBlockY() && to.getBlockZ() == from.getBlockZ()) {
            return;
        }

        if (!isPlotWorld(to.getWorld().getName())) return;

        PlotId toPlotId = PlotGeometry.getPlotId(to);
        PlotId fromPlotId = lastPlotLocation.get(player.getUniqueId());

        if (toPlotId != null && !PlotGeometry.isRoad(to)) {
            // Player is inside a plot
            Plot toPlot = plugin.getPlotAPI().getPlot(to.getWorld().getName(), toPlotId);
            
            // Enforce deny flag
            if (toPlot != null && toPlot.isDenied(player.getUniqueId())) {
                if (!player.hasPermission("plots.admin") && !player.hasPermission("plots.admin.bypass")) {
                    event.setCancelled(true);
                    player.teleport(from);
                    player.sendMessage(ChatColor.RED + "You are denied from entering this plot.");
                    return;
                }
            }

            // Enforce elytra flight restriction
            if (plugin.getConfig().getBoolean("anti-grief.elytra-flyover-restriction", true) &&
                player.isGliding() && toPlot != null && toPlot.hasFlag("elytra") && toPlot.getFlagValue("elytra").equalsIgnoreCase("false")) {
                if (!toPlot.getOwner().equals(player.getUniqueId()) &&
                    !toPlot.isTrusted(player.getUniqueId()) &&
                    !toPlot.isAdded(player.getUniqueId()) &&
                    !player.hasPermission("plots.admin")) {
                    player.setGliding(false);
                    player.sendMessage(ChatColor.RED + "Elytra flight is disabled in this plot's airspace.");
                }
            }

            // Enforce crawling/trapdoor entry prevention
            if (plugin.getConfig().getBoolean("anti-grief.crawl-entry-prevention", true) &&
                toPlot != null && !toPlot.isTrusted(player.getUniqueId()) &&
                !toPlot.isAdded(player.getUniqueId()) &&
                !toPlot.getOwner().equals(player.getUniqueId()) &&
                !player.hasPermission("plots.admin")) {
                
                if (player.getPose() == org.bukkit.entity.Pose.SWIMMING) {
                    event.setCancelled(true);
                    player.teleport(from);
                    player.sendMessage(ChatColor.RED + "Crawling or swimming is not allowed on plots you do not own.");
                    return;
                }
            }

            // Enforce entry transitions
            if (fromPlotId == null || !isSameCluster(to.getWorld().getName(), fromPlotId, toPlotId)) {
                // Entered plot / new cluster
                com.simpleplots.generator.PlotWorldConfig worldConfig = plugin.getWorldConfig(to.getWorld().getName());
                
                if (toPlot != null && toPlot.isClaimed()) {
                    // Send claimed message
                    String ownerName = plugin.getUuidCache().getName(toPlot.getOwner());
                    String template = (worldConfig != null) ? worldConfig.getEntryMessageClaimed() : "&7Entering {owner} Plot";
                    String msg = ChatColor.translateAlternateColorCodes('&', template.replace("{owner}", ownerName));
                    player.sendTitle(msg, "", 10, 40, 10);

                    // Time flag
                    if (toPlot.hasFlag("time")) {
                        try {
                            long time = Long.parseLong(toPlot.getFlagValue("time"));
                            player.setPlayerTime(time, false);
                        } catch (NumberFormatException ignored) {}
                    }
                    
                    // Weather flag
                    if (toPlot.hasFlag("weather")) {
                        String weather = toPlot.getFlagValue("weather").toUpperCase();
                        if (weather.equals("RAIN") || weather.equals("DOWNFALL")) {
                            player.setPlayerWeather(WeatherType.DOWNFALL);
                        } else {
                            player.setPlayerWeather(WeatherType.CLEAR);
                        }
                    }

                    // Fly flag
                    if (toPlot.hasFlag("fly")) {
                        boolean fly = toPlot.getFlagValue("fly").equalsIgnoreCase("true");
                        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                            player.setAllowFlight(fly);
                        }
                    }

                    // Gamemode flag
                    if (toPlot.hasFlag("gamemode")) {
                        try {
                            GameMode gm = GameMode.valueOf(toPlot.getFlagValue("gamemode").toUpperCase());
                            player.setGameMode(gm);
                        } catch (IllegalArgumentException ignored) {}
                    }

                    // Greeting message
                    if (toPlot.hasFlag("greeting")) {
                        String greeting = ChatColor.translateAlternateColorCodes('&', toPlot.getFlagValue("greeting"));
                        player.sendTitle("", greeting, 10, 40, 10);
                    }

                    // Music flag
                    if (toPlot.hasFlag("music")) {
                        try {
                            Sound sound = Sound.valueOf(toPlot.getFlagValue("music").toUpperCase());
                            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                        } catch (IllegalArgumentException ignored) {}
                    }
                } else {
                    // Send unclaimed message
                    String template = (worldConfig != null) ? worldConfig.getEntryMessageUnclaimed() : "&7Entering Unclaimed Plot";
                    String msg = ChatColor.translateAlternateColorCodes('&', template);
                    player.sendTitle(msg, "", 10, 40, 10);
                }
            }
            // Always update last known plot cell location
            lastPlotLocation.put(player.getUniqueId(), toPlotId);
        } else {
            // Player is on road or outside plots
            if (fromPlotId != null) {
                // Exited plot
                lastPlotLocation.remove(player.getUniqueId());
                player.resetPlayerTime();
                player.resetPlayerWeather();
                
                Plot fromPlot = plugin.getPlotAPI().getPlot(from.getWorld().getName(), fromPlotId);

                // Revert flight to default unless creative (only if exiting a plot that changed fly status)
                if (fromPlot != null && fromPlot.hasFlag("fly")) {
                    if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                        player.setAllowFlight(false);
                        player.setFlying(false);
                    }
                }

                // Gamemode revert (only if exiting a plot that changed gamemode)
                if (fromPlot != null && fromPlot.hasFlag("gamemode")) {
                    if (player.getGameMode() != GameMode.SURVIVAL) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                }

                // Farewell message
                if (fromPlot != null && fromPlot.hasFlag("farewell")) {
                    String farewell = ChatColor.translateAlternateColorCodes('&', fromPlot.getFlagValue("farewell"));
                    player.sendActionBar(farewell);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastPlotLocation.remove(event.getPlayer().getUniqueId());
    }

    // --- Flag Enforcement Event Handlers ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Location loc = event.getEntity().getLocation();
        if (!isPlotWorld(loc.getWorld().getName())) return;

        Player player = null;
        if (event.getDamager() instanceof Player) {
            player = (Player) event.getDamager();
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
            org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                player = (Player) proj.getShooter();
            }
        }

        if (player != null) {
            if (!checkBuildPermission(player, loc)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You do not have permission to damage entities on this plot.");
                return;
            }

            if (event.getEntity() instanceof Player) {
                // PVP flag check
                if (plotHasFlag(loc, "pvp", "false")) {
                    if (!player.hasPermission("plots.admin.bypass.flags")) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "PVP is disabled on this plot.");
                    }
                }
            } else {
                // PVE flag check
                if (plotHasFlag(loc, "pve", "false")) {
                    if (!player.hasPermission("plots.admin.bypass.flags")) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "PVE (harming passive/hostile mobs) is disabled on this plot.");
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent event) {
        if (!checkBuildPermission(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to place liquids here.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(org.bukkit.event.player.PlayerBucketFillEvent event) {
        if (!checkBuildPermission(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to collect liquids here.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(org.bukkit.event.block.BlockIgniteEvent event) {
        if (event.getPlayer() != null) {
            if (!checkBuildPermission(event.getPlayer(), event.getBlock().getLocation())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to ignite blocks here.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandManipulate(org.bukkit.event.player.PlayerArmorStandManipulateEvent event) {
        if (!checkBuildPermission(event.getPlayer(), event.getRightClicked().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to manipulate armor stands here.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(org.bukkit.event.hanging.HangingPlaceEvent event) {
        if (event.getPlayer() != null) {
            if (!checkBuildPermission(event.getPlayer(), event.getEntity().getLocation())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to place decorations here.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(org.bukkit.event.hanging.HangingBreakByEntityEvent event) {
        Player player = null;
        if (event.getRemover() instanceof Player) {
            player = (Player) event.getRemover();
        } else if (event.getRemover() instanceof org.bukkit.entity.Projectile) {
            org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) event.getRemover();
            if (proj.getShooter() instanceof Player) {
                player = (Player) proj.getShooter();
            }
        }

        if (player != null) {
            if (!checkBuildPermission(player, event.getEntity().getLocation())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "You do not have permission to break decorations here.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Location loc = event.getLocation();
        if (!isPlotWorld(loc.getWorld().getName())) return;

        if (plotHasFlag(loc, "explosion", "false")) {
            event.setCancelled(true);
            return;
        }

        // Clean up block lists that are in plots with explosion disabled
        event.blockList().removeIf(block -> {
            Location blockLoc = block.getLocation();
            return plotHasFlag(blockLoc, "explosion", "false");
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!isPlotWorld(loc.getWorld().getName())) return;

        if (plotHasFlag(loc, "explosion", "false")) {
            event.setCancelled(true);
            return;
        }

        event.blockList().removeIf(block -> {
            Location blockLoc = block.getLocation();
            return plotHasFlag(blockLoc, "explosion", "false");
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!isPlotWorld(loc.getWorld().getName())) return;

        Material type = event.getBlock().getType();
        if (type == Material.SNOW || type == Material.ICE || type == Material.PACKED_ICE) {
            if (plotHasFlag(loc, "snow-melt", "false")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!isPlotWorld(loc.getWorld().getName())) return;

        if (plotHasFlag(loc, "leaf-decay", "false")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!isPlotWorld(loc.getWorld().getName())) return;

        if (plotHasFlag(loc, "crop-grow", "false")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        Location loc = event.getBlocks().get(0).getLocation();
        if (!isPlotWorld(loc.getWorld().getName())) return;

        if (plotHasFlag(loc, "no-portals", "true")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Location loc = event.getFrom();
        if (!isPlotWorld(loc.getWorld().getName())) return;

        if (plotHasFlag(loc, "deny-portal-travel", "true")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Portal travel is disabled on this plot.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!isPlotWorld(loc.getWorld().getName())) return;

        if (plotHasFlag(loc, "redstone", "false")) {
            event.setNewCurrent(0); // Cut off power
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!isPlotWorld(loc.getWorld().getName())) return;

        if (plotHasFlag(loc, "entity-change-block", "false")) {
            event.setCancelled(true);
        }
    }

    // --- Plot Chat Hook ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getPlotChatEnabled().contains(player.getUniqueId())) {
            return;
        }

        // Chatting in plot chat
        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "You are not standing on a claimed plot. Plot chat disabled.");
            plugin.getPlotChatEnabled().remove(player.getUniqueId());
            return;
        }

        event.setCancelled(true);

        String format = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfig().getString("chat.format", "&8[&aPlot Chat&8] &7{player}: &f{message}")
                .replace("{player}", player.getName())
                .replace("{message}", event.getMessage())
        );

        // Send to everyone in the same plot cell bounds (including roads if merged)
        int[] bounds = PlotGeometry.getMergedPlotBounds(plot.getWorld(), plot.getId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getWorld().getName().equalsIgnoreCase(plot.getWorld())) {
                int ox = online.getLocation().getBlockX();
                int oz = online.getLocation().getBlockZ();
                if (ox >= bounds[0] && ox <= bounds[2] && oz >= bounds[1] && oz <= bounds[3]) {
                    online.sendMessage(format);
                }
            }
        }

        if (plugin.getConfig().getBoolean("chat.log-to-console", true)) {
            Bukkit.getConsoleSender().sendMessage(format);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChatSetup(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getCommand("plot").getExecutor() instanceof PlotCommand) {
            PlotCommand cmd = (PlotCommand) plugin.getCommand("plot").getExecutor();
            if (cmd.handleSetupChat(player, event.getMessage())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(org.bukkit.event.entity.CreatureSpawnEvent event) {
        Location loc = event.getLocation();
        if (isPlotWorld(loc.getWorld().getName())) {
            org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
            if (reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.NATURAL ||
                reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.SPAWNER ||
                reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CHUNK_GEN ||
                reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.DEFAULT ||
                reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.MOUNT ||
                reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.JOCKEY ||
                reason == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.RAID) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFoodLevelChange(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            com.simpleplots.generator.PlotWorldConfig cfg = plugin.getWorldConfig(player.getWorld().getName());
            if (cfg != null && cfg.isInfiniteSaturation()) {
                event.setCancelled(true);
                player.setFoodLevel(20);
                player.setSaturation(20f);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getConfig().getBoolean("inactivity-expiration.enabled", true)) {
            plugin.getDatabaseManager().updatePlayerLastSeen(player.getUniqueId(), System.currentTimeMillis());
            return;
        }

        int expiryDays = plugin.getConfig().getInt("inactivity-expiration.expiry-days", 30);
        int warningDays = plugin.getConfig().getInt("inactivity-expiration.warning-days", 7);
        long now = System.currentTimeMillis();

        plugin.getDatabaseManager().getPlayerLastSeen(player.getUniqueId()).thenAccept(lastSeen -> {
            long lastActive = lastSeen;
            if (lastActive <= 0) {
                lastActive = player.getLastPlayed();
            }
            if (lastActive > 0) {
                long diffMs = now - lastActive;
                long diffDays = diffMs / (1000L * 60 * 60 * 24);
                if (diffDays >= (expiryDays - warningDays)) {
                    long remainingDays = expiryDays - diffDays;
                    if (remainingDays > 0) {
                        String inactiveStr = String.valueOf(diffDays);
                        String remainingStr = String.valueOf(remainingDays);
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            com.simpleplots.util.Messages.send(player, "commands.inactivity-warning",
                                "{inactive}", inactiveStr,
                                "{remaining}", remainingStr
                            );
                        });
                    }
                }
            }
            // Update last seen to now
            plugin.getDatabaseManager().updatePlayerLastSeen(player.getUniqueId(), now);
        });

        // Notify player about pending merge requests
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            int reqCount = 0;
            for (SimplePlots.MergeRequest req : plugin.getPendingMergeRequests()) {
                if (req.receiver.equals(player.getUniqueId())) {
                    reqCount++;
                    String senderName = plugin.getUuidCache().getName(req.sender);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        "&a&l[SimplePlots] &7You have a pending merge request from &e" + senderName + 
                        " &7to merge plot &a" + req.senderPlot + " &7with your plot &a" + req.receiverPlot + "!"));
                }
            }
            if (reqCount > 0) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&a&l[SimplePlots] &7Type &e/plot merge accept &7or &c/plot merge deny &7to respond."));
            }
        }, 60L); // Delay 3 seconds (60 ticks)
    }

    private boolean isSameClusterOrBothUnclaimed(Location loc1, Location loc2) {
        Plot p1 = plugin.getPlotAPI().getPlotAt(loc1);
        Plot p2 = plugin.getPlotAPI().getPlotAt(loc2);
        if (p1 == null && p2 == null) return true;
        if (p1 == null || p2 == null) return false;
        if (!p1.getOwner().equals(p2.getOwner())) return false;
        
        java.util.Set<PlotId> cluster1 = plugin.getPlotAPI().getMergedCluster(p1.getWorld(), p1.getId());
        return cluster1.contains(p2.getId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!plugin.getConfig().getBoolean("anti-grief.liquid-flow-blocking", true)) return;

        Location from = event.getBlock().getLocation();
        Location to = event.getToBlock().getLocation();
        if (!isPlotWorld(from.getWorld().getName())) return;

        if (!isSameClusterOrBothUnclaimed(from, to)) {
            Plot toPlot = plugin.getPlotAPI().getPlotAt(to);
            if (toPlot != null && toPlot.isClaimed()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonPush(BlockPistonExtendEvent event) {
        if (!plugin.getConfig().getBoolean("anti-grief.piston-push-protection", true)) return;

        Location pistonLoc = event.getBlock().getLocation();
        if (!isPlotWorld(pistonLoc.getWorld().getName())) return;

        org.bukkit.block.BlockFace dir = event.getDirection();
        for (org.bukkit.block.Block b : event.getBlocks()) {
            Location from = b.getLocation();
            Location to = b.getRelative(dir).getLocation();
            if (!isSameClusterOrBothUnclaimed(from, to)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonPull(BlockPistonRetractEvent event) {
        if (!plugin.getConfig().getBoolean("anti-grief.piston-push-protection", true)) return;

        Location pistonLoc = event.getBlock().getLocation();
        if (!isPlotWorld(pistonLoc.getWorld().getName())) return;

        org.bukkit.block.BlockFace dir = event.getDirection().getOppositeFace();
        for (org.bukkit.block.Block b : event.getBlocks()) {
            Location from = b.getLocation();
            Location to = b.getRelative(dir).getLocation();
            if (!isSameClusterOrBothUnclaimed(from, to)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplodeShield(EntityExplodeEvent event) {
        if (!plugin.getConfig().getBoolean("anti-grief.tnt-cannon-shield", true)) return;

        Location source = event.getLocation();
        if (!isPlotWorld(source.getWorld().getName())) return;

        event.blockList().removeIf(block -> {
            Location blockLoc = block.getLocation();
            return !isSameClusterOrBothUnclaimed(source, blockLoc);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplodeShield(BlockExplodeEvent event) {
        if (!plugin.getConfig().getBoolean("anti-grief.tnt-cannon-shield", true)) return;

        Location source = event.getBlock().getLocation();
        if (!isPlotWorld(source.getWorld().getName())) return;

        event.blockList().removeIf(block -> {
            Location blockLoc = block.getLocation();
            return !isSameClusterOrBothUnclaimed(source, blockLoc);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        if (!plugin.getConfig().getBoolean("anti-grief.teleport-bypass-prevention", true)) return;

        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null || to.getWorld() == null) return;
        if (!isPlotWorld(to.getWorld().getName())) return;

        PlotId toPlotId = PlotGeometry.getPlotId(to);
        if (toPlotId != null && !PlotGeometry.isRoad(to)) {
            Plot toPlot = plugin.getPlotAPI().getPlot(to.getWorld().getName(), toPlotId);
            if (toPlot != null) {
                boolean allowed = true;
                if (toPlot.isDenied(player.getUniqueId())) {
                    allowed = false;
                } else if (toPlot.hasFlag("entry") && toPlot.getFlagValue("entry").equalsIgnoreCase("false")) {
                    if (!toPlot.getOwner().equals(player.getUniqueId()) &&
                        !toPlot.isTrusted(player.getUniqueId()) &&
                        !toPlot.isAdded(player.getUniqueId()) &&
                        !player.hasPermission("plots.admin")) {
                        allowed = false;
                    }
                }

                if (!allowed) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You are not allowed to teleport into this plot.");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTreeGrow(org.bukkit.event.world.StructureGrowEvent event) {
        if (!plugin.getConfig().getBoolean("anti-grief.tree-growth-containment", true)) return;

        Location source = event.getLocation();
        if (!isPlotWorld(source.getWorld().getName())) return;

        event.getBlocks().removeIf(block -> {
            Location blockLoc = block.getLocation();
            return !isSameClusterOrBothUnclaimed(source, blockLoc);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispense(org.bukkit.event.block.BlockDispenseEvent event) {
        if (!plugin.getConfig().getBoolean("anti-grief.dispenser-grief-prevention", true)) return;

        Location from = event.getBlock().getLocation();
        if (!isPlotWorld(from.getWorld().getName())) return;

        if (event.getBlock().getBlockData() instanceof org.bukkit.block.data.Directional) {
            org.bukkit.block.data.Directional directional = (org.bukkit.block.data.Directional) event.getBlock().getBlockData();
            org.bukkit.block.BlockFace face = directional.getFacing();
            Location to = event.getBlock().getRelative(face).getLocation();

            if (!isSameClusterOrBothUnclaimed(from, to)) {
                Plot toPlot = plugin.getPlotAPI().getPlotAt(to);
                if (toPlot != null && toPlot.isClaimed()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}

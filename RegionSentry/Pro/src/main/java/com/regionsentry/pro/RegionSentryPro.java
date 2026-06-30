package com.regionsentry.pro;

import com.regionsentry.lite.RegionSentryLite;
import com.regionsentry.lite.monitor.ChunkKey;
import com.regionsentry.lite.monitor.RegionTracker;
import com.regionsentry.pro.alert.AlertManager;
import com.regionsentry.pro.database.DatabaseManager;
import com.regionsentry.pro.gui.MitigationMenu;
import com.regionsentry.pro.listener.ProRegionClickListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class RegionSentryPro extends RegionSentryLite {
    private DatabaseManager dbManager;
    private MitigationMenu mitigationMenu;
    private AlertManager alertManager;
    private com.regionsentry.pro.mitigation.AdaptiveTickingManager adaptiveTickingManager;
    private com.regionsentry.pro.exploit.PacketStormDetector packetStormDetector;
    private com.regionsentry.pro.gui.HistoryLogbookGUI historyLogbookGUI;

    @Override
    public void onEnable() {
        // Load default config
        saveDefaultConfig();

        // 1. Initialize Lite Core
        super.onEnable();

        getLogger().info("Initializing RegionSentry Pro Modules...");

        // 2. Initialize Database
        dbManager = new DatabaseManager(this);
        dbManager.init();

        // 3. Initialize Mitigation GUI
        mitigationMenu = new MitigationMenu(this);

        // 4. Register Pro interactive click listeners on the Lite GUI
        if (this.gui != null) {
            this.gui.addClickListener(new ProRegionClickListener(this, mitigationMenu));
        }

        // Register Pro mitigation and limiter event listeners
        getServer().getPluginManager().registerEvents(new com.regionsentry.pro.listener.MitigationListener(this), this);
        getServer().getPluginManager().registerEvents(new com.regionsentry.pro.listener.LimiterListener(this), this);

        // Register outgoing proxy channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, "regionsentry:proxy");

        // 5. Start Alert Manager
        alertManager = new AlertManager(this, this.monitor);
        alertManager.start();

        // 6. Initialize and Start Pro Mitigation & Exploit Managers
        historyLogbookGUI = new com.regionsentry.pro.gui.HistoryLogbookGUI(this, dbManager);
        
        adaptiveTickingManager = new com.regionsentry.pro.mitigation.AdaptiveTickingManager(this, this.monitor);
        adaptiveTickingManager.start();
        
        packetStormDetector = new com.regionsentry.pro.exploit.PacketStormDetector(this, this.monitor);
        packetStormDetector.start();

        com.regionsentry.pro.mitigation.AFKTracker afkTracker = new com.regionsentry.pro.mitigation.AFKTracker(this);
        afkTracker.start();

        com.regionsentry.pro.mitigation.TieredDespawnEngine tieredDespawnEngine = new com.regionsentry.pro.mitigation.TieredDespawnEngine(this);
        tieredDespawnEngine.start();

        // 6. Schedule Database Logging of high MSPT spikes (runs every 10 seconds asynchronously)
        Bukkit.getAsyncScheduler().runAtFixedRate(this, (task) -> {
            logHighMsptSpikes();
        }, 10, 10, java.util.concurrent.TimeUnit.SECONDS);

        // 7. Schedule Database cleanup of records older than 24 hours (runs every hour)
        Bukkit.getAsyncScheduler().runAtFixedRate(this, (task) -> {
            dbManager.cleanOldLogsAsync();
        }, 1, 1, java.util.concurrent.TimeUnit.HOURS);
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down RegionSentry Pro Modules...");
        if (packetStormDetector != null) {
            packetStormDetector.stop();
        }
        if (dbManager != null) {
            dbManager.close();
        }
        super.onDisable();
    }

    private void logHighMsptSpikes() {
        for (RegionTracker tracker : this.monitor.getTrackers()) {
            double mspt = tracker.getAverageMSPT();
            // Log if regional MSPT exceeds 35ms (strained/lagging)
            if (mspt > 35.0 && !tracker.getChunks().isEmpty()) {
                ChunkKey representative = tracker.getChunks().iterator().next();
                
                // Fetch players in the region thread safely
                List<String> playerNames = new ArrayList<>();
                World world = Bukkit.getWorld(representative.getWorldName());
                if (world != null) {
                    // We must run on the region thread of the representative chunk to query entities/players safely
                    Bukkit.getRegionScheduler().execute(this, world, representative.getX(), representative.getZ(), () -> {
                        for (ChunkKey key : tracker.getChunks()) {
                            if (world.isChunkLoaded(key.getX(), key.getZ())) {
                                Chunk chunk = world.getChunkAt(key.getX(), key.getZ());
                                for (Entity entity : chunk.getEntities()) {
                                    if (entity instanceof Player) {
                                        playerNames.add(entity.getName());
                                    }
                                }
                            }
                        }
                        
                        // Write to the SQLite database asynchronously
                        if (!playerNames.isEmpty() || mspt > 45.0) { // Log significant lag spikes or populated laggy regions
                            dbManager.logSpikeAsync(
                                    representative.getWorldName(),
                                    representative.getX(),
                                    representative.getZ(),
                                    mspt,
                                    playerNames
                            );
                        }
                    });
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 2 && args[0].equalsIgnoreCase("stitch")) {
            if (!sender.hasPermission("regionsentry.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to stitch region threads.");
                return true;
            }
            String id1 = args[1];
            String id2 = args[2];
            com.regionsentry.lite.monitor.PerformanceMonitor.STITCHED_REGIONS.put(id1, id2);
            sender.sendMessage(ChatColor.GREEN + "[RegionSentry Pro] Successfully stitched region thread " + id1 + " to " + id2 + ". Merging performance pools on next scan.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("history")) {
            if (!sender.hasPermission("regionsentry.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to view historical performance data.");
                return true;
            }

            if (sender instanceof Player) {
                historyLogbookGUI.open((Player) sender);
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "=== RegionSentry Lag History (Top Hotspots - Last 24 Hours) ===");
            dbManager.getTopSpikesAsync(10, (records) -> {
                if (records.isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "No significant regional MSPT spikes logged in the last 24 hours.");
                    return;
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                        .withZone(ZoneId.systemDefault());

                int rank = 1;
                for (DatabaseManager.HistoryRecord record : records) {
                    String timeStr = formatter.format(Instant.ofEpochSecond(record.timestamp));
                    String blockStr = "Block: X[" + (record.chunkX * 16) + "], Z[" + (record.chunkZ * 16) + "]";
                    sender.sendMessage(ChatColor.YELLOW + "" + rank + ". [" + timeStr + "] " + ChatColor.WHITE + record.world + " (" + blockStr + ") " +
                            ChatColor.RED + String.format("%.2f ms", record.maxMspt) + ChatColor.GRAY + " | Players: " +
                            (record.players.isEmpty() ? "None" : record.players));
                    rank++;
                }
            });
            return true;
        }

        // Default command behavior opens the GUI
        return super.onCommand(sender, command, label, args);
    }
}

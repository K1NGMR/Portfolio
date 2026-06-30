package com.simpleplots;

import com.simpleplots.api.Plot;
import com.simpleplots.api.PlotGeometry;
import com.simpleplots.api.PlotId;
import com.simpleplots.commands.PlotCommand;
import com.simpleplots.commands.PlotTabCompleter;
import com.simpleplots.db.DatabaseManager;
import com.simpleplots.generator.PlotWorldConfig;
import com.simpleplots.listener.PlotListener;
import com.simpleplots.placeholder.PlotPlaceholderExpansion;
import com.simpleplots.util.DownloadHandler;
import com.simpleplots.util.SchematicHandler;
import com.simpleplots.util.UUIDCache;
import com.simpleplots.util.WorldEditHook;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.inventory.ItemStack;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main class for the SimplePlots plugin.
 */
public class SimplePlots extends JavaPlugin {
    private static SimplePlots instance;
    
    private PlotAPI plotAPI;
    private DatabaseManager databaseManager;
    private UUIDCache uuidCache;
    private SchematicHandler schematicHandler;
    private DownloadHandler downloadHandler;
    private org.bukkit.scheduler.BukkitTask inactivityTask;

    private FileConfiguration worldsFile;
    private FileConfiguration storageFile;
    private FileConfiguration messagesFile;
    private FileConfiguration permissionsConfig;

    private final Map<String, PlotWorldConfig> worldConfigs = new ConcurrentHashMap<>();
    private final Set<UUID> plotChatEnabled = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        instance = this;

        try {
            // 1. Load Configurations
            loadCustomConfigs();


            // 3. Initialize API & Database
            plotAPI = new PlotAPI(this);
            databaseManager = new DatabaseManager(this);
            databaseManager.init(storageFile);
            databaseManager.loadAllPlots();
            loadPlotWorlds();
            loadMergeRequests();

            // 3b. Refresh Plot Borders on Server Enable
            getLogger().info("Refreshing all claimed plot borders...");
            for (com.simpleplots.api.Plot plot : plotAPI.getAllPlots()) {
                try {
                    com.simpleplots.api.PlotGeometry.updatePlotBorders(plot.getWorld(), plot.getId());
                } catch (Exception e) {
                    getLogger().severe("Failed to refresh borders for plot " + plot.getId() + ": " + e.getMessage());
                }
            }

            // 4. Initialize Utilities
            uuidCache = new UUIDCache(this);
            if (getServer().getPluginManager().getPlugin("WorldEdit") != null) {
                schematicHandler = new SchematicHandler(this);
            }
            downloadHandler = new DownloadHandler(this);

            // 5. Register Listeners & WorldEdit Hooks
            getServer().getPluginManager().registerEvents(new PlotListener(this), this);
            getServer().getPluginManager().registerEvents(new com.simpleplots.listener.PlotGUIListener(this), this);
            if (getServer().getPluginManager().getPlugin("WorldEdit") != null) {
                new WorldEditHook(this).register();
            }

            // 6. Register Commands
            if (getCommand("plot") != null) {
                getCommand("plot").setExecutor(new PlotCommand(this));
                getCommand("plot").setTabCompleter(new PlotTabCompleter());
            }

            // 7. Register Placeholders (PlaceholderAPI)
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new PlotPlaceholderExpansion(this).register();
                getLogger().info("Successfully hooked into PlaceholderAPI.");
            }

            // 8. Start repeating task for Feed and Heal flags
            startFlagSchedulers();

            // 9. Start Inactivity Expiration Watchdog
            if (getConfig().getBoolean("inactivity-expiration.enabled", true)) {
                int intervalHours = getConfig().getInt("inactivity-expiration.check-interval-hours", 12);
                long intervalTicks = intervalHours * 60L * 60L * 20L;
                inactivityTask = new com.simpleplots.util.InactivityWatchdog(this).runTaskTimerAsynchronously(this, 1200L, intervalTicks);
                getLogger().info("Successfully started Inactivity Watchdog task.");
            }

            getLogger().info("SimplePlots has been fully enabled!");
        } catch (Throwable t) {
            getLogger().severe("=============================================");
            getLogger().severe("FATAL ERROR: Failed to enable SimplePlots plugin!");
            getLogger().severe("Error details: " + t.getMessage());
            t.printStackTrace();
            getLogger().severe("=============================================");
        }
    }

    @Override
    public void onDisable() {
        saveMergeRequests();
        if (inactivityTask != null) {
            inactivityTask.cancel();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("SimplePlots has been disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        loadCustomConfigs();

        if (inactivityTask != null) {
            inactivityTask.cancel();
            inactivityTask = null;
        }

        if (getConfig().getBoolean("inactivity-expiration.enabled", true)) {
            int intervalHours = getConfig().getInt("inactivity-expiration.check-interval-hours", 12);
            long intervalTicks = intervalHours * 60L * 60L * 20L;
            inactivityTask = new com.simpleplots.util.InactivityWatchdog(this).runTaskTimerAsynchronously(this, 1200L, intervalTicks);
            getLogger().info("Successfully restarted Inactivity Watchdog task during hot-reload.");
        }
    }

    public static SimplePlots getInstance() {
        return instance;
    }

    public PlotAPI getPlotAPI() {
        return plotAPI;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public UUIDCache getUuidCache() {
        return uuidCache;
    }

    public SchematicHandler getSchematicHandler() {
        return schematicHandler;
    }

    public DownloadHandler getDownloadHandler() {
        return downloadHandler;
    }


    public FileConfiguration getWorldsFile() {
        return worldsFile;
    }

    public FileConfiguration getStorageFile() {
        return storageFile;
    }

    public FileConfiguration getMessagesFile() {
        return messagesFile;
    }

    public Set<UUID> getPlotChatEnabled() {
        return plotChatEnabled;
    }

    /**
     * Retrives the world config for a given world name.
     */
    public PlotWorldConfig getWorldConfig(String worldName) {
        if (worldName == null) return null;
        return worldConfigs.get(worldName.toLowerCase());
    }

    public void addWorldConfig(String worldName, PlotWorldConfig config) {
        worldConfigs.put(worldName.toLowerCase(), config);
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        PlotWorldConfig config = getWorldConfig(worldName);
        if (config == null) {
            // Load dynamically from worlds.yml
            if (worldsFile != null && worldsFile.contains("worlds." + worldName)) {
                config = new PlotWorldConfig(worldName, worldsFile.getConfigurationSection("worlds." + worldName));
            } else {
                // Return default settings
                config = new PlotWorldConfig(worldName, getConfig().createSection("worlds." + worldName));
            }
            addWorldConfig(worldName, config);
        }
        return new com.simpleplots.generator.PlotChunkGenerator(config);
    }

    private void loadCustomConfigs() {
        saveDefaultConfig();

        File wFile = new File(getDataFolder(), "worlds.yml");
        if (!wFile.exists()) {
            saveResource("worlds.yml", false);
        }
        worldsFile = YamlConfiguration.loadConfiguration(wFile);

        File sFile = new File(getDataFolder(), "storage.yml");
        if (!sFile.exists()) {
            saveResource("storage.yml", false);
        }
        storageFile = YamlConfiguration.loadConfiguration(sFile);

        File mFile = new File(getDataFolder(), "messages.yml");
        if (!mFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesFile = YamlConfiguration.loadConfiguration(mFile);

        File pFile = new File(getDataFolder(), "permissions.yml");
        if (!pFile.exists()) {
            saveResource("permissions.yml", false);
        }
        permissionsConfig = YamlConfiguration.loadConfiguration(pFile);

        // Load pre-existing worlds from worlds.yml into memory
        if (worldsFile.contains("worlds")) {
            for (String worldName : worldsFile.getConfigurationSection("worlds").getKeys(false)) {
                PlotWorldConfig config = new PlotWorldConfig(worldName, worldsFile.getConfigurationSection("worlds." + worldName));
                addWorldConfig(worldName, config);
            }
        }
    }


    private void startFlagSchedulers() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (getWorldConfig(player.getWorld().getName()) == null) {
                    continue;
                }

                Plot plot = plotAPI.getPlotAt(player.getLocation());
                if (plot != null) {
                    // Feed flag
                    if (plot.hasFlag("feed") && plot.getFlagValue("feed").equalsIgnoreCase("true")) {
                        player.setFoodLevel(20);
                    }
                    // Heal flag
                    if (plot.hasFlag("heal") && plot.getFlagValue("heal").equalsIgnoreCase("true")) {
                        double maxHealth = player.getMaxHealth();
                        player.setHealth(maxHealth);
                    }
                }
            }
        }, 60L, 60L); // Run every 3 seconds (60 ticks)
    }

    private void loadPlotWorlds() {
        for (Map.Entry<String, PlotWorldConfig> entry : worldConfigs.entrySet()) {
            String worldName = entry.getValue().getWorldName();
            if (Bukkit.getWorld(worldName) == null) {
                getLogger().info("Loading plot world: " + worldName + "...");
                try {
                    WorldCreator creator = new WorldCreator(worldName);
                    creator.generator(new com.simpleplots.generator.PlotChunkGenerator(entry.getValue()));
                    creator.generateStructures(false);
                    World world = Bukkit.createWorld(creator);
                    if (world != null) {
                        world.setKeepSpawnInMemory(false);
                        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, entry.getValue().isDoMobSpawning());
                        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, entry.getValue().isDoDaylightCycle());
                    } else {
                        getLogger().severe("Failed to load plot world: " + worldName);
                    }
                } catch (Exception e) {
                    getLogger().severe("Failed to load plot world '" + worldName + "' due to error: " + e.getMessage());
                }
            }
        }
    }

    public static class PlotChestSession {
        public final List<ItemStack> items;
        public int page;
        public PlotChestSession(List<ItemStack> items, int page) {
            this.items = items;
            this.page = page;
        }
    }

    private final Map<UUID, PlotChestSession> activeChestSessions = new ConcurrentHashMap<>();

    public Map<UUID, PlotChestSession> getActiveChestSessions() {
        return activeChestSessions;
    }

    public synchronized void saveToPlotChest(UUID playerUuid, List<ItemStack> newItems) {
        File chestsDir = new File(getDataFolder(), "chests");
        if (!chestsDir.exists()) chestsDir.mkdirs();
        
        File file = new File(chestsDir, playerUuid.toString() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        List<Map<String, Object>> savedList = (List<Map<String, Object>>) config.getList("items");
        if (savedList == null) {
            savedList = new ArrayList<>();
        }
        
        long expiryHours = getConfig().getLong("plot-chest.expiry-hours", 168);
        long now = System.currentTimeMillis();
        
        // Add new items
        for (ItemStack item : newItems) {
            Map<String, Object> map = new HashMap<>();
            map.put("item", item);
            map.put("timestamp", now);
            savedList.add(map);
        }
        
        config.set("items", savedList);
        try {
            config.save(file);
        } catch (Exception e) {
            getLogger().severe("Failed to save items to plot chest: " + e.getMessage());
        }
    }

    public synchronized void saveAllToPlotChest(UUID playerUuid, List<ItemStack> allItems) {
        File chestsDir = new File(getDataFolder(), "chests");
        if (!chestsDir.exists()) chestsDir.mkdirs();
        
        File file = new File(chestsDir, playerUuid.toString() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        long now = System.currentTimeMillis();
        List<Map<String, Object>> savedList = new ArrayList<>();
        for (ItemStack item : allItems) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                Map<String, Object> map = new HashMap<>();
                map.put("item", item);
                map.put("timestamp", now);
                savedList.add(map);
            }
        }
        
        config.set("items", savedList);
        try {
            config.save(file);
        } catch (Exception e) {
            getLogger().severe("Failed to save all items to plot chest: " + e.getMessage());
        }
    }

    public synchronized List<ItemStack> loadPlotChest(UUID playerUuid) {
        File chestsDir = new File(getDataFolder(), "chests");
        File file = new File(chestsDir, playerUuid.toString() + ".yml");
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<String, Object>> savedList = (List<Map<String, Object>>) config.getList("items");
        if (savedList == null) {
            return new ArrayList<>();
        }
        
        long expiryHours = getConfig().getLong("plot-chest.expiry-hours", 168);
        long expiryMs = expiryHours * 60 * 60 * 1000;
        long now = System.currentTimeMillis();
        
        List<ItemStack> activeItems = new ArrayList<>();
        List<Map<String, Object>> updatedList = new ArrayList<>();
        
        for (Map<String, Object> map : savedList) {
            ItemStack item = (ItemStack) map.get("item");
            Number timestampNum = (Number) map.get("timestamp");
            long timestamp = timestampNum != null ? timestampNum.longValue() : now;
            
            if (now - timestamp < expiryMs) {
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    activeItems.add(item);
                    updatedList.add(map);
                }
            }
        }
        
        // If some items expired, save the updated list
        if (updatedList.size() < savedList.size()) {
            config.set("items", updatedList);
            try {
                config.save(file);
            } catch (Exception e) {
                getLogger().severe("Failed to save updated plot chest after expiry: " + e.getMessage());
            }
        }
        
        return activeItems;
    }

    public FileConfiguration getPermissionsConfig() {
        return permissionsConfig;
    }

    public boolean hasPermissionForSubcommand(Player player, String subcommand) {
        if (player.hasPermission("plots.admin")) {
            return true;
        }
        
        String path = "commands." + subcommand.toLowerCase();
        if (permissionsConfig == null || !permissionsConfig.contains(path)) {
            // Default checks if not defined in permissions.yml
            if (subcommand.equalsIgnoreCase("chest") || subcommand.equalsIgnoreCase("help")) {
                return true;
            }
            return player.hasPermission("plots." + subcommand.toLowerCase());
        }
        
        String perm = permissionsConfig.getString(path);
        if (perm == null || perm.equalsIgnoreCase("false")) {
            return true;
        }
        
        return player.hasPermission(perm);
    }

    public static class MergeRequest {
        public final UUID sender;
        public final UUID receiver;
        public final String world;
        public final com.simpleplots.api.PlotId senderPlot;
        public final com.simpleplots.api.PlotId receiverPlot;
        public final long timestamp;

        public MergeRequest(UUID sender, UUID receiver, String world, com.simpleplots.api.PlotId senderPlot, com.simpleplots.api.PlotId receiverPlot, long timestamp) {
            this.sender = sender;
            this.receiver = receiver;
            this.world = world;
            this.senderPlot = senderPlot;
            this.receiverPlot = receiverPlot;
            this.timestamp = timestamp;
        }
    }

    private final List<MergeRequest> pendingMergeRequests = new java.util.concurrent.CopyOnWriteArrayList<>();

    public List<MergeRequest> getPendingMergeRequests() {
        return pendingMergeRequests;
    }

    public void loadMergeRequests() {
        File file = new File(getDataFolder(), "mergerequests.yml");
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        pendingMergeRequests.clear();
        if (config.contains("requests")) {
            for (Map<?, ?> map : config.getMapList("requests")) {
                UUID sender = UUID.fromString((String) map.get("sender"));
                UUID receiver = UUID.fromString((String) map.get("receiver"));
                String world = (String) map.get("world");
                com.simpleplots.api.PlotId senderPlot = new com.simpleplots.api.PlotId(((Number) map.get("senderPlotX")).intValue(), ((Number) map.get("senderPlotZ")).intValue());
                com.simpleplots.api.PlotId receiverPlot = new com.simpleplots.api.PlotId(((Number) map.get("receiverPlotX")).intValue(), ((Number) map.get("receiverPlotZ")).intValue());
                long timestamp = ((Number) map.get("timestamp")).longValue();
                
                // Only keep requests under 7 days old
                if (System.currentTimeMillis() - timestamp < 7L * 24L * 60L * 60L * 1000L) {
                    pendingMergeRequests.add(new MergeRequest(sender, receiver, world, senderPlot, receiverPlot, timestamp));
                }
            }
        }
    }

    public void saveMergeRequests() {
        File file = new File(getDataFolder(), "mergerequests.yml");
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (MergeRequest req : pendingMergeRequests) {
            Map<String, Object> map = new HashMap<>();
            map.put("sender", req.sender.toString());
            map.put("receiver", req.receiver.toString());
            map.put("world", req.world);
            map.put("senderPlotX", req.senderPlot.getX());
            map.put("senderPlotZ", req.senderPlot.getZ());
            map.put("receiverPlotX", req.receiverPlot.getX());
            map.put("receiverPlotZ", req.receiverPlot.getZ());
            map.put("timestamp", req.timestamp);
            list.add(map);
        }
        config.set("requests", list);
        try {
            config.save(file);
        } catch (Exception e) {
            getLogger().severe("Failed to save merge requests: " + e.getMessage());
        }
    }
}

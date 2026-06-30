package com.regionsentry.lite.gui;

import com.regionsentry.lite.monitor.ChunkKey;
import com.regionsentry.lite.monitor.PerformanceMonitor;
import com.regionsentry.lite.monitor.RegionTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class RegionGridGUI implements Listener {
    private static final int ITEMS_PER_PAGE = 45;
    private static final String GUI_TITLE_PREFIX = ChatColor.DARK_GRAY + "RegionSentry - Page ";

    private final JavaPlugin plugin;
    private final PerformanceMonitor monitor;
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final List<RegionClickListener> clickListeners = new ArrayList<>();
    private final Set<UUID> openGuiPlayers = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public RegionGridGUI(JavaPlugin plugin, PerformanceMonitor monitor) {
        this.plugin = plugin;
        this.monitor = monitor;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Auto-refresh the GUI dynamically from config
        int refreshInterval = plugin.getConfig().getInt("gui-refresh-interval-seconds", 3);
        if (refreshInterval < 1) refreshInterval = 1;
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (task) -> {
            for (UUID uuid : openGuiPlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    int page = playerPages.getOrDefault(uuid, 1);
                    player.getScheduler().execute(plugin, () -> {
                        openGUI(player, page);
                    }, null, 0);
                } else {
                    openGuiPlayers.remove(uuid);
                }
            }
        }, refreshInterval, refreshInterval, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void addClickListener(RegionClickListener listener) {
        clickListeners.add(listener);
    }

    public void openGUI(Player player, int page) {
        playerPages.put(player.getUniqueId(), page);
        openGuiPlayers.add(player.getUniqueId());

        List<RegionTracker> sortedTrackers = new ArrayList<>(monitor.getTrackers());
        // Sort worst performing first (highest MSPT, then lowest TPS)
        sortedTrackers.sort((a, b) -> {
            int c = Double.compare(b.getAverageMSPT(), a.getAverageMSPT());
            if (c != 0) return c;
            return Double.compare(a.getAverageTPS(), b.getAverageTPS());
        });

        int totalRegions = sortedTrackers.size();
        int maxPage = Math.max(1, (int) Math.ceil((double) totalRegions / ITEMS_PER_PAGE));
        if (page > maxPage) page = maxPage;
        if (page < 1) page = 1;

        Inventory inv;
        String expectedTitle = GUI_TITLE_PREFIX + page;
        boolean alreadyOpen = false;
        if (player.getOpenInventory() != null && player.getOpenInventory().getTitle().equals(expectedTitle)) {
            inv = player.getOpenInventory().getTopInventory();
            alreadyOpen = true;
        } else {
            inv = Bukkit.createInventory(null, 54, expectedTitle);
        }

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalRegions);

        for (int i = startIndex; i < endIndex; i++) {
            RegionTracker tracker = sortedTrackers.get(i);
            int slot = i - startIndex;
            inv.setItem(slot, createRegionItem(tracker));
        }

        // Fill empty region slots with light gray glass to look uniform
        for (int slot = (endIndex - startIndex); slot < ITEMS_PER_PAGE; slot++) {
            inv.setItem(slot, createEmptyItem());
        }

        // Add bottom navigation row (slots 45-53)
        ItemStack filler = createGrayFiller();
        for (int slot = 45; slot < 54; slot++) {
            inv.setItem(slot, filler);
        }

        // Page navigation items
        if (page > 1) {
            inv.setItem(45, createNavigationItem(Material.ARROW, ChatColor.YELLOW + "« Previous Page"));
        }
        if (page < maxPage) {
            inv.setItem(53, createNavigationItem(Material.ARROW, ChatColor.YELLOW + "Next Page »"));
        }

        // Info item in the middle
        inv.setItem(49, createInfoItem(page, maxPage, totalRegions));

        if (!alreadyOpen) {
            player.openInventory(inv);
        }
    }

    public static final Map<String, List<String>> REGION_FLAGS = new java.util.concurrent.ConcurrentHashMap<>();

    private ItemStack createRegionItem(RegionTracker tracker) {
        double mspt = tracker.getAverageMSPT();
        double tps = tracker.getAverageTPS();

        double stableMspt = plugin.getConfig().getDouble("status-thresholds.stable.mspt", 30.0);
        double strainedMspt = plugin.getConfig().getDouble("status-thresholds.strained.mspt", 50.0);

        Material material;
        ChatColor color;
        String status;

        if (mspt < stableMspt) {
            String matStr = plugin.getConfig().getString("status-thresholds.stable.material", "GREEN_STAINED_GLASS_PANE");
            material = Material.matchMaterial(matStr);
            if (material == null) material = Material.GREEN_STAINED_GLASS_PANE;
            String colorStr = plugin.getConfig().getString("status-thresholds.stable.color", "GREEN");
            try { color = ChatColor.valueOf(colorStr); } catch (Exception e) { color = ChatColor.GREEN; }
            status = plugin.getConfig().getString("status-thresholds.stable.status-text", "STABLE");
        } else if (mspt < strainedMspt) {
            String matStr = plugin.getConfig().getString("status-thresholds.strained.material", "YELLOW_STAINED_GLASS_PANE");
            material = Material.matchMaterial(matStr);
            if (material == null) material = Material.YELLOW_STAINED_GLASS_PANE;
            String colorStr = plugin.getConfig().getString("status-thresholds.strained.color", "GOLD");
            try { color = ChatColor.valueOf(colorStr); } catch (Exception e) { color = ChatColor.GOLD; }
            status = plugin.getConfig().getString("status-thresholds.strained.status-text", "STRAINED");
        } else {
            String matStr = plugin.getConfig().getString("status-thresholds.lagging.material", "RED_STAINED_GLASS_PANE");
            material = Material.matchMaterial(matStr);
            if (material == null) material = Material.RED_STAINED_GLASS_PANE;
            String colorStr = plugin.getConfig().getString("status-thresholds.lagging.color", "RED");
            try { color = ChatColor.valueOf(colorStr); } catch (Exception e) { color = ChatColor.RED; }
            status = plugin.getConfig().getString("status-thresholds.lagging.status-text", "LAGGING");
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color + "Region Thread: " + status);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Thread ID: " + ChatColor.WHITE + tracker.getThreadId());
            lore.add(ChatColor.GRAY + "Thread Name: " + ChatColor.WHITE + tracker.getThreadName());
            lore.add("");

            // Compute boundaries
            Set<ChunkKey> chunks = tracker.getChunks();
            if (!chunks.isEmpty()) {
                int minX = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int minZ = Integer.MAX_VALUE;
                int maxZ = Integer.MIN_VALUE;
                String worldName = "";

                for (ChunkKey key : chunks) {
                    worldName = key.getWorldName();
                    if (key.getX() < minX) minX = key.getX();
                    if (key.getX() > maxX) maxX = key.getX();
                    if (key.getZ() < minZ) minZ = key.getZ();
                    if (key.getZ() > maxZ) maxZ = key.getZ();
                }

                lore.add(ChatColor.BLUE + "Boundaries (" + worldName + "):");
                lore.add(ChatColor.GRAY + "  Chunk: " + ChatColor.WHITE + "X[" + minX + " to " + maxX + "], Z[" + minZ + " to " + maxZ + "]");
                lore.add(ChatColor.GRAY + "  Block: " + ChatColor.WHITE + "X[" + (minX * 16) + " to " + (maxX * 16 + 15) + "], Z[" + (minZ * 16) + " to " + (maxZ * 16 + 15) + "]");
                lore.add(ChatColor.GRAY + "  Total Chunks: " + ChatColor.WHITE + chunks.size());
            } else {
                lore.add(ChatColor.RED + "No active chunks mapped.");
            }

            lore.add("");
            lore.add(ChatColor.GRAY + "Thread Performance:");
            lore.add(ChatColor.GRAY + "  TPS: " + color + String.format("%.2f", tps));
            lore.add(ChatColor.GRAY + "  MSPT: " + color + String.format("%.2f ms", mspt));
            lore.add(ChatColor.GRAY + "  Utilization: " + getUtilizationBar(tracker.getThreadUtilization()));
            lore.add("");
            lore.add(ChatColor.GRAY + "Load Metrics:");
            lore.add(ChatColor.GRAY + "  Active Entities: " + ChatColor.WHITE + tracker.getEntityCount());
            lore.add(ChatColor.GRAY + "  Active Players: " + ChatColor.WHITE + tracker.getPlayerCount());
            lore.add(ChatColor.GRAY + "  Chunks Loaded/Sec: " + ChatColor.WHITE + String.format("%.1f", tracker.getChunksLoadedPerSec()));
            lore.add(ChatColor.GRAY + "  New Chunks Gen/Sec: " + ChatColor.WHITE + String.format("%.1f", tracker.getChunksGeneratedPerSec()));
            lore.add(ChatColor.GRAY + "  Boundary Crossings: " + ChatColor.WHITE + String.format("%.1f / min", tracker.getBoundaryCrossingsPerMin()));
            lore.add(ChatColor.GRAY + "  Fragmentation Risk: " + ChatColor.WHITE + tracker.getFragmentationRisk());
            if (tracker.isMergeWarning()) {
                lore.add(ChatColor.RED + "  ⚠ COLLISION IMMINENT (Merge < 30s)");
            }
            
            // Draw Pro flags if any
            List<String> flags = REGION_FLAGS.get(tracker.getRegionId());
            if (flags != null && !flags.isEmpty()) {
                lore.add("");
                lore.add(ChatColor.RED + "Alert Flags:");
                for (String flag : flags) {
                    lore.add(ChatColor.RED + "  " + flag);
                }
            }

            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "Left-Click: Teleport (Pro)");
            lore.add(ChatColor.DARK_GRAY + "Right-Click: Mitigation Menu (Pro)");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getUtilizationBar(double utilization) {
        int totalBlocks = 10;
        int filledBlocks = (int) Math.round((utilization / 100.0) * totalBlocks);
        if (filledBlocks < 0) filledBlocks = 0;
        if (filledBlocks > totalBlocks) filledBlocks = totalBlocks;

        StringBuilder bar = new StringBuilder();
        ChatColor barColor;
        if (utilization < 60.0) {
            barColor = ChatColor.GREEN;
        } else if (utilization <= 100.0) {
            barColor = ChatColor.GOLD;
        } else {
            barColor = ChatColor.RED;
        }

        bar.append(barColor).append("[");
        for (int i = 0; i < totalBlocks; i++) {
            if (i < filledBlocks) {
                bar.append("■");
            } else {
                bar.append("□");
            }
        }
        bar.append("] ").append(String.format("%.1f%%", utilization));
        return bar.toString();
    }

    private ItemStack createEmptyItem() {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GRAY + "No Region Mapped");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createGrayFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavigationItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem(int page, int maxPage, int total) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Dashboard Information");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Current Page: " + ChatColor.WHITE + page + " / " + maxPage);
            lore.add(ChatColor.GRAY + "Total Mapped Threads: " + ChatColor.WHITE + total);
            lore.add("");
            lore.add(ChatColor.GRAY + "Scale Status thresholds:");
            lore.add(ChatColor.GREEN + "  Stable: <30ms MSPT");
            lore.add(ChatColor.GOLD + "  Strained: 30ms-50ms MSPT");
            lore.add(ChatColor.RED + "  Lagging: >50ms MSPT");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(GUI_TITLE_PREFIX)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        int currentPage = playerPages.getOrDefault(player.getUniqueId(), 1);

        if (slot == 45) {
            // Previous page
            if (currentPage > 1) {
                openGUI(player, currentPage - 1);
            }
        } else if (slot == 53) {
            // Next page
            List<RegionTracker> sortedTrackers = new ArrayList<>(monitor.getTrackers());
            int maxPage = Math.max(1, (int) Math.ceil((double) sortedTrackers.size() / ITEMS_PER_PAGE));
            if (currentPage < maxPage) {
                openGUI(player, currentPage + 1);
            }
        } else if (slot < ITEMS_PER_PAGE) {
            // Region item click
            List<RegionTracker> sortedTrackers = new ArrayList<>(monitor.getTrackers());
            sortedTrackers.sort((a, b) -> {
                int c = Double.compare(b.getAverageMSPT(), a.getAverageMSPT());
                if (c != 0) return c;
                return Double.compare(a.getAverageTPS(), b.getAverageTPS());
            });

            int targetIndex = (currentPage - 1) * ITEMS_PER_PAGE + slot;
            if (targetIndex < sortedTrackers.size()) {
                RegionTracker tracker = sortedTrackers.get(targetIndex);
                if (event.isLeftClick()) {
                    for (RegionClickListener listener : clickListeners) {
                        listener.onLeftClick(player, tracker);
                    }
                } else if (event.isRightClick()) {
                    for (RegionClickListener listener : clickListeners) {
                        listener.onRightClick(player, tracker);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (title.startsWith(GUI_TITLE_PREFIX)) {
            openGuiPlayers.remove(event.getPlayer().getUniqueId());
        }
    }
}

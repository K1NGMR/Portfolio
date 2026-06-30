package com.regionsentry.pro.gui;

import com.regionsentry.pro.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class HistoryLogbookGUI implements Listener {
    private static final String GUI_TITLE = ChatColor.DARK_RED + "Historical Lag Logbook";
    
    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;
    private final Map<UUID, List<DatabaseManager.HistoryRecord>> playerCachedLogs = new HashMap<>();

    public HistoryLogbookGUI(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        player.sendMessage(ChatColor.YELLOW + "[RegionSentry] Loading historical logbook from database...");
        
        dbManager.getTopSpikesAsync(45, (records) -> {
            // Must run back on the region/main thread for GUI creation
            Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
                playerCachedLogs.put(player.getUniqueId(), records);

                Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault());

                int slot = 0;
                for (DatabaseManager.HistoryRecord record : records) {
                    if (slot >= 45) break; // Limit to 45 items, leave last row for stats
                    
                    ItemStack item = new ItemStack(Material.WRITTEN_BOOK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(ChatColor.RED + "Lag Spike Event #" + (slot + 1));
                        
                        String timeStr = formatter.format(Instant.ofEpochSecond(record.timestamp));
                        List<String> lore = new ArrayList<>();
                        lore.add(ChatColor.GRAY + "Timestamp: " + ChatColor.WHITE + timeStr);
                        lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + record.world);
                        lore.add(ChatColor.GRAY + "Chunk: " + ChatColor.WHITE + "X[" + record.chunkX + "], Z[" + record.chunkZ + "]");
                        lore.add(ChatColor.GRAY + "Block: " + ChatColor.WHITE + "X[" + (record.chunkX * 16 + 8) + "], Z[" + (record.chunkZ * 16 + 8) + "]");
                        lore.add("");
                        lore.add(ChatColor.GRAY + "Metrics at Spike:");
                        lore.add(ChatColor.GRAY + "  Peak MSPT: " + ChatColor.RED + String.format("%.2f ms", record.maxMspt));
                        lore.add(ChatColor.GRAY + "  Active Players: " + ChatColor.YELLOW + (record.players.isEmpty() ? "None" : record.players));
                        lore.add("");
                        lore.add(ChatColor.GREEN + "Left-Click: Teleport to Hotspot");
                        
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                    inv.setItem(slot, item);
                    slot++;
                }

                // Fill empty slots with light gray glass
                ItemStack empty = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
                ItemMeta emptyMeta = empty.getItemMeta();
                if (emptyMeta != null) {
                    emptyMeta.setDisplayName(" ");
                    empty.setItemMeta(emptyMeta);
                }
                for (int s = slot; s < 45; s++) {
                    inv.setItem(s, empty);
                }

                // Fill bottom row with gray filler
                ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta fillerMeta = filler.getItemMeta();
                if (fillerMeta != null) {
                    fillerMeta.setDisplayName(" ");
                    filler.setItemMeta(fillerMeta);
                }
                for (int s = 45; s < 54; s++) {
                    inv.setItem(s, filler);
                }

                // Info book in the center of the bottom row
                ItemStack info = new ItemStack(Material.BOOK);
                ItemMeta infoMeta = info.getItemMeta();
                if (infoMeta != null) {
                    infoMeta.setDisplayName(ChatColor.GOLD + "Database Summary");
                    infoMeta.setLore(Arrays.asList(
                            ChatColor.GRAY + "Logged hotspots over a rolling 24h window.",
                            ChatColor.GRAY + "Click entries to investigate lag locations."
                    ));
                    info.setItemMeta(infoMeta);
                }
                inv.setItem(49, info);

                player.openInventory(inv);
            });
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(GUI_TITLE)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 45) return;

        List<DatabaseManager.HistoryRecord> cached = playerCachedLogs.get(player.getUniqueId());
        if (cached == null || slot >= cached.size()) return;

        DatabaseManager.HistoryRecord record = cached.get(slot);
        World world = Bukkit.getWorld(record.world);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Cannot teleport: World is not loaded.");
            player.closeInventory();
            return;
        }

        player.closeInventory();
        Location target = new Location(world, record.chunkX * 16 + 8, 100, record.chunkZ * 16 + 8);
        player.sendMessage(ChatColor.YELLOW + "[RegionSentry] Teleporting to historical hotspot asynchronously...");
        
        player.teleportAsync(target).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                player.sendMessage(ChatColor.GREEN + "[RegionSentry] Arrived at historical hotspot: " + record.world + " (" + (record.chunkX * 16) + ", " + (record.chunkZ * 16) + ").");
            } else {
                player.sendMessage(ChatColor.RED + "[RegionSentry] Asynchronous teleport failed.");
            }
        });
    }
}

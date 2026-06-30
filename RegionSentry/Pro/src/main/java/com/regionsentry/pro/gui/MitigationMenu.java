package com.regionsentry.pro.gui;

import com.regionsentry.lite.monitor.ChunkKey;
import com.regionsentry.lite.monitor.RegionTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class MitigationMenu implements Listener {
    private static final String TITLE_PREFIX = ChatColor.RED + "Mitigation Actions - Thread ";
    
    private final JavaPlugin plugin;
    private final Map<UUID, RegionTracker> playerTargets = new HashMap<>();

    public MitigationMenu(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player, RegionTracker tracker) {
        playerTargets.put(player.getUniqueId(), tracker);

        Inventory inv = Bukkit.createInventory(null, 18, TITLE_PREFIX + tracker.getThreadId());

        // Clear Ground Items Button
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta featherMeta = feather.getItemMeta();
        if (featherMeta != null) {
            featherMeta.setDisplayName(ChatColor.YELLOW + "Clear Ground Items");
            featherMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Clears all dropped items currently resting",
                    ChatColor.GRAY + "on the ground in this region thread.",
                    "",
                    ChatColor.RED + "This runs safely inside the region thread."
            ));
            feather.setItemMeta(featherMeta);
        }
        inv.setItem(2, feather);

        // Halt/Resume Redstone & Hoppers Button
        boolean isHalted = com.regionsentry.pro.listener.MitigationListener.HALTED_REGIONS.contains(tracker.getRegionId());
        ItemStack haltButton = new ItemStack(isHalted ? Material.LEVER : Material.REDSTONE_TORCH);
        ItemMeta haltMeta = haltButton.getItemMeta();
        if (haltMeta != null) {
            if (isHalted) {
                haltMeta.setDisplayName(ChatColor.GREEN + "Resume Redstone/Hoppers");
                haltMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Currently: " + ChatColor.RED + "HALTED",
                        ChatColor.GRAY + "Click to resume block physics and hoppers",
                        ChatColor.GRAY + "in this region thread."
                ));
            } else {
                haltMeta.setDisplayName(ChatColor.RED + "Halt Redstone/Hoppers");
                haltMeta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Currently: " + ChatColor.GREEN + "ACTIVE",
                        ChatColor.GRAY + "Click to temporarily freeze block physics",
                        ChatColor.GRAY + "and hopper transfers in this region."
                ));
            }
            haltButton.setItemMeta(haltMeta);
        }
        inv.setItem(4, haltButton);

        // Purge Hostile Mobs Button
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.setDisplayName(ChatColor.RED + "Purge Hostile Mobs");
            swordMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Removes all hostile monster entities",
                    ChatColor.GRAY + "currently ticking in this region thread.",
                    "",
                    ChatColor.RED + "This runs safely inside the region thread."
            ));
            sword.setItemMeta(swordMeta);
        }
        inv.setItem(6, sword);

        // JFR Profiling Button
        ItemStack jfrButton = new ItemStack(Material.CLOCK);
        ItemMeta jfrMeta = jfrButton.getItemMeta();
        if (jfrMeta != null) {
            jfrMeta.setDisplayName(ChatColor.AQUA + "Generate Thread Profile");
            jfrMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Triggers a localized 10-second Java",
                    ChatColor.GRAY + "Flight Recorder (JFR) profile sampling",
                    ChatColor.GRAY + "only on this region's thread.",
                    "",
                    ChatColor.YELLOW + "Saved as a .jfr file in plugins/RegionSentry/profiles/"
            ));
            jfrButton.setItemMeta(jfrMeta);
        }
        inv.setItem(13, jfrButton);

        // Gray filler for others
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < 18; i++) {
            if (i != 2 && i != 4 && i != 6 && i != 13) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(TITLE_PREFIX)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();
        if (slot != 2 && slot != 4 && slot != 6 && slot != 13) return;

        RegionTracker tracker = playerTargets.get(player.getUniqueId());
        if (tracker == null || tracker.getChunks().isEmpty()) {
            player.sendMessage(ChatColor.RED + "Error: Target region thread not found or inactive.");
            player.closeInventory();
            return;
        }

        // Find a representative chunk to submit the task to
        ChunkKey rep = tracker.getChunks().iterator().next();
        World world = Bukkit.getWorld(rep.getWorldName());
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Error: World " + rep.getWorldName() + " is not loaded.");
            player.closeInventory();
            return;
        }

        if (slot == 13) {
            player.closeInventory();
            com.regionsentry.pro.profiler.JFRProfiler.runProfile(plugin, player, tracker.getRegionId());
            return;
        }

        if (slot == 4) {
            String regionId = tracker.getRegionId();
            if (com.regionsentry.pro.listener.MitigationListener.HALTED_REGIONS.contains(regionId)) {
                com.regionsentry.pro.listener.MitigationListener.HALTED_REGIONS.remove(regionId);
                player.sendMessage(ChatColor.GREEN + "[RegionSentry Pro] Resumed Redstone/Hoppers for region: " + regionId);
            } else {
                com.regionsentry.pro.listener.MitigationListener.HALTED_REGIONS.add(regionId);
                player.sendMessage(ChatColor.RED + "[RegionSentry Pro] Halted Redstone/Hoppers for region: " + regionId);
            }
            open(player, tracker);
            return;
        }

        player.closeInventory();

        // Halt any testing lag machines running in this region thread
        com.regionsentry.lite.command.LagMachineCommand.stopLagMachinesForChunks(tracker.getChunks());

        if (slot == 2) {
            // Clear items on the region thread context
            Bukkit.getRegionScheduler().execute(plugin, world, rep.getX(), rep.getZ(), () -> {
                int clearedCount = 0;
                for (ChunkKey key : tracker.getChunks()) {
                    if (world.isChunkLoaded(key.getX(), key.getZ())) {
                        Chunk chunk = world.getChunkAt(key.getX(), key.getZ());
                        for (Entity entity : chunk.getEntities()) {
                            if (entity instanceof Item) {
                                entity.remove();
                                clearedCount++;
                            }
                        }
                    }
                }
                final int totalCleared = clearedCount;
                player.sendMessage(ChatColor.GREEN + "[RegionSentry Pro] Thread-safely cleared " + totalCleared + " ground items in region " + tracker.getThreadId() + ".");
            });
        } else {
            // Purge mobs on the region thread context
            Bukkit.getRegionScheduler().execute(plugin, world, rep.getX(), rep.getZ(), () -> {
                int purgedCount = 0;
                for (ChunkKey key : tracker.getChunks()) {
                    if (world.isChunkLoaded(key.getX(), key.getZ())) {
                        Chunk chunk = world.getChunkAt(key.getX(), key.getZ());
                        for (Entity entity : chunk.getEntities()) {
                            if (entity instanceof Monster) {
                                entity.remove();
                                purgedCount++;
                            }
                        }
                    }
                }
                final int totalPurged = purgedCount;
                player.sendMessage(ChatColor.GREEN + "[RegionSentry Pro] Thread-safely purged " + totalPurged + " hostile mobs in region " + tracker.getThreadId() + ".");
            });
        }
    }
}

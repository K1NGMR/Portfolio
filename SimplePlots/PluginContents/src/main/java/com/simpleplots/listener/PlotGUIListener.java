package com.simpleplots.listener;

import com.simpleplots.SimplePlots;
import com.simpleplots.api.Plot;
import com.simpleplots.gui.PlotGUI;
import com.simpleplots.util.Messages;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Handles all click events within the Plot Management GUIs.
 */
public class PlotGUIListener implements Listener {
    private final SimplePlots plugin;

    public PlotGUIListener(SimplePlots plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        
        String plotChestTitlePrefix = ChatColor.translateAlternateColorCodes('&', "&6Plot Chest");
        if (title.startsWith(plotChestTitlePrefix)) {
            handlePlotChestClick(event);
            return;
        }

        String adminTitle = ChatColor.translateAlternateColorCodes('&', "&cAdmin settings panel");
        if (title.equals(adminTitle)) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            int slot = event.getSlot();
            if (slot == 22) {
                player.closeInventory();
            } else if (slot == 11) {
                boolean current = plugin.getConfig().getBoolean("merged-roads.build-enabled", true);
                plugin.getConfig().set("merged-roads.build-enabled", !current);
                plugin.saveConfig();
                
                player.sendMessage(ChatColor.GREEN + "Toggled build on merged roads to: " + !current);
                
                org.bukkit.command.CommandExecutor exec = plugin.getCommand("plot").getExecutor();
                if (exec instanceof com.simpleplots.commands.PlotCommand) {
                    ((com.simpleplots.commands.PlotCommand) exec).handleAdminPanel(player);
                }
            }
            return;
        }
        
        String mainTitle = Messages.get("gui.main-title");
        if (mainTitle.isEmpty()) mainTitle = ChatColor.DARK_GREEN + "Plot Management";

        String flagsTitle = Messages.get("gui.flags-title");
        if (flagsTitle.isEmpty()) flagsTitle = ChatColor.BLUE + "Configure Plot Flags";

        String biomesTitle = Messages.get("gui.biomes-title");
        if (biomesTitle.isEmpty()) biomesTitle = ChatColor.DARK_AQUA + "Select Plot Biome";

        if (!title.equals(mainTitle) && 
            !title.equals(flagsTitle) && 
            !title.equals(biomesTitle)) {
            return;
        }

        event.setCancelled(true); // Disable dragging/taking items
        
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        Plot plot = plugin.getPlotAPI().getPlotAt(player.getLocation());

        if (title.equals(mainTitle)) {
            handleMainClick(player, event.getSlot(), plot);
        } else if (title.equals(flagsTitle)) {
            handleFlagsClick(player, event.getSlot(), plot);
        } else if (title.equals(biomesTitle)) {
            handleBiomesClick(player, event.getSlot());
        }
    }

    private void handleMainClick(Player player, int slot, Plot plot) {
        if (slot == 31) {
            player.closeInventory();
            return;
        }

        if (plot == null) {
            if (slot == 13) {
                player.performCommand("plot claim");
                player.closeInventory();
            }
            return;
        }

        // Must own or be admin to modify via GUI
        if (!plot.getOwner().equals(player.getUniqueId()) && !player.hasPermission("plots.admin")) {
            player.sendMessage(ChatColor.RED + "You must own this plot to manage it via GUI.");
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 12:
                PlotGUI.openFlagsMenu(player, plot);
                break;
            case 14:
                PlotGUI.openBiomesMenu(player);
                break;
            case 16:
                player.closeInventory();
                player.performCommand("plot clear");
                break;
        }
    }

    private void handleFlagsClick(Player player, int slot, Plot plot) {
        if (slot == 22) {
            PlotGUI.openMainMenu(player);
            return;
        }

        if (plot == null) return;

        String flag = null;
        switch (slot) {
            case 10: flag = "pvp"; break;
            case 11: flag = "pve"; break;
            case 12: flag = "fly"; break;
            case 13: flag = "explosion"; break;
            case 14: flag = "redstone"; break;
            case 15: flag = "crop-grow"; break;
            case 16: flag = "leaf-decay"; break;
        }

        if (flag != null) {
            boolean currentVal = true;
            if (plot.hasFlag(flag)) {
                currentVal = plot.getFlagValue(flag).equalsIgnoreCase("true");
            }
            boolean newVal = !currentVal;

            plot.setFlag(flag, String.valueOf(newVal));
            plugin.getDatabaseManager().savePlotFlag(plot.getWorld(), plot.getId(), flag, String.valueOf(newVal));
            player.sendMessage(ChatColor.GREEN + "Toggled flag " + flag.toUpperCase() + " to: " + newVal);
            
            // Refresh inventory
            PlotGUI.openFlagsMenu(player, plot);
        }
    }

    private void handleBiomesClick(Player player, int slot) {
        if (slot == 22) {
            PlotGUI.openMainMenu(player);
            return;
        }

        String biomeStr = null;
        switch (slot) {
            case 10: biomeStr = "PLAINS"; break;
            case 11: biomeStr = "DESERT"; break;
            case 12: biomeStr = "FOREST"; break;
            case 13: biomeStr = "OCEAN"; break;
            case 14: biomeStr = "NETHER_WASTES"; break;
            case 15: biomeStr = "THE_END"; break;
            case 16: biomeStr = "SNOWY_PLAINS"; break;
        }

        if (biomeStr != null) {
            player.closeInventory();
            player.performCommand("plot biome " + biomeStr);
        }
    }

    private void handlePlotChestClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        SimplePlots.PlotChestSession session = plugin.getActiveChestSessions().get(player.getUniqueId());
        if (session == null) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        InventoryClicked(event, player, session);
    }

    private void InventoryClicked(InventoryClickEvent event, Player player, SimplePlots.PlotChestSession session) {
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        InventoryAction action = event.getAction();
        
        if (clickedInv.equals(event.getView().getTopInventory())) {
            // Clicked inside the plot chest GUI
            int slot = event.getSlot();
            if (slot >= 45 && slot <= 53) {
                event.setCancelled(true); // Cancel control panel buttons
                if (slot == 45) {
                    // Previous Page
                    handlePageChange(player, session, -1);
                } else if (slot == 53) {
                    // Next Page
                    handlePageChange(player, session, 1);
                } else if (slot == 49) {
                    // Close button
                    player.closeInventory();
                }
                return;
            }
            
            // Block placing/swapping in the GUI
            if (action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE || 
                action == InventoryAction.PLACE_SOME || action == InventoryAction.SWAP_WITH_CURSOR ||
                action == InventoryAction.COLLECT_TO_CURSOR) {
                event.setCancelled(true);
            }
        } else {
            // Clicked inside the player's own inventory
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                // Block shift clicking items into the plot chest
                event.setCancelled(true);
            }
        }
    }

    private void handlePageChange(Player player, SimplePlots.PlotChestSession session, int delta) {
        saveCurrentPageToSession(player, session);
        session.page += delta;
        
        // Recreate and open new inventory
        // Retrieve PlotCommand to reuse its createPlotChestInventory method
        org.bukkit.command.CommandExecutor exec = plugin.getCommand("plot").getExecutor();
        if (exec instanceof com.simpleplots.commands.PlotCommand) {
            com.simpleplots.commands.PlotCommand plotCmd = (com.simpleplots.commands.PlotCommand) exec;
            Inventory inv = plotCmd.createPlotChestInventory(player, session);
            player.openInventory(inv);
        } else {
            player.closeInventory();
        }
    }

    private void saveCurrentPageToSession(Player player, SimplePlots.PlotChestSession session) {
        Inventory top = player.getOpenInventory().getTopInventory();
        int startIndex = (session.page - 1) * 45;
        for (int i = 0; i < 45; i++) {
            ItemStack item = top.getItem(i);
            int itemIndex = startIndex + i;
            
            // Expand session.items if needed to allow setting values
            while (session.items.size() <= itemIndex) {
                session.items.add(new ItemStack(Material.AIR));
            }
            session.items.set(itemIndex, item != null ? item.clone() : new ItemStack(Material.AIR));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        
        String plotChestTitlePrefix = ChatColor.translateAlternateColorCodes('&', "&6Plot Chest");
        if (!title.startsWith(plotChestTitlePrefix)) {
            return;
        }

        SimplePlots.PlotChestSession session = plugin.getActiveChestSessions().remove(player.getUniqueId());
        if (session != null) {
            saveCurrentPageToSession(player, session);
            
            // Save remaining items back to file
            List<ItemStack> remaining = new ArrayList<>();
            for (ItemStack item : session.items) {
                if (item != null && item.getType() != Material.AIR) {
                    remaining.add(item);
                }
            }
            plugin.saveAllToPlotChest(player.getUniqueId(), remaining);
            if (remaining.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "Your Plot Chest is now empty.");
            } else {
                player.sendMessage(ChatColor.GREEN + "Plot Chest contents saved successfully (" + remaining.size() + " items remaining).");
            }
        }
    }
}

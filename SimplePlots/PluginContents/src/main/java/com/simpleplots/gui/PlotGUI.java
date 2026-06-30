package com.simpleplots.gui;

import com.simpleplots.SimplePlots;
import com.simpleplots.api.Plot;
import com.simpleplots.api.PlotGeometry;
import com.simpleplots.api.PlotId;
import com.simpleplots.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles creation and rendering of Plot Management GUIs.
 */
public class PlotGUI {

    /**
     * Opens the main plot management GUI.
     */
    public static void openMainMenu(Player player) {
        String title = Messages.get("gui.main-title");
        if (title.isEmpty()) title = ChatColor.DARK_GREEN + "Plot Management";
        
        Inventory inv = Bukkit.createInventory(null, 36, title);
        Plot plot = SimplePlots.getInstance().getPlotAPI().getPlotAt(player.getLocation());

        // Fill background with grey glass panes
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, filler);
        }

        if (plot == null) {
            PlotId plotId = PlotGeometry.getPlotId(player.getLocation());
            if (plotId != null && !PlotGeometry.isRoad(player.getLocation())) {
                // Standing on unclaimed plot
                inv.setItem(13, createItemFromConfig(Material.GRASS_BLOCK, "gui.items.claim-plot", 
                        "{id}", plotId.toString()));
            } else {
                // Standing on road
                inv.setItem(13, createItemFromConfig(Material.BARRIER, "gui.items.no-plot"));
            }
        } else {
            // Standing on claimed plot
            String ownerName = SimplePlots.getInstance().getUuidCache().getName(plot.getOwner());
            
            String displayName = plot.hasFlag("display-name") ? plot.getFlagValue("display-name") : "None";
            
            // Slot 10: Plot Info
            inv.setItem(10, createItemFromConfig(Material.BOOK, "gui.items.plot-info", 
                    "{id}", plot.getId().toString(),
                    "{owner}", ownerName,
                    "{name}", displayName,
                    "{trusted}", String.valueOf(plot.getTrusted().size()),
                    "{added}", String.valueOf(plot.getAdded().size()),
                    "{flags}", String.valueOf(plot.getFlags().size())));

            // Slot 12: Flags
            inv.setItem(12, createItemFromConfig(Material.REDSTONE_TORCH, "gui.items.plot-flags"));

            // Slot 14: Biomes
            inv.setItem(14, createItemFromConfig(Material.OAK_SAPLING, "gui.items.change-biome"));

            // Slot 16: Clear/Delete Plot
            inv.setItem(16, createItemFromConfig(Material.TNT, "gui.items.reset-plot"));
        }

        // Slot 31: Close
        String closeText = Messages.get("gui.close-menu");
        inv.setItem(31, createItem(Material.BARRIER, closeText.isEmpty() ? ChatColor.DARK_RED + "Close Menu" : closeText));

        player.openInventory(inv);
    }

    /**
     * Opens the flags configuration menu.
     */
    public static void openFlagsMenu(Player player, Plot plot) {
        String title = Messages.get("gui.flags-title");
        if (title.isEmpty()) title = ChatColor.BLUE + "Configure Plot Flags";

        Inventory inv = Bukkit.createInventory(null, 27, title);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Add typical toggleable flags
        inv.setItem(10, createFlagItem(Material.IRON_SWORD, "pvp", plot));
        inv.setItem(11, createFlagItem(Material.DIAMOND_AXE, "pve", plot));
        inv.setItem(12, createFlagItem(Material.FEATHER, "fly", plot));
        inv.setItem(13, createFlagItem(Material.TNT, "explosion", plot));
        inv.setItem(14, createFlagItem(Material.REDSTONE, "redstone", plot));
        inv.setItem(15, createFlagItem(Material.WHEAT, "crop-grow", plot));
        inv.setItem(16, createFlagItem(Material.OAK_LEAVES, "leaf-decay", plot));

        // Slot 22: Go back
        String backText = Messages.get("gui.go-back");
        inv.setItem(22, createItem(Material.ARROW, backText.isEmpty() ? ChatColor.YELLOW + "Go Back" : backText));

        player.openInventory(inv);
    }

    /**
     * Opens the biome selection menu.
     */
    public static void openBiomesMenu(Player player) {
        String title = Messages.get("gui.biomes-title");
        if (title.isEmpty()) title = ChatColor.DARK_AQUA + "Select Plot Biome";

        Inventory inv = Bukkit.createInventory(null, 27, title);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Biome choices
        inv.setItem(10, createBiomeItem(Material.GRASS_BLOCK, Biome.PLAINS));
        inv.setItem(11, createBiomeItem(Material.SAND, Biome.DESERT));
        inv.setItem(12, createBiomeItem(Material.OAK_LOG, Biome.FOREST));
        inv.setItem(13, createBiomeItem(Material.WATER_BUCKET, Biome.OCEAN));
        inv.setItem(14, createBiomeItem(Material.NETHERRACK, Biome.NETHER_WASTES));
        inv.setItem(15, createBiomeItem(Material.END_STONE, Biome.THE_END));
        inv.setItem(16, createBiomeItem(Material.SNOW_BLOCK, Biome.SNOWY_PLAINS));

        // Slot 22: Go back
        String backText = Messages.get("gui.go-back");
        inv.setItem(22, createItem(Material.ARROW, backText.isEmpty() ? ChatColor.YELLOW + "Go Back" : backText));

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> list = new ArrayList<>();
            for (String line : lore) {
                list.add(line);
            }
            meta.setLore(list);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createItemFromConfig(Material material, String key, Object... replacements) {
        String name = Messages.get(key + ".name");
        List<String> rawLore = Messages.getList(key + ".lore");
        List<String> formattedLore = new ArrayList<>();
        
        for (String line : rawLore) {
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    line = line.replace(String.valueOf(replacements[i]), String.valueOf(replacements[i + 1]));
                }
            }
            formattedLore.add(line);
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name.isEmpty() ? material.name() : name);
            meta.setLore(formattedLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createFlagItem(Material material, String flagName, Plot plot) {
        boolean enabled = true;
        if (plot.hasFlag(flagName)) {
            enabled = plot.getFlagValue(flagName).equalsIgnoreCase("true");
        } else {
            if (flagName.equals("no-portals")) enabled = false;
        }

        String statusStr = enabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED";
        String togglePrompt = ChatColor.YELLOW + "Click to toggle this flag.";

        return createItem(material, ChatColor.GOLD + flagName.toUpperCase(), 
                ChatColor.GRAY + "Status: " + statusStr, 
                togglePrompt);
    }

    private static ItemStack createBiomeItem(Material material, Biome biome) {
        return createItem(material, ChatColor.GOLD + biome.name(), 
                ChatColor.GRAY + "Click to change this plot's biome to " + biome.name() + ".");
    }
}

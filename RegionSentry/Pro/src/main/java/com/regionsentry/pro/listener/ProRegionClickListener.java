package com.regionsentry.pro.listener;

import com.regionsentry.lite.gui.RegionClickListener;
import com.regionsentry.lite.monitor.ChunkKey;
import com.regionsentry.lite.monitor.RegionTracker;
import com.regionsentry.pro.gui.MitigationMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ProRegionClickListener implements RegionClickListener {
    private final JavaPlugin plugin;
    private final MitigationMenu mitigationMenu;

    public ProRegionClickListener(JavaPlugin plugin, MitigationMenu mitigationMenu) {
        this.plugin = plugin;
        this.mitigationMenu = mitigationMenu;
    }

    @Override
    public void onLeftClick(Player player, RegionTracker tracker) {
        if (tracker.getChunks().isEmpty()) {
            player.sendMessage(ChatColor.RED + "Cannot teleport: No active chunks in this region thread.");
            return;
        }

        // Get a representative chunk coordinate
        ChunkKey key = tracker.getChunks().iterator().next();
        World world = Bukkit.getWorld(key.getWorldName());
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Cannot teleport: World is not loaded.");
            return;
        }

        Location target = new Location(world, key.getX() * 16 + 8, 100, key.getZ() * 16 + 8);
        player.sendMessage(ChatColor.YELLOW + "[RegionSentry Pro] Initiating asynchronous teleport to region thread center...");
        
        // Folia thread-safe teleportation pattern
        player.teleportAsync(target).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                player.sendMessage(ChatColor.GREEN + "[RegionSentry Pro] Asynchronously teleported to region thread boundaries.");
            } else {
                player.sendMessage(ChatColor.RED + "[RegionSentry Pro] Asynchronous teleport failed.");
            }
        });
    }

    @Override
    public void onRightClick(Player player, RegionTracker tracker) {
        mitigationMenu.open(player, tracker);
    }
}

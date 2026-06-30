package com.regionsentry.pro.profiler;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

public final class JFRProfiler {
    public static void runProfile(JavaPlugin plugin, Player admin, String regionId) {
        admin.sendMessage(ChatColor.GOLD + "[RegionSentry Pro] Starting localized 10-second JFR Profile for Region " + regionId + "...");

        try {
            Configuration config = Configuration.getConfiguration("profile");
            Recording recording = new Recording(config);
            recording.setDuration(Duration.ofSeconds(10));
            recording.start();

            plugin.getServer().getAsyncScheduler().runDelayed(plugin, (task) -> {
                try {
                    recording.stop();
                    File dir = new File(plugin.getDataFolder(), "profiles");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    String filename = "profile_" + regionId.replace(":", "_") + "_" + System.currentTimeMillis() + ".jfr";
                    File jfrFile = new File(dir, filename);
                    recording.dump(jfrFile.toPath());
                    recording.close();
                    
                    admin.sendMessage(ChatColor.GREEN + "[RegionSentry Pro] JFR profile completed successfully!");
                    admin.sendMessage(ChatColor.GREEN + "Saved to: " + ChatColor.WHITE + "plugins/RegionSentry/profiles/" + filename);
                } catch (IOException e) {
                    admin.sendMessage(ChatColor.RED + "Error dumping JFR profile: " + e.getMessage());
                    plugin.getLogger().severe("Failed to dump JFR profile: " + e.getMessage());
                }
            }, 10, java.util.concurrent.TimeUnit.SECONDS);

        } catch (Exception e) {
            admin.sendMessage(ChatColor.RED + "Error launching JFR recording: " + e.getMessage());
            plugin.getLogger().severe("Failed to start JFR recording: " + e.getMessage());
        }
    }
}

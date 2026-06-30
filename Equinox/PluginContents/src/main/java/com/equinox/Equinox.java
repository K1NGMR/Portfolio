package com.equinox;

import com.equinox.commands.EquinoxCommand;
import com.equinox.modules.AntiFreecamModule;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Equinox extends JavaPlugin {

    private AntiFreecamModule antiFreecamModule;
    private boolean bypassObfuscation = false;

    public boolean isBypassObfuscation() {
        return bypassObfuscation;
    }

    public void setBypassObfuscation(boolean bypassObfuscation) {
        this.bypassObfuscation = bypassObfuscation;
    }

    @Override
    public void onEnable() {
        // Save default config if not present
        saveDefaultConfig();

        getLogger().info("=========================================");
        getLogger().info("  Equinox Security & Administration Suite");
        getLogger().info("  Status: Initializing Core Modules...");
        getLogger().info("=========================================");

        // Load modules based on configuration toggles
        loadModules();

        // Register Command Executor & Listener
        if (getCommand("equinox") != null) {
            EquinoxCommand equinoxCmd = new EquinoxCommand(this);
            getCommand("equinox").setExecutor(equinoxCmd);
            Bukkit.getPluginManager().registerEvents(equinoxCmd, this);
        }

        // Periodically update the Cave Reachability Cache on the main thread (every 5 ticks / 250ms)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                com.equinox.util.CaveReachabilityManager.updateReachableBlocks(player);
            }
        }, 20L, 5L);

        // Update exposed ore cache on a slower schedule (every 40 ticks / 2 seconds)
        // Scans 3x3 chunks around each player below y-threshold for exposed ores
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (antiFreecamModule == null) return;
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                int pcx = player.getLocation().getBlockX() >> 4;
                int pcz = player.getLocation().getBlockZ() >> 4;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        final int cx = pcx + dx;
                        final int cz = pcz + dz;
                        // Dispatch each chunk update back to main thread for safe block access
                        Bukkit.getScheduler().runTask(this, () -> {
                            if (player.getWorld().isChunkLoaded(cx, cz)) {
                                antiFreecamModule.updateExposedOreCache(player.getWorld(), cx, cz);
                            }
                        });
                    }
                }
            }
        }, 40L, 40L);

        getLogger().info("Equinox enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (antiFreecamModule != null) antiFreecamModule.stop();

        // Clear player cache to prevent memory leaks
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            com.equinox.util.CaveReachabilityManager.removePlayer(player.getUniqueId());
        }

        getLogger().info("Equinox disabling and clean up in progress...");
    }

    public AntiFreecamModule getAntiFreecamModule() {
        return antiFreecamModule;
    }

    public void reloadPluginConfig() {
        reloadConfig();
        if (antiFreecamModule != null) {
            antiFreecamModule.loadConfig();
        }
    }

    private void loadModules() {
        // Check if ProtocolLib is available
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().severe("========================================");
            getLogger().severe("FATAL ERROR: ProtocolLib NOT FOUND!");
            getLogger().severe("========================================");
            getLogger().severe("Equinox requires ProtocolLib v5.0+ to function.");
            getLogger().severe("Download: https://github.com/dmulloy2/ProtocolLib/releases");
            getLogger().severe("Place the JAR in your plugins folder and restart.");
            getLogger().severe("========================================");
            setEnabled(false);
            return;
        }

        // Anti-Freecam & Core Packet Obfuscation
        antiFreecamModule = new AntiFreecamModule(this);
        Bukkit.getPluginManager().registerEvents(antiFreecamModule, this);
        if (getConfig().getBoolean("anti-freecam.enabled", true)) {
            getLogger().info("[Module] Anti-Freecam enabled.");
            antiFreecamModule.start();
        }
    }
}

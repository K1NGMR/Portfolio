package com.regionsentry.lite.hud;

import com.regionsentry.lite.monitor.PerformanceMonitor;
import com.regionsentry.lite.monitor.RegionTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HUDManager {
    private final JavaPlugin plugin;
    private final PerformanceMonitor monitor;
    private final Set<UUID> enabledPlayers = ConcurrentHashMap.newKeySet();
    private BossBar bossBar;

    public HUDManager(JavaPlugin plugin, PerformanceMonitor monitor) {
        this.plugin = plugin;
        this.monitor = monitor;
    }

    public void start() {
        // Create the BossBar
        bossBar = Bukkit.createBossBar(
                ChatColor.GRAY + "RegionSentry - Initializing...",
                BarColor.GREEN,
                BarStyle.SOLID
        );
        bossBar.setVisible(false);

        // Run HUD update task every 1 second (20 ticks) asynchronously
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (task) -> {
            updateHUD();
        }, 1, 1, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void toggleHUD(Player player) {
        UUID uuid = player.getUniqueId();
        if (enabledPlayers.contains(uuid)) {
            enabledPlayers.remove(uuid);
            bossBar.removePlayer(player);
            player.sendMessage(ChatColor.YELLOW + "[RegionSentry] Performance HUD disabled.");
            if (enabledPlayers.isEmpty()) {
                bossBar.setVisible(false);
            }
        } else {
            enabledPlayers.add(uuid);
            bossBar.addPlayer(player);
            bossBar.setVisible(true);
            player.sendMessage(ChatColor.GREEN + "[RegionSentry] Performance HUD enabled.");
        }
    }

    private void updateHUD() {
        if (enabledPlayers.isEmpty()) {
            if (bossBar.isVisible()) {
                bossBar.setVisible(false);
            }
            return;
        }

        // Find the worst performing region thread (highest MSPT)
        RegionTracker worst = null;
        double maxMspt = -1.0;
        for (RegionTracker tracker : monitor.getTrackers()) {
            double mspt = tracker.getAverageMSPT();
            if (mspt > maxMspt) {
                maxMspt = mspt;
                worst = tracker;
            }
        }

        if (worst == null) {
            bossBar.setTitle(ChatColor.GRAY + "RegionSentry - Mapped Threads: 0");
            bossBar.setColor(BarColor.GREEN);
            bossBar.setProgress(0.0);
            return;
        }

        double tps = worst.getAverageTPS();
        long threadId = worst.getThreadId();
        int players = worst.getPlayerCount();

        BarColor color;
        ChatColor textChatColor;
        if (maxMspt < 30.0) {
            color = BarColor.GREEN;
            textChatColor = ChatColor.GREEN;
        } else if (maxMspt < 50.0) {
            color = BarColor.YELLOW;
            textChatColor = ChatColor.GOLD;
        } else {
            color = BarColor.RED;
            textChatColor = ChatColor.RED;
        }

        String title = textChatColor + ChatColor.BOLD.toString() + "Worst Region #" + threadId + " " + ChatColor.DARK_GRAY + "» " +
                ChatColor.GRAY + "MSPT: " + textChatColor + String.format("%.1f ms", maxMspt) + ChatColor.GRAY + " | " +
                ChatColor.GRAY + "TPS: " + textChatColor + String.format("%.1f", tps) + ChatColor.GRAY + " | " +
                ChatColor.GRAY + "Players: " + ChatColor.WHITE + players;

        bossBar.setTitle(title);
        bossBar.setColor(color);
        bossBar.setProgress(Math.min(1.0, maxMspt / 100.0));
        if (!bossBar.isVisible()) {
            bossBar.setVisible(true);
        }
    }

    public void cleanup() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
        enabledPlayers.clear();
    }
}

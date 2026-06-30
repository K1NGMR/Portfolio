package com.regionsentry.pro.alert;

import com.regionsentry.lite.monitor.ChunkKey;
import com.regionsentry.lite.monitor.PerformanceMonitor;
import com.regionsentry.lite.monitor.RegionTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AlertManager {
    private final JavaPlugin plugin;
    private final PerformanceMonitor monitor;
    private final Map<String, Integer> lowTpsDurations = new ConcurrentHashMap<>();
    private final Set<String> activeAlerts = ConcurrentHashMap.newKeySet();

    public AlertManager(JavaPlugin plugin, PerformanceMonitor monitor) {
        this.plugin = plugin;
        this.monitor = monitor;
    }

    public void start() {
        int checkSec = plugin.getConfig().getInt("alert-manager.check-interval-seconds", 5);
        if (checkSec < 1) checkSec = 1;
        final int finalCheckSec = checkSec;
        // Run performance monitoring check asynchronously
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (scheduledTask) -> {
            checkPerformance(finalCheckSec);
        }, finalCheckSec, finalCheckSec, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void checkPerformance(int intervalSec) {
        double tpsThreshold = plugin.getConfig().getDouble("alert-manager.tps-threshold", 15.0);
        int durationThreshold = plugin.getConfig().getInt("alert-manager.alert-duration-seconds", 30);

        for (RegionTracker tracker : monitor.getTrackers()) {
            double tps = tracker.getAverageTPS();
            String regionId = tracker.getRegionId();

            if (tps < tpsThreshold) {
                int duration = lowTpsDurations.merge(regionId, intervalSec, Integer::sum);
                if (duration >= durationThreshold) {
                    if (!activeAlerts.contains(regionId)) {
                        activeAlerts.add(regionId);
                        triggerAlert(tracker, tps);
                    }
                }
            } else {
                lowTpsDurations.remove(regionId);
                if (activeAlerts.remove(regionId)) {
                    plugin.getLogger().info("Region " + regionId + " has recovered to stable TPS (" + String.format("%.2f", tps) + ").");
                }
            }
        }
    }

    private void triggerAlert(RegionTracker tracker, double tps) {
        String worldName = "Unknown";
        int minX = 0, maxX = 0, minZ = 0, maxZ = 0;
        Set<ChunkKey> chunks = tracker.getChunks();
        
        if (!chunks.isEmpty()) {
            minX = Integer.MAX_VALUE;
            maxX = Integer.MIN_VALUE;
            minZ = Integer.MAX_VALUE;
            maxZ = Integer.MIN_VALUE;
            for (ChunkKey key : chunks) {
                worldName = key.getWorldName();
                if (key.getX() < minX) minX = key.getX();
                if (key.getX() > maxX) maxX = key.getX();
                if (key.getZ() < minZ) minZ = key.getZ();
                if (key.getZ() > maxZ) maxZ = key.getZ();
            }
        }

        String boundaryStr = "World: " + worldName + ", Chunks: X[" + minX + " to " + maxX + "], Z[" + minZ + " to " + maxZ + "]";
        String blockBoundaryStr = "Blocks: X[" + (minX * 16) + " to " + (maxX * 16 + 15) + "], Z[" + (minZ * 16) + " to " + (maxZ * 16 + 15) + "]";

        String alertMessage = ChatColor.RED + ChatColor.BOLD.toString() + "[ALERT] Region Sentry detected severe lag!\n" +
                ChatColor.GRAY + "Thread ID: " + ChatColor.WHITE + tracker.getThreadId() + "\n" +
                ChatColor.GRAY + "Current TPS: " + ChatColor.RED + String.format("%.2f", tps) + "\n" +
                ChatColor.GRAY + "MSPT: " + ChatColor.RED + String.format("%.2f ms", tracker.getAverageMSPT()) + "\n" +
                ChatColor.GRAY + "Entities: " + ChatColor.WHITE + tracker.getEntityCount() + " | Players: " + ChatColor.WHITE + tracker.getPlayerCount() + "\n" +
                ChatColor.GRAY + boundaryStr;

        // 1. Alert Online Staff
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("regionsentry.admin")) {
                p.sendMessage(alertMessage);
            }
        }

        // 2. Dispatch Discord Webhook
        String webhookUrl = plugin.getConfig().getString("discord-webhook-url", "");
        if (webhookUrl != null && !webhookUrl.isEmpty() && !webhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) {
            sendWebhook(webhookUrl, tracker, tps, boundaryStr, blockBoundaryStr);
        }

        // 3. Dispatch Proxy Rerouting Alert
        dispatchProxyRedirection(tracker);
    }

    private void dispatchProxyRedirection(RegionTracker tracker) {
        if (!plugin.getConfig().getBoolean("proxy-integration.enabled", false)) return;

        try {
            java.io.ByteArrayOutputStream byteStream = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(byteStream);

            out.writeUTF("RerouteStrainedRegion");
            out.writeUTF(tracker.getRegionId());
            out.writeDouble(tracker.getAverageMSPT());
            out.writeDouble(tracker.getAverageTPS());

            Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
            if (player != null) {
                player.sendPluginMessage(plugin, "regionsentry:proxy", byteStream.toByteArray());
                plugin.getLogger().info("[RegionSentry Pro] Dispatched proxy reroute alert to network gateway for region: " + tracker.getRegionId());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to dispatch proxy plugin message: " + e.getMessage());
        }
    }

    private void sendWebhook(String urlString, RegionTracker tracker, double tps, String chunksStr, String blocksStr) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Constructing JSON Payload manually to avoid extra dependencies
            String jsonPayload = "{"
                    + "\"embeds\": [{"
                    + "\"title\": \"⚠️ REGIONAL LAG ALERT\","
                    + "\"color\": 15158332,"
                    + "\"fields\": ["
                    + "{\"name\": \"Thread ID\", \"value\": \"" + tracker.getThreadId() + "\", \"inline\": true},"
                    + "{\"name\": \"Thread Name\", \"value\": \"" + tracker.getThreadName() + "\", \"inline\": true},"
                    + "{\"name\": \"TPS / MSPT\", \"value\": \"" + String.format("%.2f", tps) + " TPS / " + String.format("%.2f ms", tracker.getAverageMSPT()) + "\", \"inline\": false},"
                    + "{\"name\": \"Entities / Players\", \"value\": \"" + tracker.getEntityCount() + " / " + tracker.getPlayerCount() + "\", \"inline\": true},"
                    + "{\"name\": \"Boundaries\", \"value\": \"" + chunksStr + "\\n" + blocksStr + "\", \"inline\": false}"
                    + "]"
                    + "}]"
                    + "}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                plugin.getLogger().warning("Discord webhook returned an error response code: " + responseCode);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to dispatch Discord Webhook: " + e.getMessage());
        }
    }
}

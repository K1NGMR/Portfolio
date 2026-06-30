package com.simpleplots.util;

import com.simpleplots.SimplePlots;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles UUID-to-Username lookups with local caching and Mojang API fallback.
 */
public class UUIDCache {
    private final SimplePlots plugin;
    private final Map<UUID, String> cache = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameCache = new ConcurrentHashMap<>();
    private long lastMojangRequest = 0;

    public UUIDCache(SimplePlots plugin) {
        this.plugin = plugin;
        initTable();
    }

    private void initTable() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "CREATE TABLE IF NOT EXISTS uuid_cache (uuid VARCHAR(36) PRIMARY KEY, username VARCHAR(16) NOT NULL)";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.execute();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to create uuid_cache table: " + e.getMessage());
            }
        });
    }

    /**
     * Resolves a UUID to a Username.
     */
    public String getName(UUID uuid) {
        if (uuid == null) return "Unknown";
        if (cache.containsKey(uuid)) {
            return cache.get(uuid);
        }

        // Try Bukkit
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer.getName() != null) {
            cache.put(uuid, offlinePlayer.getName());
            saveToDb(uuid, offlinePlayer.getName());
            return offlinePlayer.getName();
        }

        // Try DB
        String dbName = loadFromDb(uuid);
        if (dbName != null) {
            cache.put(uuid, dbName);
            return dbName;
        }

        // Try Mojang API async to avoid blocking
        fetchFromMojang(uuid);

        return "Unknown";
    }

    /**
     * Resolves a Username to a UUID.
     */
    public UUID getUUID(String username) {
        if (username == null || username.isEmpty()) return null;
        for (Map.Entry<UUID, String> entry : cache.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(username)) {
                return entry.getKey();
            }
        }

        // Try Bukkit
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
        if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
            UUID uuid = offlinePlayer.getUniqueId();
            cache.put(uuid, offlinePlayer.getName());
            saveToDb(uuid, offlinePlayer.getName());
            return uuid;
        }

        // Try DB
        UUID dbUuid = loadFromDb(username);
        if (dbUuid != null) {
            cache.put(dbUuid, username);
            return dbUuid;
        }

        return null;
    }

    private void fetchFromMojang(UUID uuid) {
        long now = System.currentTimeMillis();
        if (now - lastMojangRequest < 2000) { // Rate limit: 2 seconds
            return;
        }
        lastMojangRequest = now;

        CompletableFuture.runAsync(() -> {
            try {
                String uuidStr = uuid.toString().replace("-", "");
                URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    JSONParser parser = new JSONParser();
                    JSONObject json = (JSONObject) parser.parse(new InputStreamReader(conn.getInputStream()));
                    String name = (String) json.get("name");
                    if (name != null) {
                        cache.put(uuid, name);
                        saveToDb(uuid, name);
                        plugin.getLogger().info("Resolved UUID " + uuid + " to " + name + " via Mojang API");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to fetch UUID from Mojang API: " + e.getMessage());
            }
        });
    }

    private String loadFromDb(UUID uuid) {
        String sql = "SELECT username FROM uuid_cache WHERE uuid = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load UUID from DB: " + e.getMessage());
        }
        return null;
    }

    private UUID loadFromDb(String name) {
        String sql = "SELECT uuid FROM uuid_cache WHERE username = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to load Username from DB: " + e.getMessage());
        }
        return null;
    }

    private void saveToDb(UUID uuid, String name) {
        CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO uuid_cache (uuid, username) VALUES (?, ?)";
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, name);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to save UUID to DB: " + e.getMessage());
            }
        });
    }
}

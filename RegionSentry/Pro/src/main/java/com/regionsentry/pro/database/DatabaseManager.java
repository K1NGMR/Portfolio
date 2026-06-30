package com.regionsentry.pro.database;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseManager {
    private final JavaPlugin plugin;
    private final File dbFile;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "history.db");
    }

    public void init() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Initialize SQLite database connection
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            
            try (Statement stmt = connection.createStatement()) {
                // Table for logging regional hot spots
                stmt.execute("CREATE TABLE IF NOT EXISTS mspt_logs (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "timestamp INTEGER, " +
                        "world TEXT, " +
                        "chunk_x INTEGER, " +
                        "chunk_z INTEGER, " +
                        "mspt REAL, " +
                        "players TEXT" +
                        ")");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize SQLite database: " + e.getMessage());
        }
    }

    public void logSpikeAsync(String world, int chunkX, int chunkZ, double mspt, List<String> players) {
        Bukkit.getAsyncScheduler().runNow(plugin, (task) -> {
            if (connection == null) return;
            
            String playerList = String.join(",", players);
            String query = "INSERT INTO mspt_logs (timestamp, world, chunk_x, chunk_z, mspt, players) VALUES (?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setLong(1, Instant.now().getEpochSecond());
                pstmt.setString(2, world);
                pstmt.setInt(3, chunkX);
                pstmt.setInt(4, chunkZ);
                pstmt.setDouble(5, mspt);
                pstmt.setString(6, playerList);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to log regional MSPT spike to database: " + e.getMessage());
            }
        });
    }

    public void cleanOldLogsAsync() {
        Bukkit.getAsyncScheduler().runNow(plugin, (task) -> {
            if (connection == null) return;
            
            // Delete logs older than 24 hours (86400 seconds)
            long cutoff = Instant.now().getEpochSecond() - 86400L;
            String query = "DELETE FROM mspt_logs WHERE timestamp < ?";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setLong(1, cutoff);
                int deleted = pstmt.executeUpdate();
                if (deleted > 0) {
                    plugin.getLogger().info("Cleaned " + deleted + " historical load logs older than 24 hours.");
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to clean old logs: " + e.getMessage());
            }
        });
    }

    public interface HistoryCallback {
        void onResult(List<HistoryRecord> records);
    }

    public void getTopSpikesAsync(int limit, HistoryCallback callback) {
        Bukkit.getAsyncScheduler().runNow(plugin, (task) -> {
            List<HistoryRecord> records = new ArrayList<>();
            if (connection == null) {
                callback.onResult(records);
                return;
            }

            long cutoff = Instant.now().getEpochSecond() - 86400L;
            String query = "SELECT world, chunk_x, chunk_z, MAX(mspt) as max_mspt, players, timestamp " +
                    "FROM mspt_logs " +
                    "WHERE timestamp >= ? " +
                    "GROUP BY world, chunk_x, chunk_z " +
                    "ORDER BY max_mspt DESC " +
                    "LIMIT ?";

            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setLong(1, cutoff);
                pstmt.setInt(2, limit);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        records.add(new HistoryRecord(
                                rs.getString("world"),
                                rs.getInt("chunk_x"),
                                rs.getInt("chunk_z"),
                                rs.getDouble("max_mspt"),
                                rs.getString("players"),
                                rs.getLong("timestamp")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to query top historical load spikes: " + e.getMessage());
            }
            
            callback.onResult(records);
        });
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }

    public static final class HistoryRecord {
        public final String world;
        public final int chunkX;
        public final int chunkZ;
        public final double maxMspt;
        public final String players;
        public final long timestamp;

        public HistoryRecord(String world, int chunkX, int chunkZ, double maxMspt, String players, long timestamp) {
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.maxMspt = maxMspt;
            this.players = players;
            this.timestamp = timestamp;
        }
    }
}

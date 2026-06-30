package com.simpleplots.db;

import com.simpleplots.SimplePlots;
import com.simpleplots.PlotAPI;
import com.simpleplots.api.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Full DAO abstraction layer and connection manager for SQLite and MySQL databases.
 */
public class DatabaseManager {
    private final SimplePlots plugin;
    private HikariDataSource dataSource;
    private String storageType;

    public DatabaseManager(SimplePlots plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the HikariCP pool and runs table creation queries.
     */
    public void init(FileConfiguration storageConfig) {
        this.storageType = storageConfig.getString("storage-type", "sqlite").toLowerCase();

        HikariConfig config = new HikariConfig();

        if (storageType.equals("mysql")) {
            String host = storageConfig.getString("mysql.host", "localhost");
            int port = storageConfig.getInt("mysql.port", 3306);
            String database = storageConfig.getString("mysql.database", "simpleplots");
            String username = storageConfig.getString("mysql.username", "root");
            String password = storageConfig.getString("mysql.password", "");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            config.setMaximumPoolSize(storageConfig.getInt("mysql.pool.maximum-pool-size", 10));
            config.setMinimumIdle(storageConfig.getInt("mysql.pool.minimum-idle", 2));
            config.setConnectionTimeout(storageConfig.getLong("mysql.pool.connection-timeout", 30000));
            config.setIdleTimeout(storageConfig.getLong("mysql.pool.idle-timeout", 600000));
            config.setMaxLifetime(storageConfig.getLong("mysql.pool.max-lifetime", 1800000));
        } else {
            // SQLite
            String dbFileName = storageConfig.getString("sqlite.file-name", "plots.db");
            File dbFile = new File(plugin.getDataFolder(), dbFileName);
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            config.setMaximumPoolSize(1); // SQLite only supports single-write, so pool size of 1 avoids locking issues
        }

        config.setPoolName("SimplePlotsPool");
        dataSource = new HikariDataSource(config);

        createTables();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void createTables() {
        String isMySQL = storageType.equals("mysql") ? "AUTO_INCREMENT" : "AUTOINCREMENT";
        String intType = storageType.equals("mysql") ? "TINYINT" : "INTEGER";

        String[] queries = {
            "CREATE TABLE IF NOT EXISTS plots (" +
                "id INTEGER PRIMARY KEY " + isMySQL + ", " +
                "world VARCHAR(64) NOT NULL, " +
                "plot_x INT NOT NULL, " +
                "plot_z INT NOT NULL, " +
                "owner_uuid VARCHAR(36) NOT NULL, " +
                "creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "merged_n " + intType + " DEFAULT 0, " +
                "merged_s " + intType + " DEFAULT 0, " +
                "merged_e " + intType + " DEFAULT 0, " +
                "merged_w " + intType + " DEFAULT 0, " +
                "UNIQUE(world, plot_x, plot_z)" +
            ");",

            "CREATE TABLE IF NOT EXISTS plot_flags (" +
                "world VARCHAR(64) NOT NULL, " +
                "plot_x INT NOT NULL, " +
                "plot_z INT NOT NULL, " +
                "flag_key VARCHAR(64) NOT NULL, " +
                "flag_value TEXT NOT NULL, " +
                "PRIMARY KEY(world, plot_x, plot_z, flag_key)" +
            ");",

            "CREATE TABLE IF NOT EXISTS plot_members (" +
                "world VARCHAR(64) NOT NULL, " +
                "plot_x INT NOT NULL, " +
                "plot_z INT NOT NULL, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "PRIMARY KEY(world, plot_x, plot_z, player_uuid)" +
            ");",

            "CREATE TABLE IF NOT EXISTS plot_trusted (" +
                "world VARCHAR(64) NOT NULL, " +
                "plot_x INT NOT NULL, " +
                "plot_z INT NOT NULL, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "PRIMARY KEY(world, plot_x, plot_z, player_uuid)" +
            ");",

            "CREATE TABLE IF NOT EXISTS plot_denied (" +
                "world VARCHAR(64) NOT NULL, " +
                "plot_x INT NOT NULL, " +
                "plot_z INT NOT NULL, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "PRIMARY KEY(world, plot_x, plot_z, player_uuid)" +
            ");",

            "CREATE TABLE IF NOT EXISTS plot_backups (" +
                "id INTEGER PRIMARY KEY " + isMySQL + ", " +
                "world VARCHAR(64) NOT NULL, " +
                "plot_x INT NOT NULL, " +
                "plot_z INT NOT NULL, " +
                "timestamp BIGINT NOT NULL, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "file_path TEXT NOT NULL" +
            ");",

            "CREATE TABLE IF NOT EXISTS plot_clusters (" +
                "id INTEGER PRIMARY KEY " + isMySQL + ", " +
                "name VARCHAR(64) NOT NULL UNIQUE, " +
                "world VARCHAR(64) NOT NULL, " +
                "min_x INT NOT NULL, " +
                "min_z INT NOT NULL, " +
                "max_x INT NOT NULL, " +
                "max_z INT NOT NULL, " +
                "owner_uuid VARCHAR(36) NOT NULL" +
            ");",

            "CREATE TABLE IF NOT EXISTS player_activity (" +
                "player_uuid VARCHAR(36) PRIMARY KEY, " +
                "last_seen BIGINT NOT NULL" +
            ");",

            "CREATE TABLE IF NOT EXISTS plot_ratings (" +
                "world VARCHAR(64) NOT NULL, " +
                "plot_x INT NOT NULL, " +
                "plot_z INT NOT NULL, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "rating INT NOT NULL, " +
                "PRIMARY KEY(world, plot_x, plot_z, player_uuid)" +
            ");",

            "CREATE TABLE IF NOT EXISTS plot_comments (" +
                "id INTEGER PRIMARY KEY " + isMySQL + ", " +
                "world VARCHAR(64) NOT NULL, " +
                "plot_x INT NOT NULL, " +
                "plot_z INT NOT NULL, " +
                "commenter_uuid VARCHAR(36) NOT NULL, " +
                "commenter_name VARCHAR(16) NOT NULL, " +
                "comment_text VARCHAR(200) NOT NULL, " +
                "timestamp BIGINT NOT NULL" +
            ");"
        };

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : queries) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads all plots, flags, and members from database into PlotAPI cache.
     */
    public void loadAllPlots() {
        PlotAPI api = plugin.getPlotAPI();
        api.clearCache();

        String selectPlots = "SELECT * FROM plots";
        String selectFlags = "SELECT * FROM plot_flags";
        String selectMembers = "SELECT * FROM plot_members";
        String selectTrusted = "SELECT * FROM plot_trusted";
        String selectDenied = "SELECT * FROM plot_denied";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Load Plots
            try (ResultSet rs = stmt.executeQuery(selectPlots)) {
                while (rs.next()) {
                    String world = rs.getString("world");
                    int px = rs.getInt("plot_x");
                    int pz = rs.getInt("plot_z");
                    String ownerStr = rs.getString("owner_uuid");
                    UUID owner = UUID.fromString(ownerStr);

                    Plot plot = new Plot(new PlotId(px, pz), world, owner);
                    plot.setMergedN(rs.getInt("merged_n") == 1);
                    plot.setMergedS(rs.getInt("merged_s") == 1);
                    plot.setMergedE(rs.getInt("merged_e") == 1);
                    plot.setMergedW(rs.getInt("merged_w") == 1);

                    api.addPlotToCache(plot);
                }
            }

            // 2. Load Flags
            try (ResultSet rs = stmt.executeQuery(selectFlags)) {
                while (rs.next()) {
                    String world = rs.getString("world");
                    int px = rs.getInt("plot_x");
                    int pz = rs.getInt("plot_z");
                    String key = rs.getString("flag_key");
                    String val = rs.getString("flag_value");

                    Plot plot = api.getPlot(world, new PlotId(px, pz));
                    if (plot != null) {
                        plot.setFlag(key, val);
                    }
                }
            }

            // 3. Load Members
            try (ResultSet rs = stmt.executeQuery(selectMembers)) {
                while (rs.next()) {
                    String world = rs.getString("world");
                    int px = rs.getInt("plot_x");
                    int pz = rs.getInt("plot_z");
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));

                    Plot plot = api.getPlot(world, new PlotId(px, pz));
                    if (plot != null) {
                        plot.getAdded().add(uuid);
                    }
                }
            }

            // 4. Load Trusted
            try (ResultSet rs = stmt.executeQuery(selectTrusted)) {
                while (rs.next()) {
                    String world = rs.getString("world");
                    int px = rs.getInt("plot_x");
                    int pz = rs.getInt("plot_z");
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));

                    Plot plot = api.getPlot(world, new PlotId(px, pz));
                    if (plot != null) {
                        plot.getTrusted().add(uuid);
                    }
                }
            }

            // 5. Load Denied
            try (ResultSet rs = stmt.executeQuery(selectDenied)) {
                while (rs.next()) {
                    String world = rs.getString("world");
                    int px = rs.getInt("plot_x");
                    int pz = rs.getInt("plot_z");
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));

                    Plot plot = api.getPlot(world, new PlotId(px, pz));
                    if (plot != null) {
                        plot.getDenied().add(uuid);
                    }
                }
            }

            // 6. Load Ratings
            String selectRatings = "SELECT * FROM plot_ratings";
            try (ResultSet rs = stmt.executeQuery(selectRatings)) {
                while (rs.next()) {
                    String world = rs.getString("world");
                    int px = rs.getInt("plot_x");
                    int pz = rs.getInt("plot_z");
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    int rating = rs.getInt("rating");

                    Plot plot = api.getPlot(world, new PlotId(px, pz));
                    if (plot != null) {
                        plot.getRatings().put(uuid, rating);
                    }
                }
            }

            plugin.getLogger().info("Successfully loaded plots from database.");

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load plots: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Saves a plot owner change to database asynchronously.
     */
    public CompletableFuture<Void> savePlotOwner(Plot plot) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO plots (world, plot_x, plot_z, owner_uuid, merged_n, merged_s, merged_e, merged_w) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            if (storageType.equals("mysql")) {
                sql = "INSERT INTO plots (world, plot_x, plot_z, owner_uuid, merged_n, merged_s, merged_e, merged_w) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE owner_uuid = ?, merged_n = ?, merged_s = ?, merged_e = ?, merged_w = ?";
            }

            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, plot.getWorld());
                pstmt.setInt(2, plot.getId().getX());
                pstmt.setInt(3, plot.getId().getZ());
                pstmt.setString(4, plot.getOwner().toString());
                pstmt.setInt(5, plot.isMergedN() ? 1 : 0);
                pstmt.setInt(6, plot.isMergedS() ? 1 : 0);
                pstmt.setInt(7, plot.isMergedE() ? 1 : 0);
                pstmt.setInt(8, plot.isMergedW() ? 1 : 0);

                if (storageType.equals("mysql")) {
                    pstmt.setString(9, plot.getOwner().toString());
                    pstmt.setInt(10, plot.isMergedN() ? 1 : 0);
                    pstmt.setInt(11, plot.isMergedS() ? 1 : 0);
                    pstmt.setInt(12, plot.isMergedE() ? 1 : 0);
                    pstmt.setInt(13, plot.isMergedW() ? 1 : 0);
                }

                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save plot owner: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Deletes a plot from the database asynchronously.
     */
    public CompletableFuture<Void> deletePlot(Plot plot) {
        return CompletableFuture.runAsync(() -> {
            String deletePlot = "DELETE FROM plots WHERE world = ? AND plot_x = ? AND plot_z = ?";
            String deleteFlags = "DELETE FROM plot_flags WHERE world = ? AND plot_x = ? AND plot_z = ?";
            String deleteMembers = "DELETE FROM plot_members WHERE world = ? AND plot_x = ? AND plot_z = ?";
            String deleteTrusted = "DELETE FROM plot_trusted WHERE world = ? AND plot_x = ? AND plot_z = ?";
            String deleteDenied = "DELETE FROM plot_denied WHERE world = ? AND plot_x = ? AND plot_z = ?";
            String deleteRatings = "DELETE FROM plot_ratings WHERE world = ? AND plot_x = ? AND plot_z = ?";
            String deleteComments = "DELETE FROM plot_comments WHERE world = ? AND plot_x = ? AND plot_z = ?";

            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement pPlot = conn.prepareStatement(deletePlot);
                     PreparedStatement pFlags = conn.prepareStatement(deleteFlags);
                     PreparedStatement pMem = conn.prepareStatement(deleteMembers);
                     PreparedStatement pTrust = conn.prepareStatement(deleteTrusted);
                     PreparedStatement pDeny = conn.prepareStatement(deleteDenied);
                     PreparedStatement pRatings = conn.prepareStatement(deleteRatings);
                     PreparedStatement pComments = conn.prepareStatement(deleteComments)) {

                    for (PreparedStatement pstmt : new PreparedStatement[]{pPlot, pFlags, pMem, pTrust, pDeny, pRatings, pComments}) {
                        pstmt.setString(1, plot.getWorld());
                        pstmt.setInt(2, plot.getId().getX());
                        pstmt.setInt(3, plot.getId().getZ());
                        pstmt.executeUpdate();
                    }
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete plot: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> savePlotFlag(String world, PlotId plotId, String flag, String value) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO plot_flags (world, plot_x, plot_z, flag_key, flag_value) VALUES (?, ?, ?, ?, ?)";
            if (storageType.equals("mysql")) {
                sql = "INSERT INTO plot_flags (world, plot_x, plot_z, flag_key, flag_value) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE flag_value = ?";
            }

            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, world);
                pstmt.setInt(2, plotId.getX());
                pstmt.setInt(3, plotId.getZ());
                pstmt.setString(4, flag);
                pstmt.setString(5, value);
                if (storageType.equals("mysql")) {
                    pstmt.setString(6, value);
                }
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save flag: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> removePlotFlag(String world, PlotId plotId, String flag) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM plot_flags WHERE world = ? AND plot_x = ? AND plot_z = ? AND flag_key = ?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, world);
                pstmt.setInt(2, plotId.getX());
                pstmt.setInt(3, plotId.getZ());
                pstmt.setString(4, flag);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to delete flag: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> addPlotMember(String world, PlotId plotId, UUID member, String type) {
        return CompletableFuture.runAsync(() -> {
            String table = "plot_members";
            if (type.equals("trusted")) table = "plot_trusted";
            if (type.equals("denied")) table = "plot_denied";

            String sql = "INSERT OR IGNORE INTO " + table + " (world, plot_x, plot_z, player_uuid) VALUES (?, ?, ?, ?)";
            if (storageType.equals("mysql")) {
                sql = "INSERT IGNORE INTO " + table + " (world, plot_x, plot_z, player_uuid) VALUES (?, ?, ?, ?)";
            }

            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, world);
                pstmt.setInt(2, plotId.getX());
                pstmt.setInt(3, plotId.getZ());
                pstmt.setString(4, member.toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to add plot member (" + type + "): " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> removePlotMember(String world, PlotId plotId, UUID member, String type) {
        return CompletableFuture.runAsync(() -> {
            String table = "plot_members";
            if (type.equals("trusted")) table = "plot_trusted";
            if (type.equals("denied")) table = "plot_denied";

            String sql = "DELETE FROM " + table + " WHERE world = ? AND plot_x = ? AND plot_z = ? AND player_uuid = ?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, world);
                pstmt.setInt(2, plotId.getX());
                pstmt.setInt(3, plotId.getZ());
                pstmt.setString(4, member.toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to remove plot member (" + type + "): " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // --- Backups DB Operations ---

    public CompletableFuture<Void> saveBackup(PlotBackup backup) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO plot_backups (world, plot_x, plot_z, timestamp, player_uuid, file_path) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, backup.getWorld());
                pstmt.setInt(2, backup.getPlotId().getX());
                pstmt.setInt(3, backup.getPlotId().getZ());
                pstmt.setLong(4, backup.getTimestamp());
                pstmt.setString(5, backup.getCreator().toString());
                pstmt.setString(6, backup.getFilePath());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save backup: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<List<PlotBackup>> getBackups(String world, PlotId plotId) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlotBackup> backups = new ArrayList<>();
            String sql = "SELECT * FROM plot_backups WHERE world = ? AND plot_x = ? AND plot_z = ? ORDER BY timestamp DESC";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, world);
                pstmt.setInt(2, plotId.getX());
                pstmt.setInt(3, plotId.getZ());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        backups.add(new PlotBackup(
                                rs.getInt("id"),
                                plotId,
                                world,
                                rs.getLong("timestamp"),
                                UUID.fromString(rs.getString("player_uuid")),
                                rs.getString("file_path")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to query backups: " + e.getMessage());
                e.printStackTrace();
            }
            return backups;
        });
    }

    // --- Clusters DB Operations ---

    public CompletableFuture<Void> saveCluster(PlotCluster cluster) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR REPLACE INTO plot_clusters (name, world, min_x, min_z, max_x, max_z, owner_uuid) VALUES (?, ?, ?, ?, ?, ?, ?)";
            if (storageType.equals("mysql")) {
                sql = "INSERT INTO plot_clusters (name, world, min_x, min_z, max_x, max_z, owner_uuid) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE world = ?, min_x = ?, min_z = ?, max_x = ?, max_z = ?, owner_uuid = ?";
            }

            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, cluster.getName());
                pstmt.setString(2, cluster.getWorld());
                pstmt.setInt(3, cluster.getMinPlot().getX());
                pstmt.setInt(4, cluster.getMinPlot().getZ());
                pstmt.setInt(5, cluster.getMaxPlot().getX());
                pstmt.setInt(6, cluster.getMaxPlot().getZ());
                pstmt.setString(7, cluster.getOwner().toString());

                if (storageType.equals("mysql")) {
                    pstmt.setString(8, cluster.getWorld());
                    pstmt.setInt(9, cluster.getMinPlot().getX());
                    pstmt.setInt(10, cluster.getMinPlot().getZ());
                    pstmt.setInt(11, cluster.getMaxPlot().getX());
                    pstmt.setInt(12, cluster.getMaxPlot().getZ());
                    pstmt.setString(13, cluster.getOwner().toString());
                }

                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save cluster: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<List<PlotCluster>> loadAllClusters() {
        return CompletableFuture.supplyAsync(() -> {
            List<PlotCluster> clusters = new ArrayList<>();
            String sql = "SELECT * FROM plot_clusters";
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    clusters.add(new PlotCluster(
                            rs.getString("name"),
                            rs.getString("world"),
                            new PlotId(rs.getInt("min_x"), rs.getInt("min_z")),
                            new PlotId(rs.getInt("max_x"), rs.getInt("max_z")),
                            UUID.fromString(rs.getString("owner_uuid"))
                    ));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load clusters: " + e.getMessage());
                e.printStackTrace();
            }
            return clusters;
        });
    }

    /**
     * Converts data between SQLite and MySQL.
     */
    public CompletableFuture<Boolean> convertDatabase(String targetType) {
        final String finalTargetType = targetType.toLowerCase();
        return CompletableFuture.supplyAsync(() -> {
            // Read all current tables into memory first, then write them to target
            if (finalTargetType.equals(storageType)) {
                return false;
            }

            plugin.getLogger().info("Starting database conversion to " + finalTargetType + "...");

            // Load all current records
            List<Plot> plots = new ArrayList<>();
            List<Map<String, String>> flags = new ArrayList<>();
            List<Map<String, String>> members = new ArrayList<>();
            List<Map<String, String>> trusted = new ArrayList<>();
            List<Map<String, String>> denied = new ArrayList<>();
            List<PlotBackup> backups = new ArrayList<>();
            List<PlotCluster> clusters = new ArrayList<>();

            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                // Fetch all data
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM plots")) {
                    while (rs.next()) {
                        Plot p = new Plot(new PlotId(rs.getInt("plot_x"), rs.getInt("plot_z")), rs.getString("world"), UUID.fromString(rs.getString("owner_uuid")));
                        p.setMergedN(rs.getInt("merged_n") == 1);
                        p.setMergedS(rs.getInt("merged_s") == 1);
                        p.setMergedE(rs.getInt("merged_e") == 1);
                        p.setMergedW(rs.getInt("merged_w") == 1);
                        plots.add(p);
                    }
                }
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM plot_flags")) {
                    while (rs.next()) {
                        Map<String, String> row = new HashMap<>();
                        row.put("world", rs.getString("world"));
                        row.put("plot_x", String.valueOf(rs.getInt("plot_x")));
                        row.put("plot_z", String.valueOf(rs.getInt("plot_z")));
                        row.put("key", rs.getString("flag_key"));
                        row.put("val", rs.getString("flag_value"));
                        flags.add(row);
                    }
                }
                for (String tbl : new String[]{"plot_members", "plot_trusted", "plot_denied"}) {
                    try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + tbl)) {
                        while (rs.next()) {
                            Map<String, String> row = new HashMap<>();
                            row.put("world", rs.getString("world"));
                            row.put("plot_x", String.valueOf(rs.getInt("plot_x")));
                            row.put("plot_z", String.valueOf(rs.getInt("plot_z")));
                            row.put("uuid", rs.getString("player_uuid"));
                            if (tbl.equals("plot_members")) members.add(row);
                            if (tbl.equals("plot_trusted")) trusted.add(row);
                            if (tbl.equals("plot_denied")) denied.add(row);
                        }
                    }
                }
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM plot_backups")) {
                    while (rs.next()) {
                        backups.add(new PlotBackup(
                                rs.getInt("id"),
                                new PlotId(rs.getInt("plot_x"), rs.getInt("plot_z")),
                                rs.getString("world"),
                                rs.getLong("timestamp"),
                                UUID.fromString(rs.getString("player_uuid")),
                                rs.getString("file_path")
                        ));
                    }
                }
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM plot_clusters")) {
                    while (rs.next()) {
                        clusters.add(new PlotCluster(
                                rs.getString("name"),
                                rs.getString("world"),
                                new PlotId(rs.getInt("min_x"), rs.getInt("min_z")),
                                new PlotId(rs.getInt("max_x"), rs.getInt("max_z")),
                                UUID.fromString(rs.getString("owner_uuid"))
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to read data for conversion: " + e.getMessage());
                return false;
            }

            // Close existing source datasource
            close();

            // Re-initialize Hikari config temporarily with the target settings
            FileConfiguration storageConfig = plugin.getStorageFile();
            storageConfig.set("storage-type", finalTargetType);
            try {
                plugin.getStorageFile().save(new File(plugin.getDataFolder(), "storage.yml"));
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save storage.yml: " + e.getMessage());
            }

            init(storageConfig);

            // Now write everything into the target database
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // Plots
                    String insPlot = "INSERT INTO plots (world, plot_x, plot_z, owner_uuid, merged_n, merged_s, merged_e, merged_w) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insPlot)) {
                        for (Plot p : plots) {
                            ps.setString(1, p.getWorld());
                            ps.setInt(2, p.getId().getX());
                            ps.setInt(3, p.getId().getZ());
                            ps.setString(4, p.getOwner().toString());
                            ps.setInt(5, p.isMergedN() ? 1 : 0);
                            ps.setInt(6, p.isMergedS() ? 1 : 0);
                            ps.setInt(7, p.isMergedE() ? 1 : 0);
                            ps.setInt(8, p.isMergedW() ? 1 : 0);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }

                    // Flags
                    String insFlag = "INSERT INTO plot_flags (world, plot_x, plot_z, flag_key, flag_value) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insFlag)) {
                        for (Map<String, String> f : flags) {
                            ps.setString(1, f.get("world"));
                            ps.setInt(2, Integer.parseInt(f.get("plot_x")));
                            ps.setInt(3, Integer.parseInt(f.get("plot_z")));
                            ps.setString(4, f.get("key"));
                            ps.setString(5, f.get("val"));
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }

                    // Members, Trusted, Denied
                    for (String tbl : new String[]{"plot_members", "plot_trusted", "plot_denied"}) {
                        List<Map<String, String>> list = tbl.equals("plot_members") ? members : tbl.equals("plot_trusted") ? trusted : denied;
                        String ins = "INSERT INTO " + tbl + " (world, plot_x, plot_z, player_uuid) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement ps = conn.prepareStatement(ins)) {
                            for (Map<String, String> m : list) {
                                ps.setString(1, m.get("world"));
                                ps.setInt(2, Integer.parseInt(m.get("plot_x")));
                                ps.setInt(3, Integer.parseInt(m.get("plot_z")));
                                ps.setString(4, m.get("uuid"));
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                    }

                    // Backups
                    String insBackup = "INSERT INTO plot_backups (world, plot_x, plot_z, timestamp, player_uuid, file_path) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insBackup)) {
                        for (PlotBackup b : backups) {
                            ps.setString(1, b.getWorld());
                            ps.setInt(2, b.getPlotId().getX());
                            ps.setInt(3, b.getPlotId().getZ());
                            ps.setLong(4, b.getTimestamp());
                            ps.setString(5, b.getCreator().toString());
                            ps.setString(6, b.getFilePath());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }

                    // Clusters
                    String insCluster = "INSERT INTO plot_clusters (name, world, min_x, min_z, max_x, max_z, owner_uuid) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insCluster)) {
                        for (PlotCluster c : clusters) {
                            ps.setString(1, c.getName());
                            ps.setString(2, c.getWorld());
                            ps.setInt(3, c.getMinPlot().getX());
                            ps.setInt(4, c.getMinPlot().getZ());
                            ps.setInt(5, c.getMaxPlot().getX());
                            ps.setInt(6, c.getMaxPlot().getZ());
                            ps.setString(7, c.getOwner().toString());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }

                    conn.commit();
                    plugin.getLogger().info("Database conversion completed successfully!");
                    return true;
                } catch (Exception e) {
                    conn.rollback();
                    plugin.getLogger().severe("Database conversion failed during write, rolled back: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Database write error during conversion: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<Void> updatePlayerLastSeen(UUID playerUuid, long timestamp) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO player_activity (player_uuid, last_seen) VALUES (?, ?) " +
                         "ON DUPLICATE KEY UPDATE last_seen = VALUES(last_seen)";
            if (storageType.equals("sqlite")) {
                sql = "INSERT INTO player_activity (player_uuid, last_seen) VALUES (?, ?) " +
                      "ON CONFLICT(player_uuid) DO UPDATE SET last_seen = excluded.last_seen";
            }
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerUuid.toString());
                pstmt.setLong(2, timestamp);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to update player activity in db: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Long> getPlayerLastSeen(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT last_seen FROM player_activity WHERE player_uuid = ?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerUuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("last_seen");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to retrieve player activity from db: " + e.getMessage());
            }
            return 0L;
        });
    }

    public CompletableFuture<Void> savePlotRating(String world, PlotId plotId, UUID rater, int rating) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO plot_ratings (world, plot_x, plot_z, player_uuid, rating) VALUES (?, ?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE rating = VALUES(rating)";
            if (storageType.equals("sqlite")) {
                sql = "INSERT INTO plot_ratings (world, plot_x, plot_z, player_uuid, rating) VALUES (?, ?, ?, ?, ?) " +
                      "ON CONFLICT(world, plot_x, plot_z, player_uuid) DO UPDATE SET rating = excluded.rating";
            }
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, world);
                pstmt.setInt(2, plotId.getX());
                pstmt.setInt(3, plotId.getZ());
                pstmt.setString(4, rater.toString());
                pstmt.setInt(5, rating);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save plot rating: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> addPlotComment(String world, PlotId plotId, UUID commenter, String name, String text) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO plot_comments (world, plot_x, plot_z, commenter_uuid, commenter_name, comment_text, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, world);
                pstmt.setInt(2, plotId.getX());
                pstmt.setInt(3, plotId.getZ());
                pstmt.setString(4, commenter.toString());
                pstmt.setString(5, name);
                pstmt.setString(6, text);
                pstmt.setLong(7, System.currentTimeMillis());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to add plot comment: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<List<com.simpleplots.api.PlotComment>> getPlotComments(String world, PlotId plotId) {
        return CompletableFuture.supplyAsync(() -> {
            List<com.simpleplots.api.PlotComment> comments = new ArrayList<>();
            String sql = "SELECT commenter_uuid, commenter_name, comment_text, timestamp FROM plot_comments WHERE world = ? AND plot_x = ? AND plot_z = ? ORDER BY timestamp DESC";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, world);
                pstmt.setInt(2, plotId.getX());
                pstmt.setInt(3, plotId.getZ());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        UUID commenterUuid = UUID.fromString(rs.getString("commenter_uuid"));
                        String name = rs.getString("commenter_name");
                        String text = rs.getString("comment_text");
                        long ts = rs.getLong("timestamp");
                        comments.add(new com.simpleplots.api.PlotComment(commenterUuid, name, text, ts));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to retrieve plot comments: " + e.getMessage());
            }
            return comments;
        });
    }

    public CompletableFuture<Void> clearPlotComments(String world, PlotId plotId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM plot_comments WHERE world = ? AND plot_x = ? AND plot_z = ?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, world);
                pstmt.setInt(2, plotId.getX());
                pstmt.setInt(3, plotId.getZ());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to clear plot comments: " + e.getMessage());
            }
        });
    }
}

package com.staffsystem.plugin.managers;

import com.staffsystem.plugin.StaffSystemPlugin;
import com.staffsystem.plugin.models.Punishment;
import com.staffsystem.plugin.models.Punishment.PunishmentType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final StaffSystemPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(StaffSystemPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        HikariConfig config = new HikariConfig();

        // SQLite only - no setup required!
        File dbFile = new File(plugin.getDataFolder(), 
            plugin.getConfig().getString("database.file", "database.db"));
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);

        config.setPoolName("StaffSystem-Pool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            dataSource = new HikariDataSource(config);
            createTables();
            plugin.getLogger().info("SQLite database initialized successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() {
        String punishmentsTable = 
            "CREATE TABLE IF NOT EXISTS punishments (" +
            "    id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    player_uuid TEXT NOT NULL," +
            "    player_name TEXT NOT NULL," +
            "    staff_uuid TEXT NOT NULL," +
            "    staff_name TEXT NOT NULL," +
            "    type TEXT NOT NULL," +
            "    reason TEXT," +
            "    timestamp INTEGER NOT NULL," +
            "    duration INTEGER NOT NULL," +
            "    expiration INTEGER NOT NULL," +
            "    active INTEGER DEFAULT 1," +
            "    server TEXT DEFAULT 'main'" +
            ")";

        String staffTable = 
            "CREATE TABLE IF NOT EXISTS staff (" +
            "    uuid TEXT PRIMARY KEY," +
            "    name TEXT NOT NULL," +
            "    staff_rank TEXT DEFAULT 'Staff'," +
            "    tier INTEGER DEFAULT 1," +
            "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "    last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";

        // Create indexes for better query performance
        String indexPlayerUuid = "CREATE INDEX IF NOT EXISTS idx_player_uuid ON punishments(player_uuid);";
        String indexActive = "CREATE INDEX IF NOT EXISTS idx_active ON punishments(active);";
        String indexType = "CREATE INDEX IF NOT EXISTS idx_type ON punishments(type);";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(punishmentsTable);
            stmt.execute(staffTable);
            stmt.execute(indexPlayerUuid);
            stmt.execute(indexActive);
            stmt.execute(indexType);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public CompletableFuture<Punishment> savePunishment(Punishment punishment) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = 
                "INSERT INTO punishments (player_uuid, player_name, staff_uuid, staff_name, " +
                "    type, reason, timestamp, duration, expiration, active, server) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, punishment.getPlayerUuid().toString());
                stmt.setString(2, punishment.getPlayerName());
                stmt.setString(3, punishment.getStaffUuid().toString());
                stmt.setString(4, punishment.getStaffName());
                stmt.setString(5, punishment.getType().name());
                stmt.setString(6, punishment.getReason());
                stmt.setLong(7, punishment.getTimestamp());
                stmt.setLong(8, punishment.getDuration());
                stmt.setLong(9, punishment.getExpiration());
                stmt.setBoolean(10, punishment.isActive());
                stmt.setString(11, punishment.getServer());

                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    punishment.setId(rs.getInt(1));
                }
                return punishment;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save punishment: " + e.getMessage());
                return null;
            }
        });
    }

    public CompletableFuture<Punishment> getActiveBan(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = 
                "SELECT * FROM punishments " +
                "WHERE player_uuid = ? AND type IN ('BAN', 'TEMP_BAN') AND active = ? " +
                "ORDER BY timestamp DESC LIMIT 1";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setBoolean(2, true);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    Punishment punishment = parsePunishment(rs);
                    if (punishment.isPermanent() || !punishment.isExpired()) {
                        return punishment;
                    }
                    // Deactivate expired punishment
                    deactivatePunishment(punishment.getId());
                }
                return null;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get active ban: " + e.getMessage());
                return null;
            }
        });
    }

    public CompletableFuture<Punishment> getActiveMute(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = 
                "SELECT * FROM punishments " +
                "WHERE player_uuid = ? AND type IN ('MUTE', 'TEMP_MUTE') AND active = ? " +
                "ORDER BY timestamp DESC LIMIT 1";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setBoolean(2, true);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    Punishment punishment = parsePunishment(rs);
                    if (punishment.isPermanent() || !punishment.isExpired()) {
                        return punishment;
                    }
                    // Deactivate expired punishment
                    deactivatePunishment(punishment.getId());
                }
                return null;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get active mute: " + e.getMessage());
                return null;
            }
        });
    }

    public CompletableFuture<List<Punishment>> getPunishmentHistory(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            String sql = "SELECT * FROM punishments WHERE player_uuid = ? ORDER BY timestamp DESC";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());

                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    punishments.add(parsePunishment(rs));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get punishment history: " + e.getMessage());
            }
            return punishments;
        });
    }

    public CompletableFuture<Integer> getWarningCount(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM punishments WHERE player_uuid = ? AND type = 'WARN' AND active = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setBoolean(2, true);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get warning count: " + e.getMessage());
            }
            return 0;
        });
    }

    public CompletableFuture<Boolean> deactivatePunishment(int punishmentId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE punishments SET active = ? WHERE id = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBoolean(1, false);
                stmt.setInt(2, punishmentId);

                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to deactivate punishment: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> unban(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE punishments SET active = ? WHERE player_uuid = ? AND type IN ('BAN', 'TEMP_BAN') AND active = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBoolean(1, false);
                stmt.setString(2, playerUuid.toString());
                stmt.setBoolean(3, true);

                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to unban player: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> unmute(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE punishments SET active = ? WHERE player_uuid = ? AND type IN ('MUTE', 'TEMP_MUTE') AND active = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBoolean(1, false);
                stmt.setString(2, playerUuid.toString());
                stmt.setBoolean(3, true);

                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to unmute player: " + e.getMessage());
                return false;
            }
        });
    }

    private Punishment parsePunishment(ResultSet rs) throws SQLException {
        Punishment punishment = new Punishment();
        punishment.setId(rs.getInt("id"));
        punishment.setPlayerUuid(UUID.fromString(rs.getString("player_uuid")));
        punishment.setPlayerName(rs.getString("player_name"));
        punishment.setStaffUuid(UUID.fromString(rs.getString("staff_uuid")));
        punishment.setStaffName(rs.getString("staff_name"));
        punishment.setType(PunishmentType.valueOf(rs.getString("type")));
        punishment.setReason(rs.getString("reason"));
        punishment.setTimestamp(rs.getLong("timestamp"));
        punishment.setDuration(rs.getLong("duration"));
        punishment.setExpiration(rs.getLong("expiration"));
        punishment.setActive(rs.getBoolean("active"));
        punishment.setServer(rs.getString("server"));
        return punishment;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}

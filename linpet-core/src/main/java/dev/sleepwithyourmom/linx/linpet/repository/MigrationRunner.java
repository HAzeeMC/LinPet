package dev.sleepwithyourmom.linx.linpet.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Applies bundled SQL migrations in order and records applied schema versions.
 */
public class MigrationRunner {
    private static final List<Migration> MIGRATIONS = List.of(
        new Migration(1, "migrations/V1__init.sql")
    );

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;

    /**
     * Creates a migration runner.
     *
     * @param plugin owning plugin
     * @param databaseManager database manager
     */
    public MigrationRunner(JavaPlugin plugin, DatabaseManager databaseManager) {
        if (plugin == null || databaseManager == null) {
            throw new IllegalArgumentException("plugin and databaseManager must not be null");
        }
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Applies every migration newer than the recorded schema version.
     */
    public void migrate() {
        try (Connection connection = databaseManager.connection()) {
            connection.setAutoCommit(false);
            createVersionTable(connection);
            int currentVersion = currentVersion(connection);
            for (Migration migration : MIGRATIONS) {
                if (migration.version() <= currentVersion) {
                    continue;
                }
                applyMigration(connection, migration);
                recordVersion(connection, migration.version());
                plugin.getLogger().info("Applied LinPet database migration V" + migration.version());
            }
            connection.commit();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to apply database migrations", ex);
        }
    }

    private void createVersionTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INTEGER PRIMARY KEY,
                    applied_at TEXT NOT NULL
                )
                """);
        }
    }

    private int currentVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT MAX(version) FROM schema_version")) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        }
    }

    private void applyMigration(Connection connection, Migration migration) throws SQLException {
        String sql = readResource(migration.resourcePath());
        for (String statementSql : sql.split(";\\s*(?:\\r?\\n|$)")) {
            String trimmed = statementSql.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(trimmed);
            }
        }
    }

    private void recordVersion(Connection connection, int version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO schema_version(version, applied_at) VALUES (?, ?)")) {
            statement.setInt(1, version);
            statement.setString(2, Instant.now().toString());
            statement.executeUpdate();
        }
    }

    private String readResource(String path) {
        try (InputStream inputStream = plugin.getResource(path)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing migration resource " + path);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
                return builder.toString();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed reading migration resource " + path, ex);
        }
    }

    private record Migration(int version, String resourcePath) {
    }
}

package dev.sleepwithyourmom.linx.linpet.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.sleepwithyourmom.linx.linpet.config.DatabaseSettings;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Owns the HikariCP data source used by Lin'Pet repositories.
 */
public class DatabaseManager implements AutoCloseable {
    private final JavaPlugin plugin;
    private final DatabaseSettings settings;
    private HikariDataSource dataSource;

    /**
     * Creates a database manager.
     *
     * @param plugin owning plugin
     * @param settings database settings
     */
    public DatabaseManager(JavaPlugin plugin, DatabaseSettings settings) {
        if (plugin == null || settings == null) {
            throw new IllegalArgumentException("plugin and settings must not be null");
        }
        this.plugin = plugin;
        this.settings = settings;
    }

    /**
     * Opens the connection pool.
     */
    public void start() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("LinPetPool");
        config.setMaximumPoolSize(settings.sqlite() ? 1 : settings.poolSize());
        config.setMinimumIdle(settings.sqlite() ? 1 : Math.min(2, settings.poolSize()));
        config.setConnectionTimeout(10_000L);
        config.setValidationTimeout(3_000L);
        if (settings.sqlite()) {
            File databaseFile = new File(plugin.getDataFolder(), settings.name() + ".db");
            config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            config.addDataSourceProperty("foreign_keys", "true");
        } else {
            config.setJdbcUrl("jdbc:mysql://" + settings.host() + ":" + settings.port() + "/" + settings.name()
                + "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC");
            config.setUsername(settings.user());
            config.setPassword(settings.password());
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        }
        dataSource = new HikariDataSource(config);
    }

    /**
     * Returns whether the active dialect is SQLite.
     *
     * @return true for SQLite
     */
    public boolean sqlite() {
        return settings.sqlite();
    }

    /**
     * Borrows a connection from the pool.
     *
     * @return JDBC connection
     * @throws SQLException when the pool cannot provide a connection
     */
    public Connection connection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("database manager has not been started");
        }
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}

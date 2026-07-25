package dev.sleepwithyourmom.linx.linpet.config;

/**
 * Database connection settings loaded from {@code config.yml}.
 *
 * @param type database type, {@code sqlite} or {@code mysql}
 * @param host MySQL host
 * @param port MySQL port
 * @param name database name or SQLite file base name
 * @param user database user
 * @param password database password
 * @param poolSize Hikari maximum pool size
 */
public record DatabaseSettings(
    String type,
    String host,
    int port,
    String name,
    String user,
    String password,
    int poolSize
) {
    /**
     * Creates validated database settings.
     */
    public DatabaseSettings {
        if (!"sqlite".equalsIgnoreCase(type) && !"mysql".equalsIgnoreCase(type)) {
            throw new ConfigValidationException("settings.database.type must be sqlite or mysql");
        }
        if (host == null || host.isBlank()) {
            host = "localhost";
        }
        if (port <= 0 || port > 65535) {
            throw new ConfigValidationException("settings.database.port must be between 1 and 65535");
        }
        if (name == null || name.isBlank()) {
            throw new ConfigValidationException("settings.database.name must not be blank");
        }
        if (user == null) {
            user = "";
        }
        if (password == null) {
            password = "";
        }
        if (poolSize < 1) {
            throw new ConfigValidationException("settings.database.pool-size must be at least 1");
        }
    }

    /**
     * Returns true when this configuration uses SQLite.
     *
     * @return true for SQLite
     */
    public boolean sqlite() {
        return "sqlite".equalsIgnoreCase(type);
    }
}

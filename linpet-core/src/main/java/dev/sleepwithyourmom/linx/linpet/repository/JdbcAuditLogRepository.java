package dev.sleepwithyourmom.linx.linpet.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

/**
 * JDBC append-only audit repository.
 */
public class JdbcAuditLogRepository implements AuditLogRepository {
    private final DatabaseManager databaseManager;

    /**
     * Creates an audit repository.
     *
     * @param databaseManager database manager
     */
    public JdbcAuditLogRepository(DatabaseManager databaseManager) {
        if (databaseManager == null) {
            throw new IllegalArgumentException("databaseManager must not be null");
        }
        this.databaseManager = databaseManager;
    }

    @Override
    public void record(UUID actor, String action, String target, String detail) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action must not be blank");
        }
        if (target == null) {
            target = "";
        }
        if (detail == null) {
            detail = "";
        }
        String sql = "INSERT INTO audit_log(actor_id, action, target, detail, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = databaseManager.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, actor == null ? "" : actor.toString());
            statement.setString(2, action);
            statement.setString(3, target);
            statement.setString(4, detail);
            statement.setString(5, Instant.now().toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RepositoryException("Failed recording audit action " + action, ex);
        }
    }
}

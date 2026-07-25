package dev.sleepwithyourmom.linx.linpet.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JDBC implementation of daily shop purchase counters.
 */
public class JdbcShopPurchaseRepository implements ShopPurchaseRepository {
    private final DatabaseManager databaseManager;

    /**
     * Creates a shop purchase repository.
     *
     * @param databaseManager database manager
     */
    public JdbcShopPurchaseRepository(DatabaseManager databaseManager) {
        if (databaseManager == null) {
            throw new IllegalArgumentException("databaseManager must not be null");
        }
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean reserve(UUID playerId, String petTemplateId, LocalDate day, int dailyLimit) {
        try (Connection connection = databaseManager.connection()) {
            connection.setAutoCommit(false);
            try {
                int current = currentAmount(connection, playerId, petTemplateId, day);
                if (dailyLimit > 0 && current >= dailyLimit) {
                    connection.rollback();
                    return false;
                }
                upsert(connection, playerId, petTemplateId, day, current + 1);
                connection.commit();
                return true;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed reserving shop purchase for " + playerId + " pet " + petTemplateId, ex);
        }
    }

    @Override
    public void release(UUID playerId, String petTemplateId, LocalDate day) {
        try (Connection connection = databaseManager.connection()) {
            connection.setAutoCommit(false);
            try {
                int current = currentAmount(connection, playerId, petTemplateId, day);
                if (current <= 1) {
                    try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM shop_purchases WHERE player_id = ? AND pet_template_id = ? AND purchase_day = ?")) {
                        bindKey(delete, playerId, petTemplateId, day);
                        delete.executeUpdate();
                    }
                } else {
                    upsert(connection, playerId, petTemplateId, day, current - 1);
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed releasing shop purchase for " + playerId + " pet " + petTemplateId, ex);
        }
    }

    private int currentAmount(Connection connection, UUID playerId, String petTemplateId, LocalDate day) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT amount FROM shop_purchases WHERE player_id = ? AND pet_template_id = ? AND purchase_day = ?")) {
            bindKey(statement, playerId, petTemplateId, day);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("amount");
                }
                return 0;
            }
        }
    }

    private void upsert(Connection connection, UUID playerId, String petTemplateId, LocalDate day, int amount) throws SQLException {
        String sql = databaseManager.sqlite()
            ? """
                INSERT INTO shop_purchases(player_id, pet_template_id, purchase_day, amount)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(player_id, pet_template_id, purchase_day) DO UPDATE SET amount=excluded.amount
                """
            : """
                INSERT INTO shop_purchases(player_id, pet_template_id, purchase_day, amount)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE amount=VALUES(amount)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindKey(statement, playerId, petTemplateId, day);
            statement.setInt(4, amount);
            statement.executeUpdate();
        }
    }

    private void bindKey(PreparedStatement statement, UUID playerId, String petTemplateId, LocalDate day) throws SQLException {
        statement.setString(1, playerId.toString());
        statement.setString(2, petTemplateId);
        statement.setString(3, day.toString());
    }
}

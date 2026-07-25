package dev.sleepwithyourmom.linx.linpet.repository;

import dev.sleepwithyourmom.linx.linpet.domain.auction.AuctionListing;
import dev.sleepwithyourmom.linx.linpet.domain.auction.AuctionStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC implementation of auction persistence with version guarded bid updates.
 */
public class JdbcAuctionRepository implements AuctionRepository {
    private final DatabaseManager databaseManager;

    /**
     * Creates a JDBC auction repository.
     *
     * @param databaseManager database manager
     */
    public JdbcAuctionRepository(DatabaseManager databaseManager) {
        if (databaseManager == null) {
            throw new IllegalArgumentException("databaseManager must not be null");
        }
        this.databaseManager = databaseManager;
    }

    @Override
    public void save(AuctionListing listing) {
        String sql = databaseManager.sqlite()
            ? """
                INSERT INTO auctions(auction_id, seller_id, pet_instance_id, pet_template_id, current_bid,
                    highest_bidder, ends_at, version, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(auction_id) DO UPDATE SET
                    seller_id=excluded.seller_id,
                    pet_instance_id=excluded.pet_instance_id,
                    pet_template_id=excluded.pet_template_id,
                    current_bid=excluded.current_bid,
                    highest_bidder=excluded.highest_bidder,
                    ends_at=excluded.ends_at,
                    version=excluded.version,
                    status=excluded.status
                """
            : """
                INSERT INTO auctions(auction_id, seller_id, pet_instance_id, pet_template_id, current_bid,
                    highest_bidder, ends_at, version, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    seller_id=VALUES(seller_id),
                    pet_instance_id=VALUES(pet_instance_id),
                    pet_template_id=VALUES(pet_template_id),
                    current_bid=VALUES(current_bid),
                    highest_bidder=VALUES(highest_bidder),
                    ends_at=VALUES(ends_at),
                    version=VALUES(version),
                    status=VALUES(status)
                """;
        try (Connection connection = databaseManager.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, listing);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new RepositoryException("Failed saving auction " + listing.auctionId(), ex);
        }
    }

    @Override
    public Optional<AuctionListing> find(UUID auctionId) {
        try (Connection connection = databaseManager.connection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM auctions WHERE auction_id = ?")) {
            statement.setString(1, auctionId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(read(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed loading auction " + auctionId, ex);
        }
    }

    @Override
    public List<AuctionListing> activeListings() {
        try (Connection connection = databaseManager.connection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT * FROM auctions WHERE status = 'ACTIVE' AND ends_at > ? ORDER BY ends_at ASC")) {
            statement.setLong(1, Instant.now().toEpochMilli());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AuctionListing> listings = new ArrayList<>();
                while (resultSet.next()) {
                    listings.add(read(resultSet));
                }
                return List.copyOf(listings);
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed loading active auctions", ex);
        }
    }

    @Override
    public List<AuctionListing> expiredActiveListings(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        try (Connection connection = databaseManager.connection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT * FROM auctions WHERE status = 'ACTIVE' AND ends_at <= ? ORDER BY ends_at ASC")) {
            statement.setLong(1, now.toEpochMilli());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AuctionListing> listings = new ArrayList<>();
                while (resultSet.next()) {
                    listings.add(read(resultSet));
                }
                return List.copyOf(listings);
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed loading expired active auctions", ex);
        }
    }

    @Override
    public List<AuctionListing> pendingSettlementListings() {
        try (Connection connection = databaseManager.connection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT * FROM auctions WHERE status = 'ENDED' ORDER BY ends_at ASC")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AuctionListing> listings = new ArrayList<>();
                while (resultSet.next()) {
                    listings.add(read(resultSet));
                }
                return List.copyOf(listings);
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed loading auctions pending settlement", ex);
        }
    }

    @Override
    public boolean updateBid(AuctionListing listing, long expectedVersion) {
        String sql = """
            UPDATE auctions
            SET current_bid = ?, highest_bidder = ?, version = ?, status = ?
            WHERE auction_id = ? AND version = ? AND status = 'ACTIVE'
            """;
        try (Connection connection = databaseManager.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, listing.currentBid().toPlainString());
            statement.setString(2, listing.highestBidder() == null ? "" : listing.highestBidder().toString());
            statement.setLong(3, listing.version());
            statement.setString(4, listing.status().name());
            statement.setString(5, listing.auctionId().toString());
            statement.setLong(6, expectedVersion);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new RepositoryException("Failed updating bid for auction " + listing.auctionId(), ex);
        }
    }

    @Override
    public boolean updateStatus(UUID auctionId, AuctionStatus expectedStatus, AuctionStatus newStatus) {
        if (auctionId == null || expectedStatus == null || newStatus == null) {
            throw new IllegalArgumentException("status update inputs must not be null");
        }
        String sql = """
            UPDATE auctions
            SET status = ?, version = version + 1
            WHERE auction_id = ? AND status = ?
            """;
        try (Connection connection = databaseManager.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newStatus.name());
            statement.setString(2, auctionId.toString());
            statement.setString(3, expectedStatus.name());
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new RepositoryException("Failed updating auction " + auctionId + " status to " + newStatus, ex);
        }
    }

    @Override
    public boolean extend(UUID auctionId, Instant newEndsAt) {
        if (auctionId == null || newEndsAt == null) {
            throw new IllegalArgumentException("extend inputs must not be null");
        }
        String sql = """
            UPDATE auctions
            SET ends_at = ?, version = version + 1
            WHERE auction_id = ? AND status = 'ACTIVE'
            """;
        try (Connection connection = databaseManager.connection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, newEndsAt.toEpochMilli());
            statement.setString(2, auctionId.toString());
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new RepositoryException("Failed extending auction " + auctionId + " to " + newEndsAt, ex);
        }
    }

    private void bind(PreparedStatement statement, AuctionListing listing) throws SQLException {
        statement.setString(1, listing.auctionId().toString());
        statement.setString(2, listing.sellerId() == null ? "" : listing.sellerId().toString());
        statement.setString(3, listing.petInstanceId() == null ? "" : listing.petInstanceId().toString());
        statement.setString(4, listing.petTemplateId());
        statement.setString(5, listing.currentBid().toPlainString());
        statement.setString(6, listing.highestBidder() == null ? "" : listing.highestBidder().toString());
        statement.setLong(7, listing.endsAt().toEpochMilli());
        statement.setLong(8, listing.version());
        statement.setString(9, listing.status().name());
    }

    private AuctionListing read(ResultSet resultSet) throws SQLException {
        return new AuctionListing(
            UUID.fromString(resultSet.getString("auction_id")),
            readUuid(resultSet.getString("seller_id")),
            readUuid(resultSet.getString("pet_instance_id")),
            resultSet.getString("pet_template_id"),
            new BigDecimal(resultSet.getString("current_bid")),
            readUuid(resultSet.getString("highest_bidder")),
            Instant.ofEpochMilli(resultSet.getLong("ends_at")),
            resultSet.getLong("version"),
            AuctionStatus.valueOf(resultSet.getString("status"))
        );
    }

    private UUID readUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }
}

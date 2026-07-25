package dev.sleepwithyourmom.linx.linpet.domain.auction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable auction listing state.
 *
 * @param auctionId unique auction id
 * @param sellerId seller UUID, or {@code null} for system listings
 * @param petInstanceId pet instance id, or {@code null} for system-generated pet template listings
 * @param petTemplateId pet template id
 * @param currentBid current bid amount
 * @param highestBidder highest bidder UUID, or {@code null} when no bid exists
 * @param endsAt end timestamp
 * @param version optimistic-lock version
 * @param status lifecycle status
 */
public record AuctionListing(
    UUID auctionId,
    UUID sellerId,
    UUID petInstanceId,
    String petTemplateId,
    BigDecimal currentBid,
    UUID highestBidder,
    Instant endsAt,
    long version,
    AuctionStatus status
) {
    /**
     * Creates a validated auction listing.
     */
    public AuctionListing {
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId must not be null");
        }
        if (petTemplateId == null || petTemplateId.isBlank()) {
            throw new IllegalArgumentException("petTemplateId must not be blank");
        }
        if (currentBid == null || currentBid.signum() < 0) {
            throw new IllegalArgumentException("currentBid must not be negative");
        }
        if (endsAt == null) {
            throw new IllegalArgumentException("endsAt must not be null");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }

    /**
     * Returns whether this listing can accept bids at the given time.
     *
     * @param now current time
     * @return true when active and not ended
     */
    public boolean acceptsBids(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        return status == AuctionStatus.ACTIVE && now.isBefore(endsAt);
    }

    /**
     * Returns a listing updated with a successful bid.
     *
     * @param bidder bidder UUID
     * @param amount bid amount
     * @return updated listing with version incremented
     */
    public AuctionListing withBid(UUID bidder, BigDecimal amount) {
        if (bidder == null) {
            throw new IllegalArgumentException("bidder must not be null");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        return new AuctionListing(
            auctionId,
            sellerId,
            petInstanceId,
            petTemplateId,
            amount,
            bidder,
            endsAt,
            version + 1,
            status
        );
    }
}

package dev.sleepwithyourmom.linx.linpet.domain.auction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Pure auction bid validation and state transition logic.
 */
public class AuctionEngine {
    private final AuctionRules rules;

    /**
     * Creates an auction engine.
     *
     * @param rules auction rules
     */
    public AuctionEngine(AuctionRules rules) {
        if (rules == null) {
            throw new IllegalArgumentException("rules must not be null");
        }
        this.rules = rules;
    }

    /**
     * Applies a bid to a listing when it satisfies auction rules.
     *
     * @param listing current listing
     * @param bidder bidder UUID
     * @param amount bid amount
     * @param now current time
     * @return updated listing
     */
    public AuctionListing placeBid(AuctionListing listing, UUID bidder, BigDecimal amount, Instant now) {
        if (listing == null || bidder == null || amount == null || now == null) {
            throw new IllegalArgumentException("bid inputs must not be null");
        }
        if (!listing.acceptsBids(now)) {
            throw new IllegalStateException("auction is not accepting bids");
        }
        if (bidder.equals(listing.sellerId())) {
            throw new IllegalStateException("seller cannot bid on their own auction");
        }
        BigDecimal minimum = listing.currentBid().add(rules.minBidIncrement());
        if (amount.compareTo(minimum) < 0) {
            throw new IllegalArgumentException("bid must be at least " + minimum);
        }
        return listing.withBid(bidder, amount);
    }
}

package dev.sleepwithyourmom.linx.linpet.domain.auction;

import java.math.BigDecimal;

/**
 * Rules that govern auction bidding.
 *
 * @param minBidIncrement minimum increase over current bid
 * @param taxPercent seller tax percentage
 * @param retryOnVersionConflict whether service layer may retry one optimistic conflict
 */
public record AuctionRules(BigDecimal minBidIncrement, BigDecimal taxPercent, boolean retryOnVersionConflict) {
    /**
     * Creates validated auction rules.
     */
    public AuctionRules {
        if (minBidIncrement == null || minBidIncrement.signum() <= 0) {
            throw new IllegalArgumentException("minBidIncrement must be positive");
        }
        if (taxPercent == null || taxPercent.signum() < 0 || taxPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("taxPercent must be between 0 and 100");
        }
    }
}

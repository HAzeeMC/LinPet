package dev.sleepwithyourmom.linx.linpet.service;

import java.util.UUID;

/**
 * Result of an auction bid operation.
 *
 * @param success whether the bid was committed
 * @param messageKey message key
 * @param auctionId auction id
 */
public record AuctionBidResult(boolean success, String messageKey, UUID auctionId) {
    /**
     * Creates a successful bid result.
     *
     * @param auctionId auction id
     * @return result
     */
    public static AuctionBidResult success(UUID auctionId) {
        return new AuctionBidResult(true, "auction.bid-success", auctionId);
    }

    /**
     * Creates a failure result.
     *
     * @param messageKey message key
     * @param auctionId auction id
     * @return result
     */
    public static AuctionBidResult failure(String messageKey, UUID auctionId) {
        return new AuctionBidResult(false, messageKey, auctionId);
    }
}

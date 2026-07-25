package dev.sleepwithyourmom.linx.linpet.domain.auction;

/**
 * Persistent lifecycle state of an auction listing.
 */
public enum AuctionStatus {
    /**
     * Listing accepts bids.
     */
    ACTIVE,

    /**
     * Listing has ended and needs settlement.
     */
    ENDED,

    /**
     * Listing was settled successfully.
     */
    SETTLED,

    /**
     * Listing was cancelled by an administrator or seller rule.
     */
    CANCELLED
}

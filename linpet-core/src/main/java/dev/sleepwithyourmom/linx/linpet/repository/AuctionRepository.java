package dev.sleepwithyourmom.linx.linpet.repository;

import dev.sleepwithyourmom.linx.linpet.domain.auction.AuctionListing;
import dev.sleepwithyourmom.linx.linpet.domain.auction.AuctionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent repository for auction listings.
 */
public interface AuctionRepository {
    /**
     * Saves a listing.
     *
     * @param listing listing to save
     */
    void save(AuctionListing listing);

    /**
     * Loads a listing by id.
     *
     * @param auctionId auction id
     * @return listing when present
     */
    Optional<AuctionListing> find(UUID auctionId);

    /**
     * Lists active auctions.
     *
     * @return active listings
     */
    List<AuctionListing> activeListings();

    /**
     * Lists active auctions whose end time has passed.
     *
     * @param now current time
     * @return expired active listings
     */
    List<AuctionListing> expiredActiveListings(Instant now);

    /**
     * Lists auctions that were claimed for settlement but not completed.
     *
     * @return pending settlement listings
     */
    List<AuctionListing> pendingSettlementListings();

    /**
     * Attempts to persist a bid using optimistic locking.
     *
     * @param listing updated listing whose version is one greater than the expected version
     * @param expectedVersion expected old version
     * @return true when the update won the optimistic race
     */
    boolean updateBid(AuctionListing listing, long expectedVersion);

    /**
     * Changes a listing status when its current status matches the expected state.
     *
     * @param auctionId auction id
     * @param expectedStatus required current status
     * @param newStatus new status
     * @return true when the status transition was written
     */
    boolean updateStatus(UUID auctionId, AuctionStatus expectedStatus, AuctionStatus newStatus);

    /**
     * Extends an active auction to a future end timestamp.
     *
     * @param auctionId auction id
     * @param newEndsAt new end timestamp
     * @return true when the end timestamp was written
     */
    boolean extend(UUID auctionId, Instant newEndsAt);
}

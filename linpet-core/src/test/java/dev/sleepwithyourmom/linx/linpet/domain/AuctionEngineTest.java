package dev.sleepwithyourmom.linx.linpet.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.sleepwithyourmom.linx.linpet.domain.auction.AuctionEngine;
import dev.sleepwithyourmom.linx.linpet.domain.auction.AuctionListing;
import dev.sleepwithyourmom.linx.linpet.domain.auction.AuctionRules;
import dev.sleepwithyourmom.linx.linpet.domain.auction.AuctionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuctionEngineTest {
    @Test
    void acceptsBidAtConfiguredMinimumAndIncrementsVersion() {
        UUID bidder = UUID.randomUUID();
        AuctionListing updated = engine().placeBid(
            listing(UUID.randomUUID(), Instant.now().plusSeconds(60)),
            bidder,
            BigDecimal.valueOf(110),
            Instant.now()
        );

        assertEquals(bidder, updated.highestBidder());
        assertEquals(BigDecimal.valueOf(110), updated.currentBid());
        assertEquals(1L, updated.version());
    }

    @Test
    void rejectsBidBelowMinimumIncrement() {
        AuctionListing listing = listing(UUID.randomUUID(), Instant.now().plusSeconds(60));

        assertThrows(IllegalArgumentException.class, () ->
            engine().placeBid(listing, UUID.randomUUID(), BigDecimal.valueOf(109), Instant.now()));
    }

    @Test
    void rejectsSellerBiddingOnOwnAuction() {
        UUID seller = UUID.randomUUID();
        AuctionListing listing = listing(seller, Instant.now().plusSeconds(60));

        assertThrows(IllegalStateException.class, () ->
            engine().placeBid(listing, seller, BigDecimal.valueOf(110), Instant.now()));
    }

    @Test
    void rejectsExpiredAuctionBid() {
        AuctionListing listing = listing(UUID.randomUUID(), Instant.now().minusSeconds(1));

        assertThrows(IllegalStateException.class, () ->
            engine().placeBid(listing, UUID.randomUUID(), BigDecimal.valueOf(110), Instant.now()));
    }

    private AuctionEngine engine() {
        return new AuctionEngine(new AuctionRules(BigDecimal.TEN, BigDecimal.valueOf(5), true));
    }

    private AuctionListing listing(UUID sellerId, Instant endsAt) {
        return new AuctionListing(
            UUID.randomUUID(),
            sellerId,
            null,
            "golden_dragon",
            BigDecimal.valueOf(100),
            null,
            endsAt,
            0L,
            AuctionStatus.ACTIVE
        );
    }
}

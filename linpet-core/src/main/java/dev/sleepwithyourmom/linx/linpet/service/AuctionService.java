package dev.sleepwithyourmom.linx.linpet.service;

import dev.sleepwithyourmom.linx.linpet.config.LinPetConfig;
import dev.sleepwithyourmom.linx.linpet.core.SchedulerAdapter;
import dev.sleepwithyourmom.linx.linpet.domain.auction.AuctionEngine;
import dev.sleepwithyourmom.linx.linpet.domain.auction.AuctionListing;
import dev.sleepwithyourmom.linx.linpet.domain.auction.AuctionStatus;
import dev.sleepwithyourmom.linx.linpet.economy.EconomyProvider;
import dev.sleepwithyourmom.linx.linpet.economy.EconomyResult;
import dev.sleepwithyourmom.linx.linpet.repository.AuctionRepository;
import dev.sleepwithyourmom.linx.linpet.repository.AuditLogRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Application service for auction bidding and system listing creation.
 */
public class AuctionService {
    private final JavaPlugin plugin;
    private final AtomicReference<LinPetConfig> configRef;
    private final AuctionRepository auctionRepository;
    private final AuditLogRepository auditLogRepository;
    private final EconomyProvider economyProvider;
    private final PetService petService;
    private final SchedulerAdapter scheduler;

    /**
     * Creates an auction service.
     *
     * @param plugin owning plugin
     * @param configRef live config reference
     * @param auctionRepository auction repository
     * @param auditLogRepository audit repository
     * @param economyProvider economy provider
     * @param petService pet service
     * @param scheduler scheduler adapter
     */
    public AuctionService(
        JavaPlugin plugin,
        AtomicReference<LinPetConfig> configRef,
        AuctionRepository auctionRepository,
        AuditLogRepository auditLogRepository,
        EconomyProvider economyProvider,
        PetService petService,
        SchedulerAdapter scheduler
    ) {
        if (plugin == null || configRef == null || auctionRepository == null || auditLogRepository == null
            || economyProvider == null || petService == null || scheduler == null) {
            throw new IllegalArgumentException("auction service dependencies must not be null");
        }
        this.plugin = plugin;
        this.configRef = configRef;
        this.auctionRepository = auctionRepository;
        this.auditLogRepository = auditLogRepository;
        this.economyProvider = economyProvider;
        this.petService = petService;
        this.scheduler = scheduler;
    }

    /**
     * Places a bid with one retry on optimistic version conflict.
     *
     * @param bidder bidder
     * @param auctionId auction id
     * @param amount bid amount
     * @return future completed with bid result
     */
    public CompletableFuture<AuctionBidResult> bid(Player bidder, UUID auctionId, BigDecimal amount) {
        if (bidder == null || auctionId == null) {
            return CompletableFuture.completedFuture(AuctionBidResult.failure("auction.bid-failed", auctionId));
        }
        if (amount == null || amount.signum() <= 0) {
            return CompletableFuture.completedFuture(AuctionBidResult.failure("auction.invalid-amount", auctionId));
        }
        if (!economyProvider.available()) {
            return CompletableFuture.completedFuture(AuctionBidResult.failure("auction.no-economy", auctionId));
        }
        if (!economyProvider.has(bidder, amount)) {
            return CompletableFuture.completedFuture(AuctionBidResult.failure("auction.not-enough-money", auctionId));
        }
        EconomyResult withdraw = economyProvider.withdraw(bidder, amount);
        if (!withdraw.success()) {
            return CompletableFuture.completedFuture(AuctionBidResult.failure("auction.withdraw-failed", auctionId));
        }
        return runAsync(() -> commitBid(bidder.getUniqueId(), auctionId, amount))
            .handle((commit, throwable) -> {
                if (throwable != null) {
                    scheduler.runOnEntity(bidder, () -> economyProvider.deposit(bidder, amount));
                    plugin.getLogger().log(Level.WARNING, "Auction bid failed for " + auctionId, throwable);
                    return AuctionBidResult.failure("auction.bid-failed", auctionId);
                }
                scheduler.runGlobal(() -> refundPreviousBidder(commit.previous()));
                return AuctionBidResult.success(auctionId);
            });
    }

    /**
     * Creates a system auction from a configured pet template.
     *
     * @param actor actor UUID
     * @param petTemplateId pet template id
     * @param startingPrice starting bid
     * @return future completed with created listing
     */
    public CompletableFuture<AuctionListing> startSystemAuction(UUID actor, String petTemplateId, BigDecimal startingPrice) {
        LinPetConfig config = configRef.get();
        if (!config.pets().containsKey(petTemplateId)) {
            CompletableFuture<AuctionListing> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalArgumentException("unknown pet template '" + petTemplateId + "'"));
            return failed;
        }
        if (startingPrice == null || startingPrice.signum() < 0) {
            CompletableFuture<AuctionListing> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalArgumentException("starting price must not be negative"));
            return failed;
        }
        AuctionListing listing = new AuctionListing(
            UUID.randomUUID(),
            null,
            null,
            petTemplateId,
            startingPrice,
            null,
            Instant.now().plusSeconds(config.settings().auctionDurationMinutes() * 60L),
            0L,
            AuctionStatus.ACTIVE
        );
        return runAsync(() -> {
            auctionRepository.save(listing);
            auditLogRepository.record(actor, "AUCTION_START", listing.auctionId().toString(), "pet=" + petTemplateId);
            return listing;
        });
    }

    /**
     * Lists active auctions asynchronously.
     *
     * @return active listings
     */
    public CompletableFuture<List<AuctionListing>> activeListings() {
        return runAsync(auctionRepository::activeListings);
    }

    /**
     * Cancels an active auction and refunds the highest bidder when needed.
     *
     * @param actor actor UUID, or {@code null} for console/system
     * @param auctionId auction id
     * @return future completed with true when the auction was stopped
     */
    public CompletableFuture<Boolean> stopAuction(UUID actor, UUID auctionId) {
        if (auctionId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("auctionId must not be null"));
        }
        return runAsync(() -> {
            Optional<AuctionListing> listing = auctionRepository.find(auctionId);
            if (listing.isEmpty() || listing.get().status() != AuctionStatus.ACTIVE) {
                return Optional.<AuctionListing>empty();
            }
            boolean stopped = auctionRepository.updateStatus(auctionId, AuctionStatus.ACTIVE, AuctionStatus.CANCELLED);
            if (!stopped) {
                return Optional.<AuctionListing>empty();
            }
            auditLogRepository.record(actor, "AUCTION_STOP", auctionId.toString(), "status=CANCELLED");
            return listing;
        }).thenApply(stopped -> {
            stopped.ifPresent(listing -> scheduler.runGlobal(() -> refundPreviousBidder(listing)));
            return stopped.isPresent();
        });
    }

    /**
     * Extends an active auction.
     *
     * @param actor actor UUID, or {@code null} for console/system
     * @param auctionId auction id
     * @param minutes minutes to add from the later of now or the current end timestamp
     * @return future completed with true when the auction was extended
     */
    public CompletableFuture<Boolean> extendAuction(UUID actor, UUID auctionId, long minutes) {
        if (auctionId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("auctionId must not be null"));
        }
        if (minutes <= 0L) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("minutes must be positive"));
        }
        return runAsync(() -> {
            Optional<AuctionListing> listing = auctionRepository.find(auctionId);
            if (listing.isEmpty() || listing.get().status() != AuctionStatus.ACTIVE) {
                return false;
            }
            Instant now = Instant.now();
            Instant base = listing.get().endsAt().isAfter(now) ? listing.get().endsAt() : now;
            Instant newEndsAt = base.plusSeconds(Math.multiplyExact(minutes, 60L));
            boolean extended = auctionRepository.extend(auctionId, newEndsAt);
            if (extended) {
                auditLogRepository.record(actor, "AUCTION_EXTEND", auctionId.toString(), "endsAt=" + newEndsAt);
            }
            return extended;
        });
    }

    /**
     * Settles expired auctions and retries listings previously claimed as ended.
     *
     * @return future completed with the number of listings settled or cancelled
     */
    public CompletableFuture<Integer> settleExpiredAuctions() {
        Instant now = Instant.now();
        return runAsync(() -> {
            List<AuctionListing> candidates = new ArrayList<>();
            candidates.addAll(auctionRepository.expiredActiveListings(now));
            candidates.addAll(auctionRepository.pendingSettlementListings());
            return List.copyOf(candidates);
        }).thenCompose(candidates -> {
            CompletableFuture<?>[] settlements = candidates.stream()
                .map(listing -> settleListing(listing).exceptionally(throwable -> {
                    plugin.getLogger().log(Level.SEVERE, "Failed settling auction " + listing.auctionId(), throwable);
                    return false;
                }))
                .toArray(CompletableFuture[]::new);
            return CompletableFuture.allOf(settlements)
                .thenApply(ignored -> {
                    int completed = 0;
                    for (CompletableFuture<?> settlement : settlements) {
                        if (Boolean.TRUE.equals(settlement.join())) {
                            completed++;
                        }
                    }
                    return completed;
                });
        });
    }

    private BidCommit commitBid(UUID bidderId, UUID auctionId, BigDecimal amount) {
        AuctionEngine engine = new AuctionEngine(configRef.get().settings().auctionRules());
        Optional<AuctionListing> first = auctionRepository.find(auctionId);
        if (first.isEmpty()) {
            throw new IllegalArgumentException("auction not found");
        }
        AuctionListing previous = first.get();
        AuctionListing updated = engine.placeBid(previous, bidderId, amount, Instant.now());
        if (auctionRepository.updateBid(updated, previous.version())) {
            auditLogRepository.record(bidderId, "AUCTION_BID", auctionId.toString(), "amount=" + amount.toPlainString());
            return new BidCommit(previous, updated);
        }
        Optional<AuctionListing> retry = auctionRepository.find(auctionId);
        if (retry.isEmpty()) {
            throw new IllegalStateException("auction disappeared during bid retry");
        }
        previous = retry.get();
        updated = engine.placeBid(previous, bidderId, amount, Instant.now());
        if (!auctionRepository.updateBid(updated, previous.version())) {
            throw new IllegalStateException("auction price changed, please retry");
        }
        auditLogRepository.record(bidderId, "AUCTION_BID", auctionId.toString(), "amount=" + amount.toPlainString());
        return new BidCommit(previous, updated);
    }

    private CompletableFuture<Boolean> settleListing(AuctionListing listing) {
        if (listing.status() == AuctionStatus.ACTIVE) {
            return runAsync(() -> auctionRepository.updateStatus(listing.auctionId(), AuctionStatus.ACTIVE, AuctionStatus.ENDED))
                .thenCompose(claimed -> claimed
                    ? settleEndedAuction(listing.auctionId())
                    : CompletableFuture.completedFuture(false));
        }
        if (listing.status() == AuctionStatus.ENDED) {
            return settleEndedAuction(listing.auctionId());
        }
        return CompletableFuture.completedFuture(false);
    }

    private CompletableFuture<Boolean> settleEndedAuction(UUID auctionId) {
        return runAsync(() -> auctionRepository.find(auctionId)
                .orElseThrow(() -> new IllegalStateException("auction not found during settlement")))
            .thenCompose(listing -> {
                if (listing.status() != AuctionStatus.ENDED) {
                    return CompletableFuture.completedFuture(false);
                }
                if (listing.highestBidder() == null) {
                    return cancelEndedWithoutBidder(listing);
                }
                UUID winnerPetId = UUID.nameUUIDFromBytes(("linpet-auction:" + auctionId).getBytes(StandardCharsets.UTF_8));
                return petService.createStoredPet(null, listing.highestBidder(), listing.petTemplateId(), 1, winnerPetId)
                    .thenCompose(pet -> markSettled(listing, pet.instanceId()).thenCompose(settled -> {
                        if (!settled) {
                            return CompletableFuture.completedFuture(false);
                        }
                        Player winner = Bukkit.getPlayer(listing.highestBidder());
                        if (winner != null) {
                            scheduler.runOnEntity(winner, () -> petService.deliverStoredPetItem(winner, pet));
                        }
                        return paySeller(listing).thenApply(ignored -> true);
                    }));
            });
    }

    private CompletableFuture<Boolean> cancelEndedWithoutBidder(AuctionListing listing) {
        return runAsync(() -> {
            boolean cancelled = auctionRepository.updateStatus(listing.auctionId(), AuctionStatus.ENDED, AuctionStatus.CANCELLED);
            if (cancelled) {
                auditLogRepository.record(null, "AUCTION_CANCEL_NO_BID", listing.auctionId().toString(),
                    "pet=" + listing.petTemplateId());
            }
            return cancelled;
        });
    }

    private CompletableFuture<Boolean> markSettled(AuctionListing listing, UUID petInstanceId) {
        return runAsync(() -> {
            boolean settled = auctionRepository.updateStatus(listing.auctionId(), AuctionStatus.ENDED, AuctionStatus.SETTLED);
            if (settled) {
                auditLogRepository.record(null, "AUCTION_SETTLE", listing.auctionId().toString(),
                    "winner=" + listing.highestBidder() + ",petInstance=" + petInstanceId
                        + ",amount=" + listing.currentBid().toPlainString());
            }
            return settled;
        });
    }

    private CompletableFuture<Boolean> paySeller(AuctionListing listing) {
        if (listing.sellerId() == null || listing.currentBid().signum() <= 0) {
            return CompletableFuture.completedFuture(true);
        }
        BigDecimal net = sellerNetAmount(listing.currentBid());
        if (net.signum() <= 0) {
            return CompletableFuture.completedFuture(true);
        }
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        scheduler.runGlobal(() -> {
            OfflinePlayer seller = Bukkit.getOfflinePlayer(listing.sellerId());
            EconomyResult result = economyProvider.deposit(seller, net);
            if (!result.success()) {
                plugin.getLogger().severe("Failed paying seller " + listing.sellerId()
                    + " for auction " + listing.auctionId() + ": " + result.message());
                future.complete(false);
                return;
            }
            auditLogRepository.record(null, "AUCTION_PAYOUT", listing.auctionId().toString(),
                "seller=" + listing.sellerId() + ",net=" + net.toPlainString());
            future.complete(true);
        });
        return future;
    }

    private BigDecimal sellerNetAmount(BigDecimal bidAmount) {
        BigDecimal taxPercent = configRef.get().settings().auctionRules().taxPercent();
        BigDecimal multiplier = BigDecimal.valueOf(100).subtract(taxPercent);
        return bidAmount.multiply(multiplier).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
    }

    private void refundPreviousBidder(AuctionListing previous) {
        if (previous.highestBidder() == null || previous.currentBid().signum() <= 0) {
            return;
        }
        OfflinePlayer previousBidder = Bukkit.getOfflinePlayer(previous.highestBidder());
        EconomyResult result = economyProvider.deposit(previousBidder, previous.currentBid());
        if (!result.success()) {
            plugin.getLogger().severe("Failed refunding previous bidder " + previous.highestBidder()
                + " for auction " + previous.auctionId() + ": " + result.message());
        }
    }

    private <T> CompletableFuture<T> runAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        scheduler.runAsync(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    private record BidCommit(AuctionListing previous, AuctionListing updated) {
    }
}

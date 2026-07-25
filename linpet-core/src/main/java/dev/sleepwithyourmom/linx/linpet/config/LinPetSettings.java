package dev.sleepwithyourmom.linx.linpet.config;

import dev.sleepwithyourmom.linx.linpet.domain.auction.AuctionRules;
import dev.sleepwithyourmom.linx.linpet.domain.buff.BuffScalingConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Validated plugin-wide settings loaded from {@code config.yml}.
 *
 * @param language configured language, or {@code auto}
 * @param enableFolia whether Folia adapter detection is enabled
 * @param database database settings
 * @param defaultPetLimit default stored pet limit
 * @param maxSlotsPerPlayer maximum equipment slots
 * @param defaultSlots default active slots
 * @param slotBuyPrice price per extra slot
 * @param blacklistWorlds blacklisted world names
 * @param scaling buff scaling config
 * @param auctionRules auction rules
 * @param systemAuctionPets system pet templates eligible for auction
 * @param auctionDurationMinutes default auction duration
 * @param startingAuctionPrice default auction starting price
 * @param allowPlayerAuction whether player auctions are enabled
 * @param auctionCooldownSeconds cooldown between auctions
 */
public record LinPetSettings(
    String language,
    boolean enableFolia,
    DatabaseSettings database,
    int defaultPetLimit,
    int maxSlotsPerPlayer,
    int defaultSlots,
    double slotBuyPrice,
    Set<String> blacklistWorlds,
    BuffScalingConfig scaling,
    AuctionRules auctionRules,
    List<String> systemAuctionPets,
    int auctionDurationMinutes,
    BigDecimal startingAuctionPrice,
    boolean allowPlayerAuction,
    int auctionCooldownSeconds
) {
    /**
     * Creates validated settings.
     */
    public LinPetSettings {
        if (language == null || language.isBlank()) {
            language = "auto";
        }
        if (database == null) {
            throw new ConfigValidationException("settings.database must be configured");
        }
        if (defaultPetLimit < 0) {
            throw new ConfigValidationException("settings.default-pet-limit must not be negative");
        }
        if (maxSlotsPerPlayer < 1 || maxSlotsPerPlayer > 20) {
            throw new ConfigValidationException("settings.max-slots-per-player must be between 1 and 20");
        }
        if (defaultSlots < 0 || defaultSlots > maxSlotsPerPlayer) {
            throw new ConfigValidationException("slots.default must be between 0 and settings.max-slots-per-player");
        }
        if (slotBuyPrice < 0.0D || !Double.isFinite(slotBuyPrice)) {
            throw new ConfigValidationException("slots.buy-price-per-slot must be finite and non-negative");
        }
        blacklistWorlds = Set.copyOf(blacklistWorlds == null ? Set.of() : blacklistWorlds);
        if (scaling == null) {
            throw new ConfigValidationException("buff-scaling must be configured");
        }
        if (auctionRules == null) {
            throw new ConfigValidationException("auction rules must be configured");
        }
        systemAuctionPets = List.copyOf(systemAuctionPets == null ? List.of() : systemAuctionPets);
        if (auctionDurationMinutes < 1) {
            throw new ConfigValidationException("auction.duration-minutes must be at least 1");
        }
        if (startingAuctionPrice == null || startingAuctionPrice.signum() < 0) {
            throw new ConfigValidationException("auction.starting-price must not be negative");
        }
        if (auctionCooldownSeconds < 0) {
            throw new ConfigValidationException("auction.cooldown-between-auctions must not be negative");
        }
    }
}

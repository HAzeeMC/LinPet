package dev.sleepwithyourmom.linx.linpet.domain.rank;

/**
 * Rank configuration that controls utility limits without granting direct strength buffs.
 *
 * @param id rank id
 * @param displayName display name
 * @param slotLimit equipment slot limit
 * @param auctionFeeDiscountPercent auction fee discount percentage
 * @param bypassBlacklist whether this rank bypasses world blacklist restrictions
 */
public record RankDefinition(
    String id,
    String displayName,
    int slotLimit,
    double auctionFeeDiscountPercent,
    boolean bypassBlacklist
) {
    /**
     * Creates a validated rank definition.
     */
    public RankDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (slotLimit < 0 || slotLimit > 20) {
            throw new IllegalArgumentException("slotLimit must be between 0 and 20");
        }
        if (!Double.isFinite(auctionFeeDiscountPercent)
            || auctionFeeDiscountPercent < 0.0D
            || auctionFeeDiscountPercent > 100.0D) {
            throw new IllegalArgumentException("auctionFeeDiscountPercent must be between 0 and 100");
        }
    }
}

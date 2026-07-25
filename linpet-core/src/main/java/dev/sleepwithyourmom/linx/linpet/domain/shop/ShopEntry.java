package dev.sleepwithyourmom.linx.linpet.domain.shop;

import java.math.BigDecimal;

/**
 * Shop configuration for a purchasable pet template.
 *
 * @param petTemplateId pet template id
 * @param price purchase price
 * @param dailyLimit maximum purchases per day, or {@code 0} for unlimited
 */
public record ShopEntry(String petTemplateId, BigDecimal price, int dailyLimit) {
    /**
     * Creates a validated shop entry.
     */
    public ShopEntry {
        if (petTemplateId == null || petTemplateId.isBlank()) {
            throw new IllegalArgumentException("petTemplateId must not be blank");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("price must not be negative");
        }
        if (dailyLimit < 0) {
            throw new IllegalArgumentException("dailyLimit must not be negative");
        }
    }
}

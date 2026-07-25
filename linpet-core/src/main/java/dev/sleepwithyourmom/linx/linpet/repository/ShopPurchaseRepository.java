package dev.sleepwithyourmom.linx.linpet.repository;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Repository for daily shop purchase counters.
 */
public interface ShopPurchaseRepository {
    /**
     * Reserves one purchase when the configured daily limit allows it.
     *
     * @param playerId player UUID
     * @param petTemplateId pet template id
     * @param day purchase day
     * @param dailyLimit daily limit, or {@code 0} for unlimited
     * @return true when the purchase was reserved
     */
    boolean reserve(UUID playerId, String petTemplateId, LocalDate day, int dailyLimit);

    /**
     * Releases a previously reserved purchase.
     *
     * @param playerId player UUID
     * @param petTemplateId pet template id
     * @param day purchase day
     */
    void release(UUID playerId, String petTemplateId, LocalDate day);
}

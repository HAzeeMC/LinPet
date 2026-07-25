package dev.sleepwithyourmom.linx.linpet.service;

import java.util.UUID;

/**
 * Result of a shop purchase flow.
 *
 * @param success whether purchase completed
 * @param messageKey message key
 * @param petInstanceId created pet id, or {@code null} when failed
 */
public record ShopPurchaseResult(boolean success, String messageKey, UUID petInstanceId) {
    /**
     * Creates a success result.
     *
     * @param petInstanceId created pet id
     * @return result
     */
    public static ShopPurchaseResult success(UUID petInstanceId) {
        return new ShopPurchaseResult(true, "shop.purchase-success", petInstanceId);
    }

    /**
     * Creates a failure result.
     *
     * @param messageKey message key
     * @return result
     */
    public static ShopPurchaseResult failure(String messageKey) {
        return new ShopPurchaseResult(false, messageKey, null);
    }
}

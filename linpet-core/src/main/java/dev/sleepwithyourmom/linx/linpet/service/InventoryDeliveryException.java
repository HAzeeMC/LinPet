package dev.sleepwithyourmom.linx.linpet.service;

/**
 * Raised when a pet item cannot be delivered to the target inventory.
 */
public class InventoryDeliveryException extends RuntimeException {
    /**
     * Creates an inventory delivery exception.
     *
     * @param message failure description
     */
    public InventoryDeliveryException(String message) {
        super(message);
    }
}

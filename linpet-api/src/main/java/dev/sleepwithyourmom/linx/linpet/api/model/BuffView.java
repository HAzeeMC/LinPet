package dev.sleepwithyourmom.linx.linpet.api.model;

/**
 * Read-only view of an aggregated buff entry.
 *
 * @param key namespaced attribute key or potion effect key
 * @param amount final numeric amount after scaling
 * @param operation operation name used for attributes, or {@code POTION} for potion effects
 */
public record BuffView(String key, double amount, String operation) {
    /**
     * Creates a validated buff view.
     */
    public BuffView {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException("amount must be finite");
        }
        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("operation must not be blank");
        }
    }
}

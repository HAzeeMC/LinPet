package dev.sleepwithyourmom.linx.linpet.domain.buff;

/**
 * Attribute modifier definition independent from Bukkit runtime types.
 *
 * @param attributeKey namespaced attribute key
 * @param amount modifier amount
 * @param operation modifier operation
 */
public record AttributeBuffDefinition(
    String attributeKey,
    double amount,
    AttributeOperation operation
) implements BuffDefinition {
    /**
     * Creates a validated attribute buff.
     */
    public AttributeBuffDefinition {
        if (attributeKey == null || attributeKey.isBlank()) {
            throw new IllegalArgumentException("attributeKey must not be blank");
        }
        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException("amount must be finite");
        }
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
    }

    @Override
    public String key() {
        return attributeKey;
    }
}

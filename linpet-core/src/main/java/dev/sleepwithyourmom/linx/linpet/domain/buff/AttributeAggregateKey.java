package dev.sleepwithyourmom.linx.linpet.domain.buff;

/**
 * Key used for aggregating attribute modifiers without losing their operation semantics.
 *
 * @param attributeKey namespaced attribute key
 * @param operation modifier operation
 */
public record AttributeAggregateKey(String attributeKey, AttributeOperation operation)
    implements Comparable<AttributeAggregateKey> {
    /**
     * Creates a validated aggregate key.
     */
    public AttributeAggregateKey {
        if (attributeKey == null || attributeKey.isBlank()) {
            throw new IllegalArgumentException("attributeKey must not be blank");
        }
        if (operation == null) {
            throw new IllegalArgumentException("operation must not be null");
        }
    }

    @Override
    public int compareTo(AttributeAggregateKey other) {
        int attributeComparison = attributeKey.compareTo(other.attributeKey);
        if (attributeComparison != 0) {
            return attributeComparison;
        }
        return operation.compareTo(other.operation);
    }
}

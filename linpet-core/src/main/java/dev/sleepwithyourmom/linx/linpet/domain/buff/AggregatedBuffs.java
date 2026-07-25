package dev.sleepwithyourmom.linx.linpet.domain.buff;

import java.util.List;
import java.util.Map;

/**
 * Immutable aggregate of every active pet and skill buff for one player.
 *
 * @param attributes summed attribute modifiers keyed by attribute and operation
 * @param potions strongest potion effect per effect key
 */
public record AggregatedBuffs(
    Map<AttributeAggregateKey, Double> attributes,
    Map<String, PotionBuffDefinition> potions
) {
    /**
     * Creates a defensive aggregate snapshot.
     */
    public AggregatedBuffs {
        attributes = Map.copyOf(attributes == null ? Map.of() : attributes);
        potions = Map.copyOf(potions == null ? Map.of() : potions);
    }

    /**
     * Returns all attribute keys in deterministic order.
     *
     * @return sorted attribute aggregate keys
     */
    public List<AttributeAggregateKey> sortedAttributeKeys() {
        return attributes.keySet().stream()
            .sorted()
            .toList();
    }
}

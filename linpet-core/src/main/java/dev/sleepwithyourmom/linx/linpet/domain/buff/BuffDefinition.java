package dev.sleepwithyourmom.linx.linpet.domain.buff;

/**
 * Marker for a declarative buff loaded from configuration.
 */
public sealed interface BuffDefinition permits AttributeBuffDefinition, PotionBuffDefinition {
    /**
     * Returns a stable key identifying the configured buff target.
     *
     * @return effect key or attribute key
     */
    String key();
}

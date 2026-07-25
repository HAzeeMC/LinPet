package dev.sleepwithyourmom.linx.linpet.domain.buff;

/**
 * Potion effect definition independent from Bukkit runtime types.
 *
 * @param effectKey effect key or Bukkit-style effect name
 * @param amplifier zero-based amplifier
 * @param durationSeconds duration in seconds, or {@code -1} while equipped
 */
public record PotionBuffDefinition(
    String effectKey,
    int amplifier,
    int durationSeconds
) implements BuffDefinition {
    /**
     * Creates a validated potion buff.
     */
    public PotionBuffDefinition {
        if (effectKey == null || effectKey.isBlank()) {
            throw new IllegalArgumentException("effectKey must not be blank");
        }
        if (amplifier < 0) {
            throw new IllegalArgumentException("amplifier must not be negative");
        }
        if (durationSeconds != -1 && durationSeconds <= 0) {
            throw new IllegalArgumentException("durationSeconds must be -1 or positive");
        }
    }

    @Override
    public String key() {
        return effectKey;
    }
}

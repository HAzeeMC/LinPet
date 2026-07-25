package dev.sleepwithyourmom.linx.linpet.domain.buff;

/**
 * Configuration for level-based buff scaling.
 *
 * @param enabled whether level scaling is enabled
 * @param scalingPerLevel multiplier added per level after level one
 * @param maxLevel maximum pet level
 */
public record BuffScalingConfig(boolean enabled, double scalingPerLevel, int maxLevel) {
    /**
     * Creates a validated scaling config.
     */
    public BuffScalingConfig {
        if (scalingPerLevel < 0.0D || !Double.isFinite(scalingPerLevel)) {
            throw new IllegalArgumentException("scalingPerLevel must be finite and non-negative");
        }
        if (maxLevel < 1) {
            throw new IllegalArgumentException("maxLevel must be at least 1");
        }
    }

    /**
     * Returns the effective multiplier for a level.
     *
     * @param level pet level
     * @return multiplier applied to configured buff amounts
     */
    public double multiplierForLevel(int level) {
        if (!enabled) {
            return 1.0D;
        }
        int clamped = Math.max(1, Math.min(level, maxLevel));
        return 1.0D + ((clamped - 1) * scalingPerLevel);
    }
}

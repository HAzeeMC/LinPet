package dev.sleepwithyourmom.linx.linpet.domain.pet;

import dev.sleepwithyourmom.linx.linpet.domain.buff.BuffScalingConfig;

/**
 * Pure progression logic for pet experience and level-up calculations.
 */
public class PetProgression {
    private final BuffScalingConfig scalingConfig;

    /**
     * Creates progression logic.
     *
     * @param scalingConfig level cap configuration
     */
    public PetProgression(BuffScalingConfig scalingConfig) {
        if (scalingConfig == null) {
            throw new IllegalArgumentException("scalingConfig must not be null");
        }
        this.scalingConfig = scalingConfig;
    }

    /**
     * Applies experience and returns the updated pet state.
     *
     * @param pet pet instance
     * @param gainedExperience experience gained
     * @return updated pet instance
     */
    public PetInstance gainExperience(PetInstance pet, double gainedExperience) {
        if (pet == null) {
            throw new IllegalArgumentException("pet must not be null");
        }
        if (gainedExperience < 0.0D || !Double.isFinite(gainedExperience)) {
            throw new IllegalArgumentException("gainedExperience must be finite and non-negative");
        }
        int level = pet.level();
        double experience = pet.experience() + gainedExperience;
        int skillPoints = pet.skillPoints();
        while (level < scalingConfig.maxLevel()) {
            double required = requiredExperience(level);
            if (experience < required) {
                break;
            }
            experience -= required;
            level++;
            skillPoints++;
        }
        if (level >= scalingConfig.maxLevel()) {
            level = scalingConfig.maxLevel();
            experience = 0.0D;
        }
        return pet.withProgress(level, experience, skillPoints);
    }

    /**
     * Returns required experience to advance from a level.
     *
     * @param level current level
     * @return required experience
     */
    public double requiredExperience(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1");
        }
        return 100.0D + ((level - 1) * 35.0D);
    }
}

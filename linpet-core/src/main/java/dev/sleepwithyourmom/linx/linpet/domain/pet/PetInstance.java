package dev.sleepwithyourmom.linx.linpet.domain.pet;

import dev.sleepwithyourmom.linx.linpet.api.model.PetRarity;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable per-player pet instance state.
 *
 * @param instanceId unique instance id
 * @param ownerId owner player UUID
 * @param templateId pet template id
 * @param level current level
 * @param experience current experience
 * @param skillPoints unspent skill points
 * @param unlockedSkillIds unlocked skill ids
 * @param rarity rarity persisted with the instance
 * @param skinId configured skin id or texture reference
 * @param customName optional sanitized custom name
 * @param expiresAt expiration time, or {@code null} for permanent pets
 */
public record PetInstance(
    UUID instanceId,
    UUID ownerId,
    String templateId,
    int level,
    double experience,
    int skillPoints,
    Set<String> unlockedSkillIds,
    PetRarity rarity,
    String skinId,
    String customName,
    Instant expiresAt
) {
    /**
     * Creates a validated pet instance.
     */
    public PetInstance {
        if (instanceId == null) {
            throw new IllegalArgumentException("instanceId must not be null");
        }
        if (ownerId == null) {
            throw new IllegalArgumentException("ownerId must not be null");
        }
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId must not be blank");
        }
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1");
        }
        if (experience < 0.0D || !Double.isFinite(experience)) {
            throw new IllegalArgumentException("experience must be finite and non-negative");
        }
        if (skillPoints < 0) {
            throw new IllegalArgumentException("skillPoints must not be negative");
        }
        if (rarity == null) {
            throw new IllegalArgumentException("rarity must not be null");
        }
        if (skinId == null || skinId.isBlank()) {
            throw new IllegalArgumentException("skinId must not be blank");
        }
        if (customName != null && customName.isBlank()) {
            customName = null;
        }
        unlockedSkillIds = Set.copyOf(unlockedSkillIds == null ? Set.of() : unlockedSkillIds);
    }

    /**
     * Checks whether this pet has expired.
     *
     * @param now current time
     * @return true when the pet has an expiration before or equal to now
     */
    public boolean isExpired(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    /**
     * Returns the preferred display name.
     *
     * @param definition template definition
     * @return custom name when present, otherwise template display name
     */
    public String displayName(PetDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null");
        }
        return customName == null ? definition.displayName() : customName;
    }

    /**
     * Returns a copy with modified level, experience, and skill point values.
     *
     * @param newLevel new level
     * @param newExperience new experience
     * @param newSkillPoints new skill points
     * @return modified pet instance
     */
    public PetInstance withProgress(int newLevel, double newExperience, int newSkillPoints) {
        return new PetInstance(
            instanceId,
            ownerId,
            templateId,
            newLevel,
            newExperience,
            newSkillPoints,
            unlockedSkillIds,
            rarity,
            skinId,
            customName,
            expiresAt
        );
    }

    /**
     * Returns a copy with one additional unlocked skill and spent points.
     *
     * @param skillId skill id to add
     * @param remainingSkillPoints points remaining after unlock
     * @return modified pet instance
     */
    public PetInstance withUnlockedSkill(String skillId, int remainingSkillPoints) {
        if (skillId == null || skillId.isBlank()) {
            throw new IllegalArgumentException("skillId must not be blank");
        }
        java.util.LinkedHashSet<String> skills = new java.util.LinkedHashSet<>(unlockedSkillIds);
        skills.add(skillId);
        return new PetInstance(
            instanceId,
            ownerId,
            templateId,
            level,
            experience,
            remainingSkillPoints,
            skills,
            rarity,
            skinId,
            customName,
            expiresAt
        );
    }
}

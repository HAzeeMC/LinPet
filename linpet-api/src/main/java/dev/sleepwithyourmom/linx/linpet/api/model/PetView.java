package dev.sleepwithyourmom.linx.linpet.api.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable read-only view of a pet instance exposed by the public API.
 *
 * @param instanceId unique pet instance identifier
 * @param ownerId owner player UUID
 * @param templateId configured pet template identifier
 * @param displayName rendered plain display name without color markup
 * @param rarity rarity tier
 * @param level current level
 * @param experience current experience points
 * @param skillPoints unspent skill points
 * @param unlockedSkillIds unlocked skill identifiers
 * @param equipped whether this pet is currently equipped
 * @param equippedSlot equipped slot, or {@code -1} when not equipped
 * @param expiresAt expiration timestamp, or {@code null} for permanent pets
 */
public record PetView(
    UUID instanceId,
    UUID ownerId,
    String templateId,
    String displayName,
    PetRarity rarity,
    int level,
    double experience,
    int skillPoints,
    Set<String> unlockedSkillIds,
    boolean equipped,
    int equippedSlot,
    Instant expiresAt
) {
    /**
     * Creates a sanitized immutable pet view.
     */
    public PetView {
        if (instanceId == null) {
            throw new IllegalArgumentException("instanceId must not be null");
        }
        if (ownerId == null) {
            throw new IllegalArgumentException("ownerId must not be null");
        }
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (rarity == null) {
            throw new IllegalArgumentException("rarity must not be null");
        }
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1");
        }
        if (experience < 0.0D) {
            throw new IllegalArgumentException("experience must not be negative");
        }
        if (skillPoints < 0) {
            throw new IllegalArgumentException("skillPoints must not be negative");
        }
        if (equippedSlot < -1) {
            throw new IllegalArgumentException("equippedSlot must be -1 or greater");
        }
        unlockedSkillIds = Set.copyOf(unlockedSkillIds == null ? Set.of() : unlockedSkillIds);
    }
}

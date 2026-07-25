package dev.sleepwithyourmom.linx.linpet.domain.pet;

import dev.sleepwithyourmom.linx.linpet.api.model.PetRarity;
import dev.sleepwithyourmom.linx.linpet.domain.buff.AttributeBuffDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.buff.PotionBuffDefinition;
import java.util.List;

/**
 * Immutable template describing a pet type loaded from {@code pets.yml}.
 *
 * @param id stable template identifier
 * @param displayName display name without formatting side effects
 * @param rarity rarity tier
 * @param defaultSkin configured skin reference
 * @param potionBuffs base potion buffs
 * @param attributeBuffs base attribute buffs
 * @param specialAbilities descriptive ability text
 */
public record PetDefinition(
    String id,
    String displayName,
    PetRarity rarity,
    String defaultSkin,
    List<PotionBuffDefinition> potionBuffs,
    List<AttributeBuffDefinition> attributeBuffs,
    List<String> specialAbilities
) {
    /**
     * Creates a validated pet definition.
     */
    public PetDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (rarity == null) {
            throw new IllegalArgumentException("rarity must not be null");
        }
        if (defaultSkin == null || defaultSkin.isBlank()) {
            throw new IllegalArgumentException("defaultSkin must not be blank");
        }
        potionBuffs = List.copyOf(potionBuffs == null ? List.of() : potionBuffs);
        attributeBuffs = List.copyOf(attributeBuffs == null ? List.of() : attributeBuffs);
        specialAbilities = List.copyOf(specialAbilities == null ? List.of() : specialAbilities);
    }
}

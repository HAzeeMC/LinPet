package dev.sleepwithyourmom.linx.linpet.config;

import dev.sleepwithyourmom.linx.linpet.domain.pet.PetDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.rank.RankDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.shop.ShopEntry;
import dev.sleepwithyourmom.linx.linpet.domain.skill.SkillTreeDefinition;
import java.util.Map;

/**
 * Complete validated configuration snapshot.
 *
 * @param settings global settings
 * @param pets pet definitions
 * @param ranks rank definitions
 * @param shopEntries shop entries
 * @param skillTrees skill trees
 * @param skinRegistry configured skin aliases
 */
public record LinPetConfig(
    LinPetSettings settings,
    Map<String, PetDefinition> pets,
    Map<String, RankDefinition> ranks,
    Map<String, ShopEntry> shopEntries,
    Map<String, SkillTreeDefinition> skillTrees,
    SkinRegistry skinRegistry
) {
    /**
     * Creates a defensive config snapshot.
     */
    public LinPetConfig {
        if (settings == null) {
            throw new ConfigValidationException("settings must not be null");
        }
        pets = Map.copyOf(pets == null ? Map.of() : pets);
        ranks = Map.copyOf(ranks == null ? Map.of() : ranks);
        shopEntries = Map.copyOf(shopEntries == null ? Map.of() : shopEntries);
        skillTrees = Map.copyOf(skillTrees == null ? Map.of() : skillTrees);
        if (skinRegistry == null) {
            skinRegistry = new SkinRegistry(Map.of());
        }
        validateReferences();
    }

    private void validateReferences() {
        shopEntries.values().forEach(entry -> {
            if (!pets.containsKey(entry.petTemplateId())) {
                throw new ConfigValidationException("shop.yml references unknown pet '" + entry.petTemplateId() + "'");
            }
        });
        skillTrees.keySet().forEach(petId -> {
            if (!pets.containsKey(petId)) {
                throw new ConfigValidationException("skilltree.yml defines tree for unknown pet '" + petId + "'");
            }
        });
        settings.systemAuctionPets().forEach(petId -> {
            if (!pets.containsKey(petId)) {
                throw new ConfigValidationException("config.yml auction.system-pets references unknown pet '" + petId + "'");
            }
        });
    }
}

package dev.sleepwithyourmom.linx.linpet.domain.buff;

import dev.sleepwithyourmom.linx.linpet.domain.pet.PetDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;
import dev.sleepwithyourmom.linx.linpet.domain.skill.SkillDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.skill.SkillTreeDefinition;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Aggregates pet template buffs and unlocked skill buffs without touching Bukkit APIs.
 */
public class BuffCalculator {
    private final BuffScalingConfig scalingConfig;

    /**
     * Creates a buff calculator.
     *
     * @param scalingConfig scaling behavior
     */
    public BuffCalculator(BuffScalingConfig scalingConfig) {
        if (scalingConfig == null) {
            throw new IllegalArgumentException("scalingConfig must not be null");
        }
        this.scalingConfig = scalingConfig;
    }

    /**
     * Computes aggregate buffs from equipped pets.
     *
     * @param equippedPets equipped pets by slot
     * @param petDefinitions definitions by template id
     * @param skillTrees skill tree definitions by pet template id
     * @return immutable aggregate
     */
    public AggregatedBuffs aggregate(
        Map<Integer, PetInstance> equippedPets,
        Map<String, PetDefinition> petDefinitions,
        Map<String, SkillTreeDefinition> skillTrees
    ) {
        if (equippedPets == null || petDefinitions == null || skillTrees == null) {
            throw new IllegalArgumentException("aggregate inputs must not be null");
        }
        Map<AttributeAggregateKey, Double> attributes = new HashMap<>();
        Map<String, PotionBuffDefinition> potions = new HashMap<>();
        for (PetInstance pet : equippedPets.values()) {
            PetDefinition definition = petDefinitions.get(pet.templateId());
            if (definition == null) {
                continue;
            }
            double multiplier = scalingConfig.multiplierForLevel(pet.level());
            definition.attributeBuffs().forEach(buff -> addAttribute(attributes, buff, multiplier));
            definition.potionBuffs().forEach(buff -> addPotion(potions, buff));

            Optional.ofNullable(skillTrees.get(pet.templateId())).ifPresent(tree -> {
                for (String skillId : pet.unlockedSkillIds()) {
                    SkillDefinition skill = tree.skills().get(skillId);
                    if (skill == null) {
                        continue;
                    }
                    skill.attributeBuffs().forEach(buff -> addAttribute(attributes, buff, multiplier));
                    skill.potionBuffs().forEach(buff -> addPotion(potions, buff));
                }
            });
        }
        return new AggregatedBuffs(attributes, potions);
    }

    private void addAttribute(Map<AttributeAggregateKey, Double> attributes, AttributeBuffDefinition buff, double multiplier) {
        AttributeAggregateKey key = new AttributeAggregateKey(buff.attributeKey(), buff.operation());
        attributes.merge(key, buff.amount() * multiplier, Double::sum);
    }

    private void addPotion(Map<String, PotionBuffDefinition> potions, PotionBuffDefinition buff) {
        potions.merge(buff.effectKey(), buff, (left, right) -> {
            if (right.amplifier() > left.amplifier()) {
                return right;
            }
            if (right.amplifier() == left.amplifier() && durationScore(right.durationSeconds()) > durationScore(left.durationSeconds())) {
                return right;
            }
            return left;
        });
    }

    private int durationScore(int durationSeconds) {
        return durationSeconds == -1 ? Integer.MAX_VALUE : durationSeconds;
    }
}

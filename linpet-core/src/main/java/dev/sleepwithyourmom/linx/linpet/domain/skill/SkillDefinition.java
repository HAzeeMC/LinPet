package dev.sleepwithyourmom.linx.linpet.domain.skill;

import dev.sleepwithyourmom.linx.linpet.domain.buff.AttributeBuffDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.buff.PotionBuffDefinition;
import java.util.List;
import java.util.Set;

/**
 * Immutable skill node definition loaded from {@code skilltree.yml}.
 *
 * @param id unique skill id
 * @param branch logical branch name
 * @param displayName display name
 * @param description short description
 * @param pointCost skill point cost
 * @param requiredLevel minimum pet level
 * @param prerequisites prerequisite skill ids
 * @param potionBuffs potion buffs granted by this skill
 * @param attributeBuffs attribute buffs granted by this skill
 */
public record SkillDefinition(
    String id,
    String branch,
    String displayName,
    String description,
    int pointCost,
    int requiredLevel,
    Set<String> prerequisites,
    List<PotionBuffDefinition> potionBuffs,
    List<AttributeBuffDefinition> attributeBuffs
) {
    /**
     * Creates a validated skill definition.
     */
    public SkillDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException("branch must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (description == null) {
            description = "";
        }
        if (pointCost < 0) {
            throw new IllegalArgumentException("pointCost must not be negative");
        }
        if (requiredLevel < 1) {
            throw new IllegalArgumentException("requiredLevel must be at least 1");
        }
        prerequisites = Set.copyOf(prerequisites == null ? Set.of() : prerequisites);
        potionBuffs = List.copyOf(potionBuffs == null ? List.of() : potionBuffs);
        attributeBuffs = List.copyOf(attributeBuffs == null ? List.of() : attributeBuffs);
    }
}

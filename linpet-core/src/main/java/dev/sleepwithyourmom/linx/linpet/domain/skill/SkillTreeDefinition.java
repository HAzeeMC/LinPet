package dev.sleepwithyourmom.linx.linpet.domain.skill;

import java.util.Map;

/**
 * Skill-tree template for a single pet type.
 *
 * @param petTemplateId pet template id
 * @param skills skill definitions by id
 */
public record SkillTreeDefinition(String petTemplateId, Map<String, SkillDefinition> skills) {
    /**
     * Creates a validated skill tree definition.
     */
    public SkillTreeDefinition {
        if (petTemplateId == null || petTemplateId.isBlank()) {
            throw new IllegalArgumentException("petTemplateId must not be blank");
        }
        skills = Map.copyOf(skills == null ? Map.of() : skills);
        skills.forEach((id, skill) -> {
            if (!id.equals(skill.id())) {
                throw new IllegalArgumentException("skill map key '" + id + "' does not match skill id '" + skill.id() + "'");
            }
        });
    }
}

package dev.sleepwithyourmom.linx.linpet.domain.skill;

import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;

/**
 * Applies skill unlock rules atomically to immutable pet state.
 */
public class SkillUnlocker {
    /**
     * Resolves a node state for a pet.
     *
     * @param pet pet instance
     * @param skill skill definition
     * @return current node state
     */
    public SkillNodeState state(PetInstance pet, SkillDefinition skill) {
        if (pet == null || skill == null) {
            throw new IllegalArgumentException("pet and skill must not be null");
        }
        if (pet.unlockedSkillIds().contains(skill.id())) {
            return SkillNodeState.UNLOCKED;
        }
        if (pet.level() < skill.requiredLevel()) {
            return SkillNodeState.LOCKED;
        }
        if (pet.skillPoints() < skill.pointCost()) {
            return SkillNodeState.LOCKED;
        }
        if (!pet.unlockedSkillIds().containsAll(skill.prerequisites())) {
            return SkillNodeState.LOCKED;
        }
        return SkillNodeState.AVAILABLE;
    }

    /**
     * Unlocks a skill after validating cost, level, and prerequisites.
     *
     * @param pet pet instance
     * @param tree skill tree definition
     * @param skillId skill id
     * @return updated pet instance
     */
    public PetInstance unlock(PetInstance pet, SkillTreeDefinition tree, String skillId) {
        if (pet == null || tree == null) {
            throw new IllegalArgumentException("pet and tree must not be null");
        }
        SkillDefinition skill = tree.skills().get(skillId);
        if (skill == null) {
            throw new IllegalArgumentException("unknown skill id '" + skillId + "'");
        }
        SkillNodeState state = state(pet, skill);
        if (state == SkillNodeState.UNLOCKED) {
            throw new IllegalStateException("skill '" + skillId + "' is already unlocked");
        }
        if (state == SkillNodeState.LOCKED) {
            throw new IllegalStateException("requirements are not met for skill '" + skillId + "'");
        }
        return pet.withUnlockedSkill(skillId, pet.skillPoints() - skill.pointCost());
    }
}

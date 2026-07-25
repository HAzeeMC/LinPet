package dev.sleepwithyourmom.linx.linpet.domain.skill;

/**
 * UI-ready state of a skill node for one pet instance.
 */
public enum SkillNodeState {
    /**
     * Requirements are not satisfied.
     */
    LOCKED,

    /**
     * Requirements are satisfied and the node can be unlocked.
     */
    AVAILABLE,

    /**
     * The pet instance already has the skill.
     */
    UNLOCKED
}

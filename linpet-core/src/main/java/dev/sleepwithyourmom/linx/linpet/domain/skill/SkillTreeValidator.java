package dev.sleepwithyourmom.linx.linpet.domain.skill;

import dev.sleepwithyourmom.linx.linpet.config.ConfigValidationException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Validates skill-tree dependency graphs at load time.
 */
public class SkillTreeValidator {
    /**
     * Validates all skill trees.
     *
     * @param skillTrees trees by pet template id
     */
    public void validate(Map<String, SkillTreeDefinition> skillTrees) {
        if (skillTrees == null) {
            throw new ConfigValidationException("skillTrees must not be null");
        }
        for (SkillTreeDefinition tree : skillTrees.values()) {
            validateTree(tree);
        }
    }

    /**
     * Validates one skill tree.
     *
     * @param tree skill tree
     */
    public void validateTree(SkillTreeDefinition tree) {
        if (tree == null) {
            throw new ConfigValidationException("skill tree must not be null");
        }
        for (SkillDefinition skill : tree.skills().values()) {
            for (String prerequisite : skill.prerequisites()) {
                if (!tree.skills().containsKey(prerequisite)) {
                    throw new ConfigValidationException("skilltree.yml:" + tree.petTemplateId()
                        + "." + skill.id() + " references missing prerequisite '" + prerequisite + "'");
                }
            }
        }
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (String skillId : tree.skills().keySet()) {
            visit(tree, skillId, visiting, visited);
        }
    }

    private void visit(SkillTreeDefinition tree, String skillId, Set<String> visiting, Set<String> visited) {
        if (visited.contains(skillId)) {
            return;
        }
        if (!visiting.add(skillId)) {
            throw new ConfigValidationException("skilltree.yml:" + tree.petTemplateId()
                + " contains a circular dependency at skill '" + skillId + "'");
        }
        SkillDefinition skill = tree.skills().get(skillId);
        for (String prerequisite : skill.prerequisites()) {
            visit(tree, prerequisite, visiting, visited);
        }
        visiting.remove(skillId);
        visited.add(skillId);
    }
}

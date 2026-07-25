package dev.sleepwithyourmom.linx.linpet.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.sleepwithyourmom.linx.linpet.config.ConfigValidationException;
import dev.sleepwithyourmom.linx.linpet.domain.skill.SkillDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.skill.SkillTreeDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.skill.SkillTreeValidator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SkillTreeValidatorTest {
    @Test
    void rejectsCircularDependencies() {
        SkillDefinition a = skill("a", Set.of("b"));
        SkillDefinition b = skill("b", Set.of("a"));
        SkillTreeDefinition tree = new SkillTreeDefinition("golden_dragon", Map.of("a", a, "b", b));

        assertThrows(ConfigValidationException.class, () -> new SkillTreeValidator().validateTree(tree));
    }

    @Test
    void rejectsMissingPrerequisites() {
        SkillDefinition a = skill("a", Set.of("missing"));
        SkillTreeDefinition tree = new SkillTreeDefinition("golden_dragon", Map.of("a", a));

        assertThrows(ConfigValidationException.class, () -> new SkillTreeValidator().validateTree(tree));
    }

    private SkillDefinition skill(String id, Set<String> prerequisites) {
        return new SkillDefinition(id, "attack", id, "", 1, 1, prerequisites, List.of(), List.of());
    }
}

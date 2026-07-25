package dev.sleepwithyourmom.linx.linpet.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.sleepwithyourmom.linx.linpet.api.model.PetRarity;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;
import dev.sleepwithyourmom.linx.linpet.domain.skill.SkillDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.skill.SkillTreeDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.skill.SkillUnlocker;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SkillUnlockerTest {
    @Test
    void unlockConsumesPointsAndAddsSkill() {
        PetInstance pet = pet(Set.of(), 2, 1);
        SkillDefinition skill = new SkillDefinition("golden_claw", "attack", "Golden Claw", "", 1, 2, Set.of(), List.of(), List.of());
        SkillTreeDefinition tree = new SkillTreeDefinition("golden_dragon", Map.of("golden_claw", skill));

        PetInstance unlocked = new SkillUnlocker().unlock(pet, tree, "golden_claw");

        assertEquals(0, unlocked.skillPoints());
        assertEquals(Set.of("golden_claw"), unlocked.unlockedSkillIds());
    }

    @Test
    void rejectsMissingPrerequisiteAtUnlockTime() {
        PetInstance pet = pet(Set.of(), 5, 5);
        SkillDefinition skill = new SkillDefinition("royal_guard", "defense", "Guard", "", 1, 1, Set.of("golden_claw"), List.of(), List.of());
        SkillTreeDefinition tree = new SkillTreeDefinition("golden_dragon", Map.of("royal_guard", skill));

        assertThrows(IllegalStateException.class, () -> new SkillUnlocker().unlock(pet, tree, "royal_guard"));
    }

    private PetInstance pet(Set<String> skills, int level, int points) {
        return new PetInstance(UUID.randomUUID(), UUID.randomUUID(), "golden_dragon", level, 0.0D, points,
            skills, PetRarity.ULTRA_RARE, "MHF_Dragon", null, null);
    }
}

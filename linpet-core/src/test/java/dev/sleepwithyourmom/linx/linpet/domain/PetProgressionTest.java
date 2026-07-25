package dev.sleepwithyourmom.linx.linpet.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.sleepwithyourmom.linx.linpet.api.model.PetRarity;
import dev.sleepwithyourmom.linx.linpet.domain.buff.BuffScalingConfig;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetProgression;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PetProgressionTest {
    @Test
    void levelsUpAndAwardsSkillPoints() {
        PetInstance pet = new PetInstance(UUID.randomUUID(), UUID.randomUUID(), "golden_dragon", 1, 0.0D, 0,
            Set.of(), PetRarity.ULTRA_RARE, "MHF_Dragon", null, null);
        PetProgression progression = new PetProgression(new BuffScalingConfig(true, 0.02D, 100));

        PetInstance updated = progression.gainExperience(pet, 135.0D);

        assertEquals(2, updated.level());
        assertEquals(35.0D, updated.experience(), 0.0001D);
        assertEquals(1, updated.skillPoints());
    }
}

package dev.sleepwithyourmom.linx.linpet.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.sleepwithyourmom.linx.linpet.api.model.PetRarity;
import dev.sleepwithyourmom.linx.linpet.domain.buff.AttributeAggregateKey;
import dev.sleepwithyourmom.linx.linpet.domain.buff.AttributeBuffDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.buff.AttributeOperation;
import dev.sleepwithyourmom.linx.linpet.domain.buff.BuffCalculator;
import dev.sleepwithyourmom.linx.linpet.domain.buff.BuffScalingConfig;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuffCalculatorTest {
    @Test
    void appliesLevelScalingWithoutMutatingDefinitions() {
        PetDefinition definition = new PetDefinition(
            "golden_dragon",
            "Rồng Vàng",
            PetRarity.ULTRA_RARE,
            "MHF_Dragon",
            List.of(),
            List.of(new AttributeBuffDefinition("minecraft:attack_damage", 5.0D, AttributeOperation.ADD_NUMBER)),
            List.of()
        );
        PetInstance pet = new PetInstance(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "golden_dragon",
            11,
            0.0D,
            0,
            Set.of(),
            PetRarity.ULTRA_RARE,
            "MHF_Dragon",
            null,
            null
        );

        BuffCalculator calculator = new BuffCalculator(new BuffScalingConfig(true, 0.02D, 100));

        double amount = calculator.aggregate(Map.of(0, pet), Map.of("golden_dragon", definition), Map.of())
            .attributes()
            .get(new AttributeAggregateKey("minecraft:attack_damage", AttributeOperation.ADD_NUMBER));

        assertEquals(6.0D, amount, 0.0001D);
        assertEquals(5.0D, definition.attributeBuffs().get(0).amount(), 0.0001D);
    }
}

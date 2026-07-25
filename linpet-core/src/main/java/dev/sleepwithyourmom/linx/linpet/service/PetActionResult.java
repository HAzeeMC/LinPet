package dev.sleepwithyourmom.linx.linpet.service;

import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;
import java.util.Optional;

/**
 * Result of an equip or unequip operation.
 *
 * @param success whether the operation completed
 * @param messageKey language message key
 * @param pet affected pet
 */
public record PetActionResult(boolean success, String messageKey, PetInstance pet) {
    /**
     * Creates a successful action result.
     *
     * @param messageKey language message key
     * @param pet affected pet
     * @return result
     */
    public static PetActionResult success(String messageKey, PetInstance pet) {
        return new PetActionResult(true, messageKey, pet);
    }

    /**
     * Creates a failed action result.
     *
     * @param messageKey language message key
     * @return result
     */
    public static PetActionResult failure(String messageKey) {
        return new PetActionResult(false, messageKey, null);
    }

    /**
     * Returns the affected pet if present.
     *
     * @return optional pet
     */
    public Optional<PetInstance> petOptional() {
        return Optional.ofNullable(pet);
    }
}

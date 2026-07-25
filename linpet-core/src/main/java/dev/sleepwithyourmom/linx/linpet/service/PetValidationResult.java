package dev.sleepwithyourmom.linx.linpet.service;

import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;
import java.util.Optional;

/**
 * Result of validating an item as an equippable pet.
 *
 * @param valid whether validation passed
 * @param messageKey language key for failures
 * @param pet decoded pet when valid
 */
public record PetValidationResult(boolean valid, String messageKey, PetInstance pet) {
    /**
     * Creates a successful validation result.
     *
     * @param pet decoded pet
     * @return validation result
     */
    public static PetValidationResult success(PetInstance pet) {
        return new PetValidationResult(true, "", pet);
    }

    /**
     * Creates a failed validation result.
     *
     * @param messageKey language key
     * @return validation result
     */
    public static PetValidationResult failure(String messageKey) {
        return new PetValidationResult(false, messageKey, null);
    }

    /**
     * Returns the pet as an optional value.
     *
     * @return pet optional
     */
    public Optional<PetInstance> petOptional() {
        return Optional.ofNullable(pet);
    }
}

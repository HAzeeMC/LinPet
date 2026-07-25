package dev.sleepwithyourmom.linx.linpet.repository;

import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent repository for pet instances and equipment slots.
 */
public interface PetRepository {
    /**
     * Saves or replaces a pet instance.
     *
     * @param pet pet instance
     */
    void save(PetInstance pet);

    /**
     * Finds a pet instance by id.
     *
     * @param instanceId pet instance UUID
     * @return pet when present
     */
    Optional<PetInstance> find(UUID instanceId);

    /**
     * Deletes a pet instance and any equipment reference to it.
     *
     * @param instanceId pet instance UUID
     */
    void delete(UUID instanceId);

    /**
     * Finds all pets owned by a player.
     *
     * @param ownerId owner UUID
     * @return owned pets
     */
    List<PetInstance> findOwned(UUID ownerId);

    /**
     * Loads equipped pets for a player.
     *
     * @param ownerId owner UUID
     * @return immutable slot-to-pet map
     */
    Map<Integer, PetInstance> findEquipped(UUID ownerId);

    /**
     * Persists an equipment slot assignment.
     *
     * @param ownerId owner UUID
     * @param slot equipment slot
     * @param instanceId pet instance UUID
     */
    void equip(UUID ownerId, int slot, UUID instanceId);

    /**
     * Clears an equipment slot assignment.
     *
     * @param ownerId owner UUID
     * @param slot equipment slot
     */
    void unequip(UUID ownerId, int slot);

    /**
     * Removes every pet and equipment entry for a player.
     *
     * @param ownerId owner UUID
     */
    void wipe(UUID ownerId);
}

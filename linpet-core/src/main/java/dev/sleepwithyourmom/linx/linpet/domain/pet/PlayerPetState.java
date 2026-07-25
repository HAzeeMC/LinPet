package dev.sleepwithyourmom.linx.linpet.domain.pet;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutable online state for a player's currently equipped pets.
 */
public class PlayerPetState {
    private final UUID playerId;
    private final ConcurrentHashMap<Integer, PetInstance> equippedPets = new ConcurrentHashMap<>();

    /**
     * Creates a player pet state.
     *
     * @param playerId player UUID
     * @param equipped initial equipped slot map
     */
    public PlayerPetState(UUID playerId, Map<Integer, PetInstance> equipped) {
        if (playerId == null) {
            throw new IllegalArgumentException("playerId must not be null");
        }
        this.playerId = playerId;
        if (equipped != null) {
            equipped.forEach((slot, pet) -> {
                if (slot != null && slot >= 0 && pet != null) {
                    equippedPets.put(slot, pet);
                }
            });
        }
    }

    /**
     * Returns the player UUID.
     *
     * @return player UUID
     */
    public UUID playerId() {
        return playerId;
    }

    /**
     * Returns a snapshot of equipped pets.
     *
     * @return immutable slot-to-pet map
     */
    public Map<Integer, PetInstance> equippedPets() {
        return Map.copyOf(equippedPets);
    }

    /**
     * Reads a pet equipped in a slot.
     *
     * @param slot zero-based slot
     * @return equipped pet when present
     */
    public Optional<PetInstance> equippedPet(int slot) {
        return Optional.ofNullable(equippedPets.get(slot));
    }

    /**
     * Equips a pet into an empty slot.
     *
     * @param slot zero-based slot
     * @param pet pet to equip
     * @return true when the slot was empty and the pet was equipped
     */
    public boolean equip(int slot, PetInstance pet) {
        if (slot < 0) {
            throw new IllegalArgumentException("slot must not be negative");
        }
        if (pet == null) {
            throw new IllegalArgumentException("pet must not be null");
        }
        return equippedPets.putIfAbsent(slot, pet) == null;
    }

    /**
     * Removes a pet from a slot.
     *
     * @param slot zero-based slot
     * @return removed pet when present
     */
    public Optional<PetInstance> unequip(int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("slot must not be negative");
        }
        return Optional.ofNullable(equippedPets.remove(slot));
    }
}

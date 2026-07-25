package dev.sleepwithyourmom.linx.linpet.api.service;

import dev.sleepwithyourmom.linx.linpet.api.model.BuffView;
import dev.sleepwithyourmom.linx.linpet.api.model.PetView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public read-only API for addons that need to inspect Lin'Pet state.
 */
public interface LinPetApi {
    /**
     * Loads all known pets owned by a player.
     *
     * @param playerId player UUID
     * @return future completed with immutable pet views
     */
    CompletableFuture<List<PetView>> petsOwnedBy(UUID playerId);

    /**
     * Loads a pet instance by id.
     *
     * @param petInstanceId pet instance UUID
     * @return future completed with the pet view when it exists
     */
    CompletableFuture<Optional<PetView>> pet(UUID petInstanceId);

    /**
     * Returns the currently equipped pet in a slot from cache or persistent storage.
     *
     * @param playerId player UUID
     * @param slot zero-based equipment slot
     * @return future completed with the equipped pet when present
     */
    CompletableFuture<Optional<PetView>> equippedPet(UUID playerId, int slot);

    /**
     * Returns the currently aggregated buffs for an online player.
     *
     * @param playerId player UUID
     * @return immutable buff list; empty when the player is offline or has no equipped pets
     */
    List<BuffView> activeBuffs(UUID playerId);
}

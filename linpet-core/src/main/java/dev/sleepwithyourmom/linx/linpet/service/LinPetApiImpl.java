package dev.sleepwithyourmom.linx.linpet.service;

import dev.sleepwithyourmom.linx.linpet.api.model.BuffView;
import dev.sleepwithyourmom.linx.linpet.api.model.PetView;
import dev.sleepwithyourmom.linx.linpet.api.service.LinPetApi;
import dev.sleepwithyourmom.linx.linpet.domain.buff.AggregatedBuffs;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Default public API implementation backed by PetService.
 */
public class LinPetApiImpl implements LinPetApi {
    private final PetService petService;
    private final BuffApplier buffApplier;

    /**
     * Creates API implementation.
     *
     * @param petService pet service
     * @param buffApplier buff applier
     */
    public LinPetApiImpl(PetService petService, BuffApplier buffApplier) {
        if (petService == null || buffApplier == null) {
            throw new IllegalArgumentException("petService and buffApplier must not be null");
        }
        this.petService = petService;
        this.buffApplier = buffApplier;
    }

    @Override
    public CompletableFuture<List<PetView>> petsOwnedBy(UUID playerId) {
        CompletableFuture<List<PetInstance>> pets = petService.ownedPets(playerId);
        CompletableFuture<Map<Integer, PetInstance>> equipped = petService.equippedPets(playerId);
        return pets.thenCombine(equipped, (ownedPets, equippedPets) -> ownedPets.stream()
                .map(pet -> petService.toView(pet, equippedSlot(equippedPets, pet)))
                .toList());
    }

    @Override
    public CompletableFuture<Optional<PetView>> pet(UUID petInstanceId) {
        return petService.pet(petInstanceId)
            .thenCompose(optional -> {
                if (optional.isEmpty()) {
                    return CompletableFuture.completedFuture(Optional.empty());
                }
                PetInstance pet = optional.get();
                return petService.equippedPets(pet.ownerId())
                    .thenApply(equipped -> Optional.of(petService.toView(pet, equippedSlot(equipped, pet))));
            });
    }

    @Override
    public CompletableFuture<Optional<PetView>> equippedPet(UUID playerId, int slot) {
        return petService.equippedPet(playerId, slot)
            .thenApply(pet -> pet.map(value -> petService.toView(value, slot)));
    }

    @Override
    public List<BuffView> activeBuffs(UUID playerId) {
        return buffApplier.activeBuffs(playerId)
            .map(this::views)
            .orElse(List.of());
    }

    private int equippedSlot(Map<Integer, PetInstance> equippedPets, PetInstance pet) {
        for (Map.Entry<Integer, PetInstance> entry : equippedPets.entrySet()) {
            if (entry.getValue().instanceId().equals(pet.instanceId())) {
                return entry.getKey();
            }
        }
        return -1;
    }

    private List<BuffView> views(AggregatedBuffs buffs) {
        java.util.ArrayList<BuffView> result = new java.util.ArrayList<>();
        buffs.attributes().forEach((key, amount) -> result.add(new BuffView(key.attributeKey(), amount, key.operation().name())));
        buffs.potions().forEach((key, potion) -> result.add(new BuffView(key, potion.amplifier() + 1.0D, "POTION")));
        return List.copyOf(result);
    }
}

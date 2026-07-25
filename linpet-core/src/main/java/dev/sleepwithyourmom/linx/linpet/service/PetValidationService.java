package dev.sleepwithyourmom.linx.linpet.service;

import dev.sleepwithyourmom.linx.linpet.config.LinPetConfig;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;
import dev.sleepwithyourmom.linx.linpet.item.PetItemCodec;
import dev.sleepwithyourmom.linx.linpet.item.PetItemDecodeException;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Centralized validation for items, ownership, expiration, and world restrictions.
 */
public class PetValidationService {
    private final AtomicReference<LinPetConfig> configRef;
    private final PetItemCodec petItemCodec;
    private final RankService rankService;

    /**
     * Creates a pet validation service.
     *
     * @param configRef live config reference
     * @param petItemCodec item codec
     * @param rankService rank service
     */
    public PetValidationService(AtomicReference<LinPetConfig> configRef, PetItemCodec petItemCodec, RankService rankService) {
        if (configRef == null || petItemCodec == null || rankService == null) {
            throw new IllegalArgumentException("validation dependencies must not be null");
        }
        this.configRef = configRef;
        this.petItemCodec = petItemCodec;
        this.rankService = rankService;
    }

    /**
     * Validates an item for equipping by a player.
     *
     * @param player player attempting equip
     * @param item item to validate
     * @return validation result
     */
    public PetValidationResult validateForEquip(Player player, ItemStack item) {
        if (player == null) {
            throw new IllegalArgumentException("player must not be null");
        }
        Optional<PetInstance> decoded;
        try {
            decoded = petItemCodec.decode(item);
        } catch (PetItemDecodeException ex) {
            return PetValidationResult.failure("pet.invalid-checksum");
        }
        if (decoded.isEmpty()) {
            return PetValidationResult.failure("pet.invalid-item");
        }
        PetInstance pet = decoded.get();
        if (!pet.ownerId().equals(player.getUniqueId())) {
            return PetValidationResult.failure("pet.not-owner");
        }
        LinPetConfig config = configRef.get();
        PetDefinition definition = config.pets().get(pet.templateId());
        if (definition == null) {
            return PetValidationResult.failure("pet.unknown-template");
        }
        if (pet.isExpired(Instant.now())) {
            return PetValidationResult.failure("pet.expired");
        }
        if (!rankService.bypassesBlacklist(player)
            && config.settings().blacklistWorlds().contains(player.getWorld().getName())) {
            return PetValidationResult.failure("pet.blacklisted-world");
        }
        return PetValidationResult.success(pet);
    }

    /**
     * Returns true when an item is a signed Lin'Pet pet item.
     *
     * @param item item stack
     * @return true for pet items
     */
    public boolean isPetItem(ItemStack item) {
        return petItemCodec.isPetItem(item);
    }
}

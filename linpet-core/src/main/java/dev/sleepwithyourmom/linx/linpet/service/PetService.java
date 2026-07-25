package dev.sleepwithyourmom.linx.linpet.service;

import dev.sleepwithyourmom.linx.linpet.api.event.PetEquipEvent;
import dev.sleepwithyourmom.linx.linpet.api.event.PetUnequipEvent;
import dev.sleepwithyourmom.linx.linpet.api.model.PetRarity;
import dev.sleepwithyourmom.linx.linpet.api.model.PetView;
import dev.sleepwithyourmom.linx.linpet.cache.PetStateCache;
import dev.sleepwithyourmom.linx.linpet.config.LinPetConfig;
import dev.sleepwithyourmom.linx.linpet.core.SchedulerAdapter;
import dev.sleepwithyourmom.linx.linpet.domain.buff.AggregatedBuffs;
import dev.sleepwithyourmom.linx.linpet.domain.buff.BuffCalculator;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PlayerPetState;
import dev.sleepwithyourmom.linx.linpet.item.PetItemCodec;
import dev.sleepwithyourmom.linx.linpet.repository.AuditLogRepository;
import dev.sleepwithyourmom.linx.linpet.repository.PetRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Application service for pet lifecycle, equipment, cache, and buff refresh.
 */
public class PetService {
    private final JavaPlugin plugin;
    private final AtomicReference<LinPetConfig> configRef;
    private final PetRepository petRepository;
    private final AuditLogRepository auditLogRepository;
    private final SchedulerAdapter scheduler;
    private final PetStateCache stateCache;
    private final PetValidationService validationService;
    private final RankService rankService;
    private final PetItemCodec itemCodec;
    private final BuffApplier buffApplier;
    private volatile BuffCalculator buffCalculator;

    /**
     * Creates a pet service.
     *
     * @param plugin owning plugin
     * @param configRef live config reference
     * @param petRepository pet repository
     * @param auditLogRepository audit log repository
     * @param scheduler scheduler adapter
     * @param stateCache state cache
     * @param validationService validation service
     * @param rankService rank service
     * @param itemCodec item codec
     * @param buffApplier buff applier
     */
    public PetService(
        JavaPlugin plugin,
        AtomicReference<LinPetConfig> configRef,
        PetRepository petRepository,
        AuditLogRepository auditLogRepository,
        SchedulerAdapter scheduler,
        PetStateCache stateCache,
        PetValidationService validationService,
        RankService rankService,
        PetItemCodec itemCodec,
        BuffApplier buffApplier
    ) {
        if (plugin == null || configRef == null || petRepository == null || auditLogRepository == null
            || scheduler == null || stateCache == null || validationService == null || rankService == null
            || itemCodec == null || buffApplier == null) {
            throw new IllegalArgumentException("pet service dependencies must not be null");
        }
        this.plugin = plugin;
        this.configRef = configRef;
        this.petRepository = petRepository;
        this.auditLogRepository = auditLogRepository;
        this.scheduler = scheduler;
        this.stateCache = stateCache;
        this.validationService = validationService;
        this.rankService = rankService;
        this.itemCodec = itemCodec;
        this.buffApplier = buffApplier;
        this.buffCalculator = new BuffCalculator(configRef.get().settings().scaling());
    }

    /**
     * Updates config-dependent calculators after a successful reload.
     */
    public void refreshConfig() {
        this.buffCalculator = new BuffCalculator(configRef.get().settings().scaling());
        Bukkit.getOnlinePlayers().forEach(this::reapplyBuffs);
    }

    /**
     * Loads player equipment from the database into cache.
     *
     * @param player player joining or being initialized
     */
    public void loadPlayer(Player player) {
        supplyAsync(() -> petRepository.findEquipped(player.getUniqueId()))
            .whenComplete((equipped, throwable) -> scheduler.runOnEntity(player, () -> {
                if (throwable != null) {
                    plugin.getLogger().log(Level.SEVERE, "Failed loading LinPet state for " + player.getName(), throwable);
                    stateCache.put(new PlayerPetState(player.getUniqueId(), Map.of()));
                    return;
                }
                stateCache.put(new PlayerPetState(player.getUniqueId(), equipped));
                reapplyBuffs(player);
            }));
    }

    /**
     * Clears online cache and modifiers for a player.
     *
     * @param player player leaving
     */
    public void unloadPlayer(Player player) {
        buffApplier.remove(player);
        stateCache.invalidate(player.getUniqueId());
    }

    /**
     * Gives a newly generated pet to a player.
     *
     * @param actor actor UUID, or {@code null} for console
     * @param target target player
     * @param templateId pet template id
     * @param level initial level
     * @return future completed with the created pet
     */
    public CompletableFuture<PetInstance> givePet(UUID actor, Player target, String templateId, int level) {
        if (target == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("target must not be null"));
        }
        LinPetConfig config = configRef.get();
        PetDefinition definition = config.pets().get(templateId);
        if (definition == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("unknown pet template '" + templateId + "'"));
        }
        if (level < 1 || level > config.settings().scaling().maxLevel()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                "level must be between 1 and " + config.settings().scaling().maxLevel()));
        }

        CompletableFuture<PetInstance> result = new CompletableFuture<>();
        scheduler.runOnEntity(target, () -> {
            if (target.getInventory().firstEmpty() < 0) {
                result.completeExceptionally(new InventoryDeliveryException("target inventory is full"));
                return;
            }
            PetInstance pet = createPetInstance(target.getUniqueId(), definition, level);
            runAsync(() -> {
                petRepository.save(pet);
                auditLogRepository.record(actor, "PET_GIVE", target.getUniqueId().toString(),
                    "template=" + templateId + ",level=" + level + ",instance=" + pet.instanceId());
                return pet;
            }).whenComplete((created, throwable) -> {
                if (throwable != null) {
                    plugin.getLogger().log(Level.SEVERE, "Failed creating pet " + templateId + " for " + target.getName(), throwable);
                    result.completeExceptionally(throwable);
                    return;
                }
                scheduler.runOnEntity(target, () -> deliverNewPetItem(target, definition, created, result));
            });
        });
        return result;
    }

    CompletableFuture<PetInstance> createStoredPet(UUID actor, UUID ownerId, String templateId, int level) {
        return createStoredPet(actor, ownerId, templateId, level, UUID.randomUUID());
    }

    CompletableFuture<PetInstance> createStoredPet(UUID actor, UUID ownerId, String templateId, int level, UUID instanceId) {
        if (ownerId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ownerId must not be null"));
        }
        if (instanceId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("instanceId must not be null"));
        }
        LinPetConfig config = configRef.get();
        PetDefinition definition = config.pets().get(templateId);
        if (definition == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("unknown pet template '" + templateId + "'"));
        }
        if (level < 1 || level > config.settings().scaling().maxLevel()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                "level must be between 1 and " + config.settings().scaling().maxLevel()));
        }
        PetInstance pet = createPetInstance(ownerId, definition, level, instanceId);
        return runAsync(() -> {
            petRepository.save(pet);
            auditLogRepository.record(actor, "PET_CREATE", ownerId.toString(),
                "template=" + templateId + ",level=" + level + ",instance=" + pet.instanceId());
            return pet;
        });
    }

    boolean deliverStoredPetItem(Player target, PetInstance pet) {
        if (target == null || pet == null || !target.getUniqueId().equals(pet.ownerId())) {
            throw new IllegalArgumentException("target and pet owner must match");
        }
        PetDefinition definition = configRef.get().pets().get(pet.templateId());
        if (definition == null) {
            plugin.getLogger().warning("Cannot deliver stored pet item " + pet.instanceId()
                + " because template " + pet.templateId() + " is no longer configured");
            return false;
        }
        Map<Integer, ItemStack> rejected = target.getInventory().addItem(itemCodec.encode(pet, definition));
        if (!rejected.isEmpty()) {
            plugin.getLogger().warning("Inventory full while delivering stored pet " + pet.instanceId()
                + " to " + target.getName() + "; database ownership is preserved");
            return false;
        }
        return true;
    }

    private PetInstance createPetInstance(UUID ownerId, PetDefinition definition, int level) {
        return createPetInstance(ownerId, definition, level, UUID.randomUUID());
    }

    private PetInstance createPetInstance(UUID ownerId, PetDefinition definition, int level, UUID instanceId) {
        PetInstance pet = new PetInstance(
            instanceId,
            ownerId,
            definition.id(),
            level,
            0.0D,
            Math.max(0, level - 1),
            java.util.Set.of(),
            definition.rarity(),
            definition.defaultSkin(),
            null,
            null
        );
        return pet;
    }

    private void deliverNewPetItem(
        Player target,
        PetDefinition definition,
        PetInstance pet,
        CompletableFuture<PetInstance> result
    ) {
        ItemStack item = itemCodec.encode(pet, definition);
        Map<Integer, ItemStack> rejected = target.getInventory().addItem(item);
        if (rejected.isEmpty()) {
            result.complete(pet);
            return;
        }
        runAsync(() -> {
            petRepository.delete(pet.instanceId());
            return null;
        }).whenComplete((ignored, rollbackThrowable) -> {
            if (rollbackThrowable != null) {
                plugin.getLogger().log(Level.SEVERE, "Failed rolling back undelivered pet " + pet.instanceId(), rollbackThrowable);
            }
            result.completeExceptionally(new InventoryDeliveryException("target inventory is full"));
        });
    }

    /**
     * Equips the item in a target slot if validation passes.
     *
     * @param player player equipping
     * @param item source item
     * @param slot equipment slot
     * @return action result
     */
    public PetActionResult equip(Player player, ItemStack item, int slot) {
        if (slot < 0 || slot >= rankService.slotLimit(player)) {
            return PetActionResult.failure("equipment.slot-locked");
        }
        PetValidationResult validation = validationService.validateForEquip(player, item);
        if (!validation.valid()) {
            return PetActionResult.failure(validation.messageKey());
        }
        PetInstance pet = validation.pet();
        PlayerPetState state = stateCache.getOrCreate(player.getUniqueId());
        boolean alreadyEquipped = state.equippedPets().values().stream()
            .anyMatch(equipped -> equipped.instanceId().equals(pet.instanceId()));
        if (alreadyEquipped) {
            return PetActionResult.failure("equipment.already-equipped");
        }
        if (state.equippedPet(slot).isPresent()) {
            return PetActionResult.failure("equipment.slot-occupied");
        }
        PetEquipEvent event = new PetEquipEvent(player, pet.instanceId(), slot, item);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return PetActionResult.failure("equipment.cancelled");
        }
        if (!state.equip(slot, pet)) {
            return PetActionResult.failure("equipment.slot-occupied");
        }
        persistEquip(player.getUniqueId(), slot, pet);
        reapplyBuffs(player);
        return PetActionResult.success("equipment.equipped", pet);
    }

    /**
     * Equips a pet from the first matching inventory item.
     *
     * @param player player equipping
     * @param instanceId pet instance id
     * @return action result
     */
    public PetActionResult equipFromInventory(Player player, UUID instanceId) {
        int freeSlot = firstFreeSlot(player);
        if (freeSlot < 0) {
            return PetActionResult.failure("equipment.no-free-slot");
        }
        for (int index = 0; index < player.getInventory().getSize(); index++) {
            ItemStack item = player.getInventory().getItem(index);
            Optional<PetInstance> decoded;
            try {
                decoded = itemCodec.decode(item);
            } catch (RuntimeException ex) {
                continue;
            }
            if (decoded.isPresent() && decoded.get().instanceId().equals(instanceId)) {
                PetActionResult result = equip(player, item, freeSlot);
                if (result.success()) {
                    player.getInventory().setItem(index, null);
                }
                return result;
            }
        }
        return PetActionResult.failure("pet.not-found");
    }

    /**
     * Unequips a pet from a slot and returns it to the player's inventory.
     *
     * @param player player
     * @param slot equipment slot
     * @return action result
     */
    public PetActionResult unequip(Player player, int slot) {
        if (player.getInventory().firstEmpty() < 0) {
            return PetActionResult.failure("equipment.inventory-full");
        }
        PlayerPetState state = stateCache.getOrCreate(player.getUniqueId());
        Optional<PetInstance> removed = state.unequip(slot);
        if (removed.isEmpty()) {
            return PetActionResult.failure("equipment.empty-slot");
        }
        PetInstance pet = removed.get();
        PetDefinition definition = configRef.get().pets().get(pet.templateId());
        if (definition == null) {
            state.equip(slot, pet);
            return PetActionResult.failure("pet.unknown-template");
        }
        ItemStack item = itemCodec.encode(pet, definition);
        PetUnequipEvent event = new PetUnequipEvent(player, pet.instanceId(), slot, item);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            state.equip(slot, pet);
            return PetActionResult.failure("equipment.cancelled");
        }
        player.getInventory().addItem(item);
        persistUnequip(player.getUniqueId(), slot);
        reapplyBuffs(player);
        return PetActionResult.success("equipment.unequipped", pet);
    }

    /**
     * Reapplies buffs according to current world and equipped state.
     *
     * @param player online player
     */
    public void reapplyBuffs(Player player) {
        LinPetConfig config = configRef.get();
        if (!rankService.bypassesBlacklist(player) && config.settings().blacklistWorlds().contains(player.getWorld().getName())) {
            buffApplier.remove(player);
            return;
        }
        PlayerPetState state = stateCache.getOrCreate(player.getUniqueId());
        AggregatedBuffs aggregate = buffCalculator.aggregate(state.equippedPets(), config.pets(), config.skillTrees());
        buffApplier.apply(player, aggregate);
    }

    /**
     * Returns cached equipment for a player.
     *
     * @param playerId player UUID
     * @return slot-to-pet map
     */
    public Map<Integer, PetInstance> equippedSnapshot(UUID playerId) {
        return stateCache.get(playerId)
            .map(PlayerPetState::equippedPets)
            .orElse(Map.of());
    }

    /**
     * Returns equipment from online cache, or loads it from persistent storage when absent.
     *
     * @param playerId player UUID
     * @return future of slot-to-pet map
     */
    public CompletableFuture<Map<Integer, PetInstance>> equippedPets(UUID playerId) {
        if (playerId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("playerId must not be null"));
        }
        Optional<PlayerPetState> cached = stateCache.get(playerId);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get().equippedPets());
        }
        return runAsync(() -> petRepository.findEquipped(playerId));
    }

    /**
     * Returns one equipped pet from online cache, or persistent storage when absent.
     *
     * @param playerId player UUID
     * @param slot equipment slot
     * @return future of equipped pet when present
     */
    public CompletableFuture<Optional<PetInstance>> equippedPet(UUID playerId, int slot) {
        if (slot < 0) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return equippedPets(playerId).thenApply(equipped -> Optional.ofNullable(equipped.get(slot)));
    }

    /**
     * Loads pets owned by a player asynchronously.
     *
     * @param playerId player UUID
     * @return future of owned pets
     */
    public CompletableFuture<List<PetInstance>> ownedPets(UUID playerId) {
        return runAsync(() -> petRepository.findOwned(playerId));
    }

    /**
     * Loads a pet by id asynchronously.
     *
     * @param instanceId pet instance UUID
     * @return future of pet when present
     */
    public CompletableFuture<Optional<PetInstance>> pet(UUID instanceId) {
        return runAsync(() -> petRepository.find(instanceId));
    }

    /**
     * Builds a public API pet view.
     *
     * @param pet pet instance
     * @param equippedSlot equipped slot, or {@code -1}
     * @return API view
     */
    public PetView toView(PetInstance pet, int equippedSlot) {
        PetDefinition definition = configRef.get().pets().get(pet.templateId());
        String displayName = definition == null ? pet.templateId() : pet.displayName(definition);
        return new PetView(
            pet.instanceId(),
            pet.ownerId(),
            pet.templateId(),
            displayName,
            pet.rarity(),
            pet.level(),
            pet.experience(),
            pet.skillPoints(),
            pet.unlockedSkillIds(),
            equippedSlot >= 0,
            equippedSlot,
            pet.expiresAt()
        );
    }

    /**
     * Finds a pet definition.
     *
     * @param templateId pet template id
     * @return definition when present
     */
    public Optional<PetDefinition> definition(String templateId) {
        return Optional.ofNullable(configRef.get().pets().get(templateId));
    }

    /**
     * Returns the current item codec.
     *
     * @return item codec
     */
    public PetItemCodec itemCodec() {
        return itemCodec;
    }

    /**
     * Returns the first free active equipment slot.
     *
     * @param player player
     * @return slot index or {@code -1}
     */
    public int firstFreeSlot(Player player) {
        PlayerPetState state = stateCache.getOrCreate(player.getUniqueId());
        int limit = rankService.slotLimit(player);
        for (int slot = 0; slot < limit; slot++) {
            if (state.equippedPet(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private void persistEquip(UUID playerId, int slot, PetInstance pet) {
        scheduler.runAsync(() -> {
            try {
                petRepository.equip(playerId, slot, pet.instanceId());
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed persisting equipped pet " + pet.instanceId(), ex);
            }
        });
    }

    private void persistUnequip(UUID playerId, int slot) {
        scheduler.runAsync(() -> {
            try {
                petRepository.unequip(playerId, slot);
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed persisting unequip for " + playerId + " slot " + slot, ex);
            }
        });
    }

    private <T> CompletableFuture<T> runAsync(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        scheduler.runAsync(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    private <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return runAsync(supplier);
    }
}

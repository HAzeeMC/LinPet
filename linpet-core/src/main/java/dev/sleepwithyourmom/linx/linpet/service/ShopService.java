package dev.sleepwithyourmom.linx.linpet.service;

import dev.sleepwithyourmom.linx.linpet.config.LinPetConfig;
import dev.sleepwithyourmom.linx.linpet.core.SchedulerAdapter;
import dev.sleepwithyourmom.linx.linpet.domain.shop.ShopEntry;
import dev.sleepwithyourmom.linx.linpet.economy.EconomyProvider;
import dev.sleepwithyourmom.linx.linpet.economy.EconomyResult;
import dev.sleepwithyourmom.linx.linpet.repository.ShopPurchaseRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Application service for atomic shop purchase workflow.
 */
public class ShopService {
    private final JavaPlugin plugin;
    private final AtomicReference<LinPetConfig> configRef;
    private final EconomyProvider economyProvider;
    private final ShopPurchaseRepository purchaseRepository;
    private final PetService petService;
    private final SchedulerAdapter scheduler;

    /**
     * Creates a shop service.
     *
     * @param plugin owning plugin
     * @param configRef live config reference
     * @param economyProvider economy provider
     * @param purchaseRepository purchase counter repository
     * @param petService pet service
     * @param scheduler scheduler adapter
     */
    public ShopService(
        JavaPlugin plugin,
        AtomicReference<LinPetConfig> configRef,
        EconomyProvider economyProvider,
        ShopPurchaseRepository purchaseRepository,
        PetService petService,
        SchedulerAdapter scheduler
    ) {
        if (plugin == null || configRef == null || economyProvider == null || purchaseRepository == null
            || petService == null || scheduler == null) {
            throw new IllegalArgumentException("shop service dependencies must not be null");
        }
        this.plugin = plugin;
        this.configRef = configRef;
        this.economyProvider = economyProvider;
        this.purchaseRepository = purchaseRepository;
        this.petService = petService;
        this.scheduler = scheduler;
    }

    /**
     * Purchases a configured shop pet for a player.
     *
     * @param player purchaser
     * @param petTemplateId pet template id
     * @return future completed with purchase result
     */
    public CompletableFuture<ShopPurchaseResult> purchase(Player player, String petTemplateId) {
        Optional<ShopEntry> entry = Optional.ofNullable(configRef.get().shopEntries().get(petTemplateId));
        if (entry.isEmpty()) {
            return CompletableFuture.completedFuture(ShopPurchaseResult.failure("shop.not-for-sale"));
        }
        if (!economyProvider.available()) {
            return CompletableFuture.completedFuture(ShopPurchaseResult.failure("shop.no-economy"));
        }
        LocalDate day = LocalDate.now(ZoneOffset.UTC);
        ShopEntry shopEntry = entry.get();
        return runAsync(() -> purchaseRepository.reserve(player.getUniqueId(), petTemplateId, day, shopEntry.dailyLimit()))
            .thenCompose(reserved -> {
                if (!reserved) {
                    return CompletableFuture.completedFuture(ShopPurchaseResult.failure("shop.daily-limit"));
                }
                CompletableFuture<ShopPurchaseResult> result = new CompletableFuture<>();
                scheduler.runOnEntity(player, () -> {
                    if (!economyProvider.has(player, shopEntry.price())) {
                        releaseReservation(player.getUniqueId(), petTemplateId, day);
                        result.complete(ShopPurchaseResult.failure("shop.not-enough-money"));
                        return;
                    }
                    EconomyResult withdraw = economyProvider.withdraw(player, shopEntry.price());
                    if (!withdraw.success()) {
                        releaseReservation(player.getUniqueId(), petTemplateId, day);
                        result.complete(ShopPurchaseResult.failure("shop.withdraw-failed"));
                        return;
                    }
                    petService.givePet(player.getUniqueId(), player, petTemplateId, 1)
                        .whenComplete((pet, throwable) -> {
                            if (throwable != null) {
                                scheduler.runOnEntity(player, () -> economyProvider.deposit(player, shopEntry.price()));
                                releaseReservation(player.getUniqueId(), petTemplateId, day);
                                result.complete(ShopPurchaseResult.failure("shop.give-failed"));
                                return;
                            }
                            result.complete(ShopPurchaseResult.success(pet.instanceId()));
                        });
                });
                return result;
            });
    }

    private void releaseReservation(UUID playerId, String petTemplateId, LocalDate day) {
        scheduler.runAsync(() -> {
            try {
                purchaseRepository.release(playerId, petTemplateId, day);
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed releasing shop reservation for " + playerId + " pet " + petTemplateId, ex);
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
}

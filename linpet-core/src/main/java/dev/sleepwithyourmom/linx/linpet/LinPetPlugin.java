package dev.sleepwithyourmom.linx.linpet;

import dev.sleepwithyourmom.linx.linpet.api.service.LinPetApi;
import dev.sleepwithyourmom.linx.linpet.api.service.LinPetProvider;
import dev.sleepwithyourmom.linx.linpet.cache.PetStateCache;
import dev.sleepwithyourmom.linx.linpet.commands.LinPetCommand;
import dev.sleepwithyourmom.linx.linpet.config.LinPetConfig;
import dev.sleepwithyourmom.linx.linpet.config.YamlLinPetConfigLoader;
import dev.sleepwithyourmom.linx.linpet.core.SchedulerAdapter;
import dev.sleepwithyourmom.linx.linpet.core.SchedulerAdapterFactory;
import dev.sleepwithyourmom.linx.linpet.core.TaskHandle;
import dev.sleepwithyourmom.linx.linpet.economy.EconomyProvider;
import dev.sleepwithyourmom.linx.linpet.economy.EconomyResolver;
import dev.sleepwithyourmom.linx.linpet.gui.MenuFactory;
import dev.sleepwithyourmom.linx.linpet.item.ItemIntegrityService;
import dev.sleepwithyourmom.linx.linpet.item.LinPetKeys;
import dev.sleepwithyourmom.linx.linpet.item.PetItemCodec;
import dev.sleepwithyourmom.linx.linpet.item.SkinResolver;
import dev.sleepwithyourmom.linx.linpet.listener.GuiListener;
import dev.sleepwithyourmom.linx.linpet.listener.CustomAttributeListener;
import dev.sleepwithyourmom.linx.linpet.listener.PlayerLifecycleListener;
import dev.sleepwithyourmom.linx.linpet.repository.DatabaseManager;
import dev.sleepwithyourmom.linx.linpet.repository.JdbcAuditLogRepository;
import dev.sleepwithyourmom.linx.linpet.repository.JdbcAuctionRepository;
import dev.sleepwithyourmom.linx.linpet.repository.JdbcPetRepository;
import dev.sleepwithyourmom.linx.linpet.repository.JdbcShopPurchaseRepository;
import dev.sleepwithyourmom.linx.linpet.repository.MigrationRunner;
import dev.sleepwithyourmom.linx.linpet.service.AuctionService;
import dev.sleepwithyourmom.linx.linpet.service.BuffApplier;
import dev.sleepwithyourmom.linx.linpet.service.LinPetApiImpl;
import dev.sleepwithyourmom.linx.linpet.service.MessageService;
import dev.sleepwithyourmom.linx.linpet.service.PetService;
import dev.sleepwithyourmom.linx.linpet.service.PetValidationService;
import dev.sleepwithyourmom.linx.linpet.service.RankService;
import dev.sleepwithyourmom.linx.linpet.service.ShopService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main Lin'Pet plugin bootstrap.
 */
public class LinPetPlugin extends JavaPlugin {
    private final AtomicReference<LinPetConfig> configRef = new AtomicReference<>();

    private YamlLinPetConfigLoader configLoader;
    private DatabaseManager databaseManager;
    private SchedulerAdapter scheduler;
    private PetService petService;
    private MessageService messageService;
    private SkinResolver skinResolver;
    private BuffApplier buffApplier;
    private AuctionService auctionService;
    private ShopService shopService;
    private LinPetApi api;
    private TaskHandle customEffectTask;
    private TaskHandle refreshTask;
    private TaskHandle auctionSettlementTask;

    @Override
    public void onEnable() {
        try {
            bootstrap();
        } catch (RuntimeException ex) {
            getLogger().severe("LinPet failed to enable: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (customEffectTask != null) {
            customEffectTask.cancel();
        }
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        if (auctionSettlementTask != null) {
            auctionSettlementTask.cancel();
        }
        if (buffApplier != null) {
            Bukkit.getOnlinePlayers().forEach(buffApplier::remove);
        }
        if (api != null) {
            LinPetProvider.unregister(api);
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    private void bootstrap() {
        saveDefaultConfig();
        configLoader = new YamlLinPetConfigLoader(this);
        LinPetConfig loadedConfig = configLoader.load();
        configRef.set(loadedConfig);

        messageService = new MessageService(this);
        messageService.reload();
        scheduler = SchedulerAdapterFactory.create(this, loadedConfig.settings().enableFolia());

        databaseManager = new DatabaseManager(this, loadedConfig.settings().database());
        databaseManager.start();
        new MigrationRunner(this, databaseManager).migrate();

        LinPetKeys keys = new LinPetKeys(this);
        ItemIntegrityService integrityService = ItemIntegrityService.load(this);
        skinResolver = new SkinResolver(loadedConfig.skinRegistry());
        PetItemCodec itemCodec = new PetItemCodec(keys, integrityService, skinResolver);

        RankService rankService = new RankService(configRef);
        buffApplier = new BuffApplier(this);
        PetValidationService validationService = new PetValidationService(configRef, itemCodec, rankService);
        JdbcAuditLogRepository auditLogRepository = new JdbcAuditLogRepository(databaseManager);
        petService = new PetService(
            this,
            configRef,
            new JdbcPetRepository(databaseManager),
            auditLogRepository,
            scheduler,
            new PetStateCache(Duration.ofMinutes(30), 10_000L),
            validationService,
            rankService,
            itemCodec,
            buffApplier
        );
        EconomyProvider economyProvider = new EconomyResolver(this).resolve();
        auctionService = new AuctionService(
            this,
            configRef,
            new JdbcAuctionRepository(databaseManager),
            auditLogRepository,
            economyProvider,
            petService,
            scheduler
        );
        shopService = new ShopService(
            this,
            configRef,
            economyProvider,
            new JdbcShopPurchaseRepository(databaseManager),
            petService,
            scheduler
        );
        MenuFactory menuFactory = new MenuFactory(petService, rankService, messageService, itemCodec);
        registerListeners();
        registerCommand(menuFactory);
        startBuffTasks();
        api = new LinPetApiImpl(petService, buffApplier);
        LinPetProvider.register(api);

        Bukkit.getOnlinePlayers().forEach(petService::loadPlayer);
        getLogger().info("LinPet enabled with " + loadedConfig.pets().size() + " pet definitions.");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(petService), this);
        getServer().getPluginManager().registerEvents(new CustomAttributeListener(buffApplier), this);
    }

    private void registerCommand(MenuFactory menuFactory) {
        LinPetCommand command = new LinPetCommand(
            configRef,
            petService,
            menuFactory,
            messageService,
            scheduler,
            shopService,
            auctionService,
            this::reloadRuntimeConfig
        );
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
            event.registrar().register("linpet", "Lin'Pet main command", List.of("lp", "pet"), command));
    }

    private void startBuffTasks() {
        customEffectTask = scheduler.runAsyncTimer(() ->
            Bukkit.getOnlinePlayers().forEach(player ->
                scheduler.runOnEntity(player, () -> buffApplier.tickCustomEffects(player))), 20L, 20L);
        refreshTask = scheduler.runAsyncTimer(() ->
            Bukkit.getOnlinePlayers().forEach(player ->
                scheduler.runOnEntity(player, () -> petService.reapplyBuffs(player))), 20L * 30L, 20L * 30L);
        auctionSettlementTask = scheduler.runAsyncTimer(() ->
            auctionService.settleExpiredAuctions().whenComplete((settled, throwable) -> {
                if (throwable != null) {
                    getLogger().severe("Failed running auction settlement task: " + throwable.getMessage());
                } else if (settled > 0) {
                    getLogger().info("Settled " + settled + " LinPet auction(s).");
                }
            }), 20L * 15L, 20L * 30L);
    }

    private void reloadRuntimeConfig() {
        LinPetConfig reloaded = configLoader.load();
        configRef.set(reloaded);
        skinResolver.setSkinRegistry(reloaded.skinRegistry());
        messageService.reload();
        petService.refreshConfig();
    }
}

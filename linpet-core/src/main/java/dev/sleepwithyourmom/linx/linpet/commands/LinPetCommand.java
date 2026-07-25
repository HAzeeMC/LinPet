package dev.sleepwithyourmom.linx.linpet.commands;

import dev.sleepwithyourmom.linx.linpet.config.LinPetConfig;
import dev.sleepwithyourmom.linx.linpet.core.SchedulerAdapter;
import dev.sleepwithyourmom.linx.linpet.domain.auction.AuctionListing;
import dev.sleepwithyourmom.linx.linpet.gui.MenuFactory;
import dev.sleepwithyourmom.linx.linpet.service.AuctionBidResult;
import dev.sleepwithyourmom.linx.linpet.service.AuctionService;
import dev.sleepwithyourmom.linx.linpet.service.MessageService;
import dev.sleepwithyourmom.linx.linpet.service.PetActionResult;
import dev.sleepwithyourmom.linx.linpet.service.PetService;
import dev.sleepwithyourmom.linx.linpet.service.ShopPurchaseResult;
import dev.sleepwithyourmom.linx.linpet.service.ShopService;
import java.math.BigDecimal;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Paper command entrypoint for {@code /linpet}.
 */
public class LinPetCommand implements BasicCommand {
    private final AtomicReference<LinPetConfig> configRef;
    private final PetService petService;
    private final MenuFactory menuFactory;
    private final MessageService messages;
    private final SchedulerAdapter scheduler;
    private final ShopService shopService;
    private final AuctionService auctionService;
    private final Runnable reloadAction;

    /**
     * Creates command handler.
     *
     * @param configRef live config reference
     * @param petService pet service
     * @param menuFactory menu factory
     * @param messages message service
     * @param scheduler scheduler adapter
     * @param shopService shop service
     * @param auctionService auction service
     * @param reloadAction reload action
     */
    public LinPetCommand(
        AtomicReference<LinPetConfig> configRef,
        PetService petService,
        MenuFactory menuFactory,
        MessageService messages,
        SchedulerAdapter scheduler,
        ShopService shopService,
        AuctionService auctionService,
        Runnable reloadAction
    ) {
        if (configRef == null || petService == null || menuFactory == null || messages == null || scheduler == null
            || shopService == null || auctionService == null || reloadAction == null) {
            throw new IllegalArgumentException("command dependencies must not be null");
        }
        this.configRef = configRef;
        this.petService = petService;
        this.menuFactory = menuFactory;
        this.messages = messages;
        this.scheduler = scheduler;
        this.shopService = shopService;
        this.auctionService = auctionService;
        this.reloadAction = reloadAction;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            requirePlayer(sender).ifPresent(menuFactory::openMain);
            return;
        }
        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "list", "gui" -> requirePlayer(sender).ifPresent(menuFactory::openMain);
            case "equip" -> equip(sender, args);
            case "unequip" -> unequip(sender, args);
            case "shop" -> shop(sender, args);
            case "auction" -> auction(sender, args);
            case "admin" -> admin(sender, args);
            default -> messages.send(sender, "command.usage");
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 0 || args.length == 1) {
            return filter(List.of("list", "equip", "unequip", "shop", "auction", "admin"), args.length == 0 ? "" : args[0]);
        }
        if ("shop".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return filter(List.of("buy"), args[1]);
            }
            if (args.length == 3 && "buy".equalsIgnoreCase(args[1])) {
                return filter(new ArrayList<>(configRef.get().shopEntries().keySet()), args[2]);
            }
        }
        if ("auction".equalsIgnoreCase(args[0]) && args.length == 2) {
            return filter(List.of("bid"), args[1]);
        }
        if ("admin".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return filter(List.of("give", "reload", "view", "edit", "auction"), args[1]);
            }
            if (args.length == 3 && List.of("give", "view", "edit").contains(args[1].toLowerCase(Locale.ROOT))) {
                return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
            }
            if (args.length == 4 && "give".equalsIgnoreCase(args[1])) {
                return filter(new ArrayList<>(configRef.get().pets().keySet()), args[3]);
            }
            if (args.length == 3 && "auction".equalsIgnoreCase(args[1])) {
                return filter(List.of("start", "stop", "extend"), args[2]);
            }
            if (args.length == 4 && "auction".equalsIgnoreCase(args[1]) && "start".equalsIgnoreCase(args[2])) {
                return filter(new ArrayList<>(configRef.get().pets().keySet()), args[3]);
            }
        }
        return List.of();
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return sender.hasPermission("linpet.user") || sender.hasPermission("linpet.admin");
    }

    @Override
    public String permission() {
        return "linpet.user";
    }

    private void equip(CommandSender sender, String[] args) {
        if (!sender.hasPermission("linpet.user.equip")) {
            messages.send(sender, "command.no-permission");
            return;
        }
        Player player = requirePlayer(sender).orElse(null);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            messages.send(sender, "command.usage-equip");
            return;
        }
        try {
            PetActionResult result = petService.equipFromInventory(player, UUID.fromString(args[1]));
            messages.send(sender, result.messageKey());
        } catch (IllegalArgumentException ex) {
            messages.send(sender, "command.invalid-uuid");
        }
    }

    private void unequip(CommandSender sender, String[] args) {
        if (!sender.hasPermission("linpet.user.unequip")) {
            messages.send(sender, "command.no-permission");
            return;
        }
        Player player = requirePlayer(sender).orElse(null);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            messages.send(sender, "command.usage-unequip");
            return;
        }
        try {
            int slot = Integer.parseInt(args[1]);
            PetActionResult result = petService.unequip(player, slot);
            messages.send(sender, result.messageKey());
        } catch (NumberFormatException ex) {
            messages.send(sender, "command.invalid-number");
        }
    }

    private void admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("linpet.admin")) {
            messages.send(sender, "command.no-permission");
            return;
        }
        if (args.length < 2) {
            messages.send(sender, "command.usage-admin");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "give" -> adminGive(sender, args);
            case "reload" -> adminReload(sender);
            case "view" -> adminView(sender, args, false);
            case "edit" -> adminView(sender, args, true);
            case "auction" -> adminAuction(sender, args);
            default -> messages.send(sender, "command.usage-admin");
        }
    }

    private void shop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("linpet.user.shop")) {
            messages.send(sender, "command.no-permission");
            return;
        }
        Player player = requirePlayer(sender).orElse(null);
        if (player == null) {
            return;
        }
        if (args.length < 3 || !"buy".equalsIgnoreCase(args[1])) {
            messages.send(sender, "command.usage-shop");
            return;
        }
        shopService.purchase(player, args[2])
            .whenComplete((result, throwable) -> scheduler.runOnEntity(player, () -> {
                if (throwable != null) {
                    messages.send(player, "shop.purchase-failed");
                    return;
                }
                if (result.success()) {
                    messages.send(player, result.messageKey(), Map.of("pet_id", result.petInstanceId().toString()));
                } else {
                    messages.send(player, result.messageKey());
                }
            }));
    }

    private void auction(CommandSender sender, String[] args) {
        if (!sender.hasPermission("linpet.auction.bid")) {
            messages.send(sender, "command.no-permission");
            return;
        }
        Player player = requirePlayer(sender).orElse(null);
        if (player == null) {
            return;
        }
        if (args.length < 4 || !"bid".equalsIgnoreCase(args[1])) {
            messages.send(sender, "command.usage-auction-bid");
            return;
        }
        try {
            UUID auctionId = UUID.fromString(args[2]);
            BigDecimal amount = new BigDecimal(args[3]);
            auctionService.bid(player, auctionId, amount)
                .whenComplete((result, throwable) -> scheduler.runOnEntity(player, () -> {
                    if (throwable != null) {
                        messages.send(player, "auction.bid-failed");
                        return;
                    }
                    messages.send(player, result.messageKey());
                }));
        } catch (IllegalArgumentException ex) {
            messages.send(sender, "command.invalid-number");
        }
    }

    private void adminGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("linpet.admin.give")) {
            messages.send(sender, "command.no-permission");
            return;
        }
        if (args.length < 4) {
            messages.send(sender, "command.usage-admin-give");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            messages.send(sender, "command.player-not-found");
            return;
        }
        String petId = args[3];
        int level = 1;
        if (args.length >= 5) {
            try {
                level = Integer.parseInt(args[4]);
            } catch (NumberFormatException ex) {
                messages.send(sender, "command.invalid-number");
                return;
            }
        }
        UUID actor = sender instanceof Player player ? player.getUniqueId() : null;
        petService.givePet(actor, target, petId, level)
            .whenComplete((pet, throwable) -> scheduler.runGlobal(() -> {
                if (throwable != null) {
                    messages.send(sender, "admin.give-failed", Map.of("reason", throwable.getMessage()));
                    return;
                }
                messages.send(sender, "admin.give-success", Map.of(
                    "player_name", target.getName(),
                    "pet_name", petId,
                    "pet_id", pet.instanceId().toString()
                ));
            }));
    }

    private void adminReload(CommandSender sender) {
        if (!sender.hasPermission("linpet.admin.reload")) {
            messages.send(sender, "command.no-permission");
            return;
        }
        try {
            reloadAction.run();
            messages.send(sender, "admin.reload-success");
        } catch (RuntimeException ex) {
            messages.send(sender, "admin.reload-failed", Map.of("reason", ex.getMessage()));
        }
    }

    private void adminView(CommandSender sender, String[] args, boolean edit) {
        if (!sender.hasPermission(edit ? "linpet.admin.edit" : "linpet.admin.view")) {
            messages.send(sender, "command.no-permission");
            return;
        }
        Player viewer = requirePlayer(sender).orElse(null);
        if (viewer == null) {
            return;
        }
        if (args.length < 3) {
            messages.send(sender, "command.usage-admin-view");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            messages.send(sender, "command.player-not-found");
            return;
        }
        menuFactory.openEquipment(viewer, target, edit);
    }

    private void adminAuction(CommandSender sender, String[] args) {
        if (!sender.hasPermission("linpet.admin.auction")) {
            messages.send(sender, "command.no-permission");
            return;
        }
        if (args.length < 4) {
            messages.send(sender, "command.usage-admin-auction");
            return;
        }
        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "start" -> adminAuctionStart(sender, args);
            case "stop" -> adminAuctionStop(sender, args);
            case "extend" -> adminAuctionExtend(sender, args);
            default -> messages.send(sender, "command.usage-admin-auction");
        }
    }

    private void adminAuctionStart(CommandSender sender, String[] args) {
        BigDecimal price = configRef.get().settings().startingAuctionPrice();
        if (args.length >= 5) {
            try {
                price = new BigDecimal(args[4]);
            } catch (NumberFormatException ex) {
                messages.send(sender, "command.invalid-number");
                return;
            }
        }
        UUID actor = sender instanceof Player player ? player.getUniqueId() : null;
        auctionService.startSystemAuction(actor, args[3], price)
            .whenComplete((listing, throwable) -> scheduler.runGlobal(() -> {
                if (throwable != null) {
                    messages.send(sender, "auction.start-failed", Map.of("reason", throwable.getMessage()));
                    return;
                }
                messages.send(sender, "auction.start-success", Map.of("auction_id", listing.auctionId().toString()));
            }));
    }

    private void adminAuctionStop(CommandSender sender, String[] args) {
        UUID actor = sender instanceof Player player ? player.getUniqueId() : null;
        try {
            UUID auctionId = UUID.fromString(args[3]);
            auctionService.stopAuction(actor, auctionId)
                .whenComplete((stopped, throwable) -> scheduler.runGlobal(() -> {
                    if (throwable != null || !stopped) {
                        messages.send(sender, "auction.stop-failed");
                        return;
                    }
                    messages.send(sender, "auction.stop-success", Map.of("auction_id", auctionId.toString()));
                }));
        } catch (IllegalArgumentException ex) {
            messages.send(sender, "command.invalid-uuid");
        }
    }

    private void adminAuctionExtend(CommandSender sender, String[] args) {
        if (args.length < 5) {
            messages.send(sender, "command.usage-admin-auction");
            return;
        }
        UUID actor = sender instanceof Player player ? player.getUniqueId() : null;
        try {
            UUID auctionId = UUID.fromString(args[3]);
            long minutes = Long.parseLong(args[4]);
            auctionService.extendAuction(actor, auctionId, minutes)
                .whenComplete((extended, throwable) -> scheduler.runGlobal(() -> {
                    if (throwable != null || !extended) {
                        messages.send(sender, "auction.extend-failed");
                        return;
                    }
                    messages.send(sender, "auction.extend-success", Map.of(
                        "auction_id", auctionId.toString(),
                        "minutes", Long.toString(minutes)
                    ));
                }));
        } catch (IllegalArgumentException ex) {
            messages.send(sender, "command.invalid-number");
        }
    }

    private java.util.Optional<Player> requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return java.util.Optional.of(player);
        }
        messages.send(sender, "command.players-only");
        return java.util.Optional.empty();
    }

    private List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
            .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
            .sorted()
            .toList();
    }
}

package dev.sleepwithyourmom.linx.linpet.gui;

import dev.sleepwithyourmom.linx.linpet.domain.pet.PetDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;
import dev.sleepwithyourmom.linx.linpet.item.PetItemCodec;
import dev.sleepwithyourmom.linx.linpet.service.MessageService;
import dev.sleepwithyourmom.linx.linpet.service.PetActionResult;
import dev.sleepwithyourmom.linx.linpet.service.PetService;
import dev.sleepwithyourmom.linx.linpet.service.RankService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Equipment menu that handles pet drag/click equip and unequip workflows.
 */
public class EquipmentMenu implements LinPetMenu {
    private static final int SIZE = 54;

    private final Player viewer;
    private final Player owner;
    private final boolean adminEdit;
    private final Inventory inventory;
    private final PetService petService;
    private final RankService rankService;
    private final MessageService messages;
    private final PetItemCodec itemCodec;
    private final MenuFactory menuFactory;

    /**
     * Creates an equipment menu.
     *
     * @param viewer viewer
     * @param owner equipment owner
     * @param adminEdit whether the viewer may edit another player's equipment
     * @param petService pet service
     * @param rankService rank service
     * @param messages message service
     * @param itemCodec item codec
     * @param menuFactory menu factory
     */
    public EquipmentMenu(
        Player viewer,
        Player owner,
        boolean adminEdit,
        PetService petService,
        RankService rankService,
        MessageService messages,
        PetItemCodec itemCodec,
        MenuFactory menuFactory
    ) {
        if (viewer == null || owner == null || petService == null || rankService == null
            || messages == null || itemCodec == null || menuFactory == null) {
            throw new IllegalArgumentException("equipment menu dependencies must not be null");
        }
        this.viewer = viewer;
        this.owner = owner;
        this.adminEdit = adminEdit;
        this.petService = petService;
        this.rankService = rankService;
        this.messages = messages;
        this.itemCodec = itemCodec;
        this.menuFactory = menuFactory;
        this.inventory = Bukkit.createInventory(this, SIZE, title());
        refresh();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void refresh() {
        inventory.clear();
        int activeSlots = rankService.slotLimit(owner);
        Map<Integer, PetInstance> equipped = petService.equippedSnapshot(owner.getUniqueId());
        for (int slot = 0; slot < SIZE; slot++) {
            if (slot < activeSlots) {
                PetInstance pet = equipped.get(slot);
                if (pet != null) {
                    Optional<PetDefinition> definition = petService.definition(pet.templateId());
                    ItemStack item = definition
                        .map(value -> itemCodec.encode(pet, value))
                        .orElseGet(() -> MenuItems.item(Material.BARRIER, "<red>Pet lỗi", List.of("<gray>" + pet.templateId())));
                    inventory.setItem(slot, itemCodec.markMenuSlot(item, slot, false));
                } else {
                    inventory.setItem(slot, itemCodec.markMenuSlot(emptySlot(), slot, false));
                }
            } else if (slot < 20) {
                inventory.setItem(slot, itemCodec.markMenuSlot(lockedSlot(), slot, true));
            } else {
                inventory.setItem(slot, itemCodec.markFiller(filler()));
            }
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        boolean topClick = event.getRawSlot() >= 0 && event.getRawSlot() < inventory.getSize();
        if (isReadOnlyFor(player)) {
            if (topClick || event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }
        InventoryAction action = event.getAction();
        if (action == InventoryAction.HOTBAR_SWAP
            || "HOTBAR_MOVE_AND_READD".equals(action.name())
            || action == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        if (topClick) {
            event.setCancelled(true);
            handleTopClick(event, player);
            return;
        }
        if (event.isShiftClick() && event.getCurrentItem() != null) {
            event.setCancelled(true);
            int freeSlot = petService.firstFreeSlot(owner);
            if (freeSlot < 0) {
                messages.send(player, "equipment.no-free-slot");
                return;
            }
            PetActionResult result = petService.equip(owner, event.getCurrentItem(), freeSlot);
            messages.send(player, result.messageKey());
            if (result.success()) {
                event.setCurrentItem(null);
                refresh();
            }
        }
    }

    @Override
    public void handleDrag(InventoryDragEvent event) {
        boolean touchesTop = event.getRawSlots().stream().anyMatch(slot -> slot < inventory.getSize());
        if (!touchesTop) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || isReadOnlyFor(player)) {
            return;
        }
        List<Integer> topTargets = event.getRawSlots().stream()
            .filter(slot -> slot < inventory.getSize())
            .sorted()
            .toList();
        if (topTargets.size() != 1) {
            return;
        }
        int slot = topTargets.get(0);
        if (slot >= rankService.slotLimit(owner)) {
            messages.send(player, "equipment.slot-locked");
            return;
        }
        PetActionResult result = petService.equip(owner, event.getOldCursor(), slot);
        messages.send(player, result.messageKey());
        if (result.success()) {
            player.setItemOnCursor(null);
            refresh();
        }
    }

    private void handleTopClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        ItemStack current = event.getCurrentItem();
        if (slot >= rankService.slotLimit(owner) || itemCodec.isLockedSlot(current)) {
            messages.send(player, "equipment.slot-locked");
            return;
        }
        if (event.getClick() == ClickType.RIGHT && current != null && itemCodec.isPetItem(current)) {
            petService.itemCodec().decode(current).ifPresent(pet ->
                petService.definition(pet.templateId()).ifPresent(definition ->
                    new PetInfoMenu(player, owner, pet, definition, itemCodec, menuFactory).open(player)));
            return;
        }
        if (current != null && itemCodec.isPetItem(current)
            && (event.getClick().isLeftClick() || event.getClick().isShiftClick())) {
            PetActionResult result = petService.unequip(owner, slot);
            messages.send(player, result.messageKey());
            refresh();
            return;
        }
        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            PetActionResult result = petService.equip(owner, cursor, slot);
            messages.send(player, result.messageKey());
            if (result.success()) {
                player.setItemOnCursor(null);
                refresh();
            }
        }
    }

    private boolean isReadOnlyFor(HumanEntity entity) {
        return !entity.getUniqueId().equals(owner.getUniqueId()) && !adminEdit;
    }

    private Component title() {
        MiniMessage mini = MiniMessage.miniMessage();
        if (viewer.getUniqueId().equals(owner.getUniqueId())) {
            return mini.deserialize("<green>Trang bị Linh Thú của " + mini.escapeTags(owner.getName()));
        }
        if (adminEdit) {
            return mini.deserialize("<red>Chỉnh sửa Linh Thú của " + mini.escapeTags(owner.getName()));
        }
        return mini.deserialize("<gold>Trang bị của " + mini.escapeTags(owner.getName()) + " <gray>(Xem)");
    }

    private ItemStack emptySlot() {
        return MenuItems.item(Material.GRAY_STAINED_GLASS_PANE, "<gray>Trống", List.of("<gray>Kéo thả pet vào đây"));
    }

    private ItemStack lockedSlot() {
        return MenuItems.item(Material.RED_STAINED_GLASS_PANE, "<red>Slot bị khóa", List.of("<gray>Nâng cấp rank để mở"));
    }

    private ItemStack filler() {
        return MenuItems.item(Material.BLACK_STAINED_GLASS_PANE, "<dark_gray>", List.of());
    }
}

package dev.sleepwithyourmom.linx.linpet.gui;

import dev.sleepwithyourmom.linx.linpet.domain.pet.PetDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;
import dev.sleepwithyourmom.linx.linpet.item.PetItemCodec;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/**
 * Read-only pet detail menu.
 */
public class PetInfoMenu implements LinPetMenu {
    private final Inventory inventory;
    private final PetItemCodec itemCodec;
    private final PetInstance pet;
    private final PetDefinition definition;
    private final MenuFactory menuFactory;
    private final Player viewer;
    private final Player owner;

    /**
     * Creates a pet detail menu.
     *
     * @param viewer viewer
     * @param owner owner
     * @param pet pet instance
     * @param definition pet definition
     * @param itemCodec item codec
     * @param menuFactory menu factory
     */
    public PetInfoMenu(
        Player viewer,
        Player owner,
        PetInstance pet,
        PetDefinition definition,
        PetItemCodec itemCodec,
        MenuFactory menuFactory
    ) {
        if (viewer == null || owner == null || pet == null || definition == null || itemCodec == null || menuFactory == null) {
            throw new IllegalArgumentException("pet info menu dependencies must not be null");
        }
        this.viewer = viewer;
        this.owner = owner;
        this.pet = pet;
        this.definition = definition;
        this.itemCodec = itemCodec;
        this.menuFactory = menuFactory;
        this.inventory = Bukkit.createInventory(this, 27,
            MiniMessage.miniMessage().deserialize("<green>Thông tin Linh Thú"));
        refresh();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void refresh() {
        inventory.clear();
        inventory.setItem(13, itemCodec.encode(pet, definition));
        inventory.setItem(22, MenuItems.item(Material.ARROW, "<yellow>Quay lại", List.of()));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() == 22) {
            menuFactory.openEquipment(viewer, owner, false);
        }
    }

    @Override
    public void handleDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }
}

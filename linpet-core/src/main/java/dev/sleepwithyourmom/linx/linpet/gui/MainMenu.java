package dev.sleepwithyourmom.linx.linpet.gui;

import dev.sleepwithyourmom.linx.linpet.service.MessageService;
import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/**
 * Main Lin'Pet navigation menu.
 */
public class MainMenu implements LinPetMenu {
    private final Player viewer;
    private final MenuFactory menuFactory;
    private final Inventory inventory;

    /**
     * Creates the main menu.
     *
     * @param viewer viewer
     * @param menuFactory menu factory
     * @param messageService message service
     */
    public MainMenu(Player viewer, MenuFactory menuFactory, MessageService messageService) {
        if (viewer == null || menuFactory == null || messageService == null) {
            throw new IllegalArgumentException("main menu dependencies must not be null");
        }
        this.viewer = viewer;
        this.menuFactory = menuFactory;
        this.inventory = Bukkit.createInventory(this, 27,
            MiniMessage.miniMessage().deserialize("<green>Lin'Pet"));
        refresh();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void refresh() {
        inventory.clear();
        inventory.setItem(10, MenuItems.item(Material.PLAYER_HEAD, "<green>Trang bị",
            List.of("<gray>Mở các slot Linh Thú đang dùng")));
        inventory.setItem(12, MenuItems.item(Material.CHEST, "<aqua>Danh sách pet",
            List.of("<gray>Xem pet trong inventory và DB")));
        inventory.setItem(14, MenuItems.item(Material.EMERALD, "<gold>Cửa hàng",
            List.of("<gray>Mua pet được cấu hình")));
        inventory.setItem(16, MenuItems.item(Material.GOLD_INGOT, "<yellow>Đấu giá",
            List.of("<gray>Xem các phiên đấu giá đang mở")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() == 10) {
            menuFactory.openEquipment(viewer, viewer, false);
        }
    }

    @Override
    public void handleDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }
}

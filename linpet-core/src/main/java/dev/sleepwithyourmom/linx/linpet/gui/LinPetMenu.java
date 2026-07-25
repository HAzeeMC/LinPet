package dev.sleepwithyourmom.linx.linpet.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Common lifecycle contract for Lin'Pet inventory menus.
 */
public interface LinPetMenu extends InventoryHolder {
    /**
     * Opens this menu for a viewer.
     *
     * @param viewer viewer
     */
    default void open(Player viewer) {
        viewer.openInventory(getInventory());
    }

    /**
     * Refreshes rendered inventory contents.
     */
    void refresh();

    /**
     * Handles an inventory click event.
     *
     * @param event click event
     */
    void handleClick(InventoryClickEvent event);

    /**
     * Handles an inventory drag event.
     *
     * @param event drag event
     */
    void handleDrag(InventoryDragEvent event);

    @Override
    Inventory getInventory();
}

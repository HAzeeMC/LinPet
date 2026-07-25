package dev.sleepwithyourmom.linx.linpet.listener;

import dev.sleepwithyourmom.linx.linpet.gui.LinPetMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Routes inventory events to Lin'Pet menus and prevents vanilla transfer bypasses.
 */
public class GuiListener implements Listener {
    /**
     * Routes click events.
     *
     * @param event click event
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof LinPetMenu menu) {
            menu.handleClick(event);
        }
    }

    /**
     * Routes drag events.
     *
     * @param event drag event
     */
    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof LinPetMenu menu) {
            menu.handleDrag(event);
        }
    }
}

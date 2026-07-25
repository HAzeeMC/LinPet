package dev.sleepwithyourmom.linx.linpet.api.event;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired before a pet item is equipped into a Lin'Pet equipment slot.
 */
public class PetEquipEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final UUID petInstanceId;
    private final int slot;
    private final ItemStack petItem;
    private boolean cancelled;

    /**
     * Creates an equip event.
     *
     * @param player player equipping the pet
     * @param petInstanceId unique pet instance id
     * @param slot zero-based equipment slot
     * @param petItem pet item being equipped
     */
    public PetEquipEvent(Player player, UUID petInstanceId, int slot, ItemStack petItem) {
        this.player = player;
        this.petInstanceId = petInstanceId;
        this.slot = slot;
        this.petItem = petItem.clone();
    }

    /**
     * Returns the player equipping the pet.
     *
     * @return player
     */
    public Player player() {
        return player;
    }

    /**
     * Returns the pet instance identifier.
     *
     * @return pet instance id
     */
    public UUID petInstanceId() {
        return petInstanceId;
    }

    /**
     * Returns the target equipment slot.
     *
     * @return zero-based slot
     */
    public int slot() {
        return slot;
    }

    /**
     * Returns a clone of the pet item being equipped.
     *
     * @return cloned pet item
     */
    public ItemStack petItem() {
        return petItem.clone();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Returns Bukkit handlers for this event.
     *
     * @return handler list
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

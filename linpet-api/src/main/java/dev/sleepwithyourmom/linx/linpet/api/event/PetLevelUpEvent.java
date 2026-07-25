package dev.sleepwithyourmom.linx.linpet.api.event;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired after a pet instance gains one or more levels.
 */
public class PetLevelUpEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final UUID petInstanceId;
    private final int oldLevel;
    private final int newLevel;

    /**
     * Creates a level-up event.
     *
     * @param player pet owner
     * @param petInstanceId pet instance id
     * @param oldLevel previous level
     * @param newLevel new level
     */
    public PetLevelUpEvent(Player player, UUID petInstanceId, int oldLevel, int newLevel) {
        this.player = player;
        this.petInstanceId = petInstanceId;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    /**
     * Returns the pet owner.
     *
     * @return owner player
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
     * Returns the previous pet level.
     *
     * @return old level
     */
    public int oldLevel() {
        return oldLevel;
    }

    /**
     * Returns the new pet level.
     *
     * @return new level
     */
    public int newLevel() {
        return newLevel;
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

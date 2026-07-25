package dev.sleepwithyourmom.linx.linpet.api.event;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before a skill is committed to a pet instance.
 */
public class SkillUnlockEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final UUID petInstanceId;
    private final String skillId;
    private boolean cancelled;

    /**
     * Creates a skill unlock event.
     *
     * @param player owner unlocking the skill
     * @param petInstanceId pet instance id
     * @param skillId configured skill id
     */
    public SkillUnlockEvent(Player player, UUID petInstanceId, String skillId) {
        this.player = player;
        this.petInstanceId = petInstanceId;
        this.skillId = skillId;
    }

    /**
     * Returns the pet owner.
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
     * Returns the skill identifier.
     *
     * @return skill id
     */
    public String skillId() {
        return skillId;
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

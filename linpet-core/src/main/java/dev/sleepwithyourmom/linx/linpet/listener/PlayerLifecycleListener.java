package dev.sleepwithyourmom.linx.linpet.listener;

import dev.sleepwithyourmom.linx.linpet.service.PetService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player lifecycle transitions for cache and buff state.
 */
public class PlayerLifecycleListener implements Listener {
    private final PetService petService;

    /**
     * Creates lifecycle listener.
     *
     * @param petService pet service
     */
    public PlayerLifecycleListener(PetService petService) {
        if (petService == null) {
            throw new IllegalArgumentException("petService must not be null");
        }
        this.petService = petService;
    }

    /**
     * Loads player state on join.
     *
     * @param event join event
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        petService.loadPlayer(event.getPlayer());
    }

    /**
     * Clears player state on quit.
     *
     * @param event quit event
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        petService.unloadPlayer(event.getPlayer());
    }

    /**
     * Reapplies or disables buffs when the player moves across blacklist boundaries.
     *
     * @param event world-change event
     */
    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        petService.reapplyBuffs(event.getPlayer());
    }
}

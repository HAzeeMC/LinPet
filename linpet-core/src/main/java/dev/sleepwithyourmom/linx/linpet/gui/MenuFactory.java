package dev.sleepwithyourmom.linx.linpet.gui;

import dev.sleepwithyourmom.linx.linpet.item.PetItemCodec;
import dev.sleepwithyourmom.linx.linpet.service.MessageService;
import dev.sleepwithyourmom.linx.linpet.service.PetService;
import dev.sleepwithyourmom.linx.linpet.service.RankService;
import org.bukkit.entity.Player;

/**
 * Factory for Lin'Pet inventory menus.
 */
public class MenuFactory {
    private final PetService petService;
    private final RankService rankService;
    private final MessageService messageService;
    private final PetItemCodec itemCodec;

    /**
     * Creates a menu factory.
     *
     * @param petService pet service
     * @param rankService rank service
     * @param messageService message service
     * @param itemCodec item codec
     */
    public MenuFactory(PetService petService, RankService rankService, MessageService messageService, PetItemCodec itemCodec) {
        if (petService == null || rankService == null || messageService == null || itemCodec == null) {
            throw new IllegalArgumentException("menu factory dependencies must not be null");
        }
        this.petService = petService;
        this.rankService = rankService;
        this.messageService = messageService;
        this.itemCodec = itemCodec;
    }

    /**
     * Opens the main menu.
     *
     * @param viewer viewer
     */
    public void openMain(Player viewer) {
        new MainMenu(viewer, this, messageService).open(viewer);
    }

    /**
     * Opens an equipment menu.
     *
     * @param viewer viewer
     * @param owner target owner
     * @param adminEdit whether editing another player is allowed
     */
    public void openEquipment(Player viewer, Player owner, boolean adminEdit) {
        new EquipmentMenu(viewer, owner, adminEdit, petService, rankService, messageService, itemCodec, this).open(viewer);
    }
}

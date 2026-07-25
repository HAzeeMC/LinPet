package dev.sleepwithyourmom.linx.linpet.gui;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Utility for constructing menu items with Adventure text.
 */
public final class MenuItems {
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private MenuItems() {
    }

    /**
     * Creates an item with MiniMessage display text and lore.
     *
     * @param material material
     * @param name MiniMessage display name
     * @param lore MiniMessage lore lines
     * @return item stack
     */
    public static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MINI.deserialize(name));
        meta.lore(lore.stream().map(MINI::deserialize).map(Component.class::cast).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}

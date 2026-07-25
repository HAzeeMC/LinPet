package dev.sleepwithyourmom.linx.linpet.item;

import dev.sleepwithyourmom.linx.linpet.api.model.PetRarity;
import java.util.Map;

/**
 * MiniMessage color palette for pet rarity rendering.
 */
public final class RarityPalette {
    private static final Map<PetRarity, String> COLORS = Map.of(
        PetRarity.COMMON, "<white>",
        PetRarity.UNCOMMON, "<green>",
        PetRarity.RARE, "<blue>",
        PetRarity.EPIC, "<gold>",
        PetRarity.LEGENDARY, "<yellow>",
        PetRarity.MYTHIC, "<light_purple>",
        PetRarity.ULTRA_RARE, "<#FFD700>"
    );

    private RarityPalette() {
    }

    /**
     * Returns the MiniMessage color prefix for a rarity.
     *
     * @param rarity pet rarity
     * @return color tag
     */
    public static String color(PetRarity rarity) {
        return COLORS.getOrDefault(rarity, "<white>");
    }
}

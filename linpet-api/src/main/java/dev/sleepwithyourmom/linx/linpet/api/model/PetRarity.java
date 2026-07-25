package dev.sleepwithyourmom.linx.linpet.api.model;

import java.util.Locale;
import java.util.Optional;

/**
 * Enumerates the configured Lin'Pet rarity tiers exposed to addons.
 */
public enum PetRarity {
    /**
     * Baseline pet rarity.
     */
    COMMON("Common"),

    /**
     * Slightly stronger than common.
     */
    UNCOMMON("Uncommon"),

    /**
     * Rare pet rarity.
     */
    RARE("Rare"),

    /**
     * Epic pet rarity.
     */
    EPIC("Epic"),

    /**
     * Legendary pet rarity.
     */
    LEGENDARY("Legendary"),

    /**
     * Mythic pet rarity.
     */
    MYTHIC("Mythic"),

    /**
     * Auction-focused ultra rare pet rarity.
     */
    ULTRA_RARE("Ultra Rare");

    private final String displayName;

    PetRarity(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable rarity name used by configuration files.
     *
     * @return display name for this rarity
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Parses a config rarity value. Spaces, hyphens, and underscores are treated equivalently.
     *
     * @param value configured rarity value
     * @return parsed rarity when the value is valid
     */
    public static Optional<PetRarity> fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim()
            .replace('-', '_')
            .replace(' ', '_')
            .toUpperCase(Locale.ROOT);
        for (PetRarity rarity : values()) {
            if (rarity.name().equals(normalized)) {
                return Optional.of(rarity);
            }
        }
        return Optional.empty();
    }
}

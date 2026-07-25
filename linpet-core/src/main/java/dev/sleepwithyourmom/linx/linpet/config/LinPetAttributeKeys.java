package dev.sleepwithyourmom.linx.linpet.config;

import java.util.Locale;
import java.util.Set;

/**
 * Registry of Lin'Pet custom attribute keys declared by the technical design.
 */
public final class LinPetAttributeKeys {
    private static final Set<String> CUSTOM_KEYS = Set.of(
        "linpet:critical_chance",
        "linpet:critical_damage",
        "linpet:lifesteal",
        "linpet:thorns",
        "linpet:dodge",
        "linpet:block_chance",
        "linpet:health_regen",
        "linpet:magic_resist",
        "linpet:fall_damage_reduction",
        "linpet:harvest_speed",
        "linpet:jump_boost",
        "linpet:knockback_resist",
        "linpet:water_breathing",
        "linpet:poison_resist",
        "linpet:wither_resist",
        "linpet:fire_resist"
    );

    private LinPetAttributeKeys() {
    }

    /**
     * Checks whether a namespaced key is a supported Lin'Pet custom attribute.
     *
     * @param key namespaced attribute key
     * @return true when supported
     */
    public static boolean isCustomAttribute(String key) {
        return key != null && CUSTOM_KEYS.contains(key.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns all supported custom attribute keys.
     *
     * @return immutable custom attribute key set
     */
    public static Set<String> all() {
        return CUSTOM_KEYS;
    }
}

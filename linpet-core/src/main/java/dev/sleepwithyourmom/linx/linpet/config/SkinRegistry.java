package dev.sleepwithyourmom.linx.linpet.config;

import java.util.Map;
import java.util.Optional;

/**
 * Registry of configured skin aliases and their resolved references.
 *
 * @param skins configured skin references by id
 */
public record SkinRegistry(Map<String, String> skins) {
    /**
     * Creates a defensive skin registry.
     */
    public SkinRegistry {
        skins = Map.copyOf(skins == null ? Map.of() : skins);
    }

    /**
     * Resolves an alias to its configured value.
     *
     * @param skinId skin id or raw reference
     * @return configured value when present, otherwise the original reference
     */
    public String resolve(String skinId) {
        if (skinId == null || skinId.isBlank()) {
            throw new IllegalArgumentException("skinId must not be blank");
        }
        return Optional.ofNullable(skins.get(skinId)).orElse(skinId);
    }
}

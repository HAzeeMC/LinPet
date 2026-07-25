package dev.sleepwithyourmom.linx.linpet.service;

import dev.sleepwithyourmom.linx.linpet.config.LinPetConfig;
import dev.sleepwithyourmom.linx.linpet.domain.rank.RankDefinition;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.entity.Player;

/**
 * Resolves rank utility limits from permissions and config.
 */
public class RankService {
    private final AtomicReference<LinPetConfig> configRef;

    /**
     * Creates a rank service.
     *
     * @param configRef live config reference
     */
    public RankService(AtomicReference<LinPetConfig> configRef) {
        if (configRef == null) {
            throw new IllegalArgumentException("configRef must not be null");
        }
        this.configRef = configRef;
    }

    /**
     * Resolves a player's effective slot limit.
     *
     * @param player player
     * @return slot limit clamped to configured maximum
     */
    public int slotLimit(Player player) {
        LinPetConfig config = configRef.get();
        int configuredDefault = config.settings().defaultSlots();
        int rankLimit = config.ranks().values().stream()
            .filter(rank -> player.hasPermission("linpet.rank." + rank.id()))
            .map(RankDefinition::slotLimit)
            .max(Comparator.naturalOrder())
            .orElse(configuredDefault);
        return Math.min(rankLimit, config.settings().maxSlotsPerPlayer());
    }

    /**
     * Returns whether the player bypasses blacklisted worlds.
     *
     * @param player player
     * @return true when any effective rank bypasses blacklists
     */
    public boolean bypassesBlacklist(Player player) {
        LinPetConfig config = configRef.get();
        return config.ranks().values().stream()
            .filter(rank -> rank.bypassBlacklist() && player.hasPermission("linpet.rank." + rank.id()))
            .findAny()
            .isPresent();
    }

    /**
     * Resolves the highest-slot rank id for placeholder/API display.
     *
     * @param player player
     * @return rank id or {@code default}
     */
    public String rankId(Player player) {
        return configRef.get().ranks().values().stream()
            .filter(rank -> player.hasPermission("linpet.rank." + rank.id()))
            .max(Comparator.comparingInt(RankDefinition::slotLimit))
            .map(RankDefinition::id)
            .orElse("default");
    }
}

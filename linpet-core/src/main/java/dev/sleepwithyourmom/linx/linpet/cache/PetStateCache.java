package dev.sleepwithyourmom.linx.linpet.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PlayerPetState;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Online player pet-state cache backed by Caffeine.
 */
public class PetStateCache {
    private final Cache<UUID, PlayerPetState> cache;

    /**
     * Creates a pet state cache.
     *
     * @param expireAfterAccess cache expiration after last access
     * @param maximumSize maximum cached player states
     */
    public PetStateCache(Duration expireAfterAccess, long maximumSize) {
        if (expireAfterAccess == null || expireAfterAccess.isNegative() || expireAfterAccess.isZero()) {
            throw new IllegalArgumentException("expireAfterAccess must be positive");
        }
        if (maximumSize < 1) {
            throw new IllegalArgumentException("maximumSize must be positive");
        }
        this.cache = Caffeine.newBuilder()
            .expireAfterAccess(expireAfterAccess)
            .maximumSize(maximumSize)
            .build();
    }

    /**
     * Reads cached state.
     *
     * @param playerId player UUID
     * @return player state when cached
     */
    public Optional<PlayerPetState> get(UUID playerId) {
        return Optional.ofNullable(cache.getIfPresent(playerId));
    }

    /**
     * Returns cached state or creates an empty one.
     *
     * @param playerId player UUID
     * @return player state
     */
    public PlayerPetState getOrCreate(UUID playerId) {
        return cache.get(playerId, id -> new PlayerPetState(id, Map.of()));
    }

    /**
     * Writes a state snapshot to cache.
     *
     * @param state player state
     */
    public void put(PlayerPetState state) {
        cache.put(state.playerId(), state);
    }

    /**
     * Invalidates cached state for one player.
     *
     * @param playerId player UUID
     */
    public void invalidate(UUID playerId) {
        cache.invalidate(playerId);
    }

    /**
     * Clears every cached state.
     */
    public void invalidateAll() {
        cache.invalidateAll();
    }
}

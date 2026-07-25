package dev.sleepwithyourmom.linx.linpet.api.service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Static access point used by addons to obtain the active Lin'Pet API service.
 */
public final class LinPetProvider {
    private static final AtomicReference<LinPetApi> API = new AtomicReference<>();

    private LinPetProvider() {
    }

    /**
     * Returns the registered API instance.
     *
     * @return optional active API instance
     */
    public static Optional<LinPetApi> api() {
        return Optional.ofNullable(API.get());
    }

    /**
     * Registers the API implementation while the plugin is enabled.
     *
     * @param api API implementation
     * @throws IllegalStateException when an API implementation is already registered
     */
    public static void register(LinPetApi api) {
        if (api == null) {
            throw new IllegalArgumentException("api must not be null");
        }
        if (!API.compareAndSet(null, api)) {
            throw new IllegalStateException("LinPet API is already registered");
        }
    }

    /**
     * Removes the active API implementation.
     *
     * @param api API implementation currently registered
     */
    public static void unregister(LinPetApi api) {
        API.compareAndSet(api, null);
    }
}

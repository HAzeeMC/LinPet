package dev.sleepwithyourmom.linx.linpet.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.sleepwithyourmom.linx.linpet.config.SkinRegistry;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;
import com.destroystokyo.paper.profile.PlayerProfile;

/**
 * Applies configured player-head skins using Bukkit/Paper profile APIs.
 */
public class SkinResolver {
    private final AtomicReference<SkinRegistry> skinRegistry;

    /**
     * Creates a skin resolver.
     *
     * @param skinRegistry configured skin aliases
     */
    public SkinResolver(SkinRegistry skinRegistry) {
        if (skinRegistry == null) {
            throw new IllegalArgumentException("skinRegistry must not be null");
        }
        this.skinRegistry = new AtomicReference<>(skinRegistry);
    }

    /**
     * Updates the active skin registry after a config reload.
     *
     * @param skinRegistry new skin registry
     */
    public void setSkinRegistry(SkinRegistry skinRegistry) {
        if (skinRegistry == null) {
            throw new IllegalArgumentException("skinRegistry must not be null");
        }
        this.skinRegistry.set(skinRegistry);
    }

    /**
     * Applies a skin reference to skull metadata.
     *
     * @param meta skull metadata
     * @param skinId configured skin id or raw reference
     */
    public void apply(SkullMeta meta, String skinId) {
        if (meta == null) {
            throw new IllegalArgumentException("meta must not be null");
        }
        String resolved = skinRegistry.get().resolve(skinId);
        try {
            URL url = toSkinUrl(resolved);
            if (url != null) {
                PlayerProfile profile = Bukkit.getServer().createProfile(
                    UUID.nameUUIDFromBytes(("linpet-skin:" + url).getBytes(StandardCharsets.UTF_8)),
                    "LinPet"
                );
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(url);
                profile.setTextures(textures);
                meta.setPlayerProfile(profile);
                return;
            }
            meta.setPlayerProfile(Bukkit.getServer().createProfile(resolved));
        } catch (IllegalArgumentException ex) {
            throw new PetItemDecodeException("Invalid skin reference '" + skinId + "'", ex);
        }
    }

    private URL toSkinUrl(String reference) {
        if (reference.startsWith("http://") || reference.startsWith("https://")) {
            return url(reference);
        }
        if (reference.startsWith("base64:")) {
            String json = new String(Base64.getDecoder().decode(reference.substring("base64:".length())), StandardCharsets.UTF_8);
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            JsonObject textures = object.getAsJsonObject("textures");
            JsonObject skin = textures.getAsJsonObject("SKIN");
            if (skin == null || !skin.has("url")) {
                throw new PetItemDecodeException("Base64 skin payload does not contain textures.SKIN.url");
            }
            return url(skin.get("url").getAsString());
        }
        return null;
    }

    private URL url(String value) {
        try {
            return new URL(value);
        } catch (MalformedURLException ex) {
            throw new PetItemDecodeException("Invalid skin URL '" + value + "'", ex);
        }
    }
}

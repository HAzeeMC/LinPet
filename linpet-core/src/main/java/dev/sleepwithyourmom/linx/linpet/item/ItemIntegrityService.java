package dev.sleepwithyourmom.linx.linpet.item;

import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Computes and verifies short HMAC checksums for pet item PDC snapshots.
 */
public class ItemIntegrityService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] secret;

    /**
     * Loads or creates a persistent secret in the plugin data folder.
     *
     * @param plugin owning plugin
     * @return integrity service
     */
    public static ItemIntegrityService load(JavaPlugin plugin) {
        Path path = plugin.getDataFolder().toPath().resolve("item-hmac.key");
        try {
            Files.createDirectories(path.getParent());
            if (Files.exists(path)) {
                return new ItemIntegrityService(Base64.getDecoder().decode(Files.readString(path).trim()));
            }
            byte[] secret = new byte[32];
            new SecureRandom().nextBytes(secret);
            Files.writeString(path, Base64.getEncoder().encodeToString(secret));
            return new ItemIntegrityService(secret);
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Failed loading LinPet item HMAC secret", ex);
        }
    }

    /**
     * Creates a service using the supplied secret.
     *
     * @param secret HMAC secret bytes
     */
    public ItemIntegrityService(byte[] secret) {
        if (secret == null || secret.length < 32) {
            throw new IllegalArgumentException("secret must be at least 32 bytes");
        }
        this.secret = secret.clone();
    }

    /**
     * Signs a pet instance snapshot.
     *
     * @param pet pet instance
     * @return short hexadecimal checksum
     */
    public String sign(PetInstance pet) {
        return sign(canonicalPayload(pet));
    }

    /**
     * Verifies a pet item checksum.
     *
     * @param pet pet instance
     * @param checksum expected checksum
     * @return true when checksum matches
     */
    public boolean verify(PetInstance pet, String checksum) {
        if (checksum == null || checksum.isBlank()) {
            return false;
        }
        return constantTimeEquals(sign(pet), checksum);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] shortened = java.util.Arrays.copyOf(digest, 16);
            return HexFormat.of().formatHex(shortened);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to compute HMAC", ex);
        }
    }

    private String canonicalPayload(PetInstance pet) {
        String skills = pet.unlockedSkillIds().stream()
            .sorted(Comparator.naturalOrder())
            .collect(Collectors.joining(","));
        Instant expiresAt = pet.expiresAt();
        return String.join("|",
            pet.instanceId().toString(),
            pet.ownerId().toString(),
            pet.templateId(),
            Integer.toString(pet.level()),
            Double.toString(pet.experience()),
            Integer.toString(pet.skillPoints()),
            skills,
            pet.rarity().name(),
            pet.skinId(),
            pet.customName() == null ? "" : pet.customName(),
            expiresAt == null ? "-1" : Long.toString(expiresAt.toEpochMilli())
        );
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left.length() != right.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < left.length(); i++) {
            diff |= left.charAt(i) ^ right.charAt(i);
        }
        return diff == 0;
    }
}

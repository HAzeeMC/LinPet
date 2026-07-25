package dev.sleepwithyourmom.linx.linpet.item;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Centralized persistent-data keys used by Lin'Pet items and GUI markers.
 */
public class LinPetKeys {
    private final NamespacedKey marker;
    private final NamespacedKey itemVersion;
    private final NamespacedKey instanceId;
    private final NamespacedKey ownerId;
    private final NamespacedKey templateId;
    private final NamespacedKey level;
    private final NamespacedKey experience;
    private final NamespacedKey skillPoints;
    private final NamespacedKey unlockedSkills;
    private final NamespacedKey rarity;
    private final NamespacedKey skinId;
    private final NamespacedKey customName;
    private final NamespacedKey expiresAt;
    private final NamespacedKey checksum;
    private final NamespacedKey menuSlot;
    private final NamespacedKey lockedSlot;
    private final NamespacedKey filler;

    /**
     * Creates namespaced keys for the plugin.
     *
     * @param plugin owning plugin
     */
    public LinPetKeys(JavaPlugin plugin) {
        marker = key(plugin, "pet_marker");
        itemVersion = key(plugin, "item_version");
        instanceId = key(plugin, "pet_instance_id");
        ownerId = key(plugin, "owner_id");
        templateId = key(plugin, "pet_id");
        level = key(plugin, "level");
        experience = key(plugin, "experience");
        skillPoints = key(plugin, "skill_points");
        unlockedSkills = key(plugin, "unlocked_skills");
        rarity = key(plugin, "rarity");
        skinId = key(plugin, "skin_id");
        customName = key(plugin, "custom_name");
        expiresAt = key(plugin, "expires_at");
        checksum = key(plugin, "checksum");
        menuSlot = key(plugin, "menu_slot");
        lockedSlot = key(plugin, "locked");
        filler = key(plugin, "filler");
    }

    /**
     * Returns item marker key.
     *
     * @return marker key
     */
    public NamespacedKey marker() {
        return marker;
    }

    /**
     * Returns item version key.
     *
     * @return item version key
     */
    public NamespacedKey itemVersion() {
        return itemVersion;
    }

    /**
     * Returns pet instance id key.
     *
     * @return instance id key
     */
    public NamespacedKey instanceId() {
        return instanceId;
    }

    /**
     * Returns owner id key.
     *
     * @return owner id key
     */
    public NamespacedKey ownerId() {
        return ownerId;
    }

    /**
     * Returns template id key.
     *
     * @return template id key
     */
    public NamespacedKey templateId() {
        return templateId;
    }

    /**
     * Returns level key.
     *
     * @return level key
     */
    public NamespacedKey level() {
        return level;
    }

    /**
     * Returns experience key.
     *
     * @return experience key
     */
    public NamespacedKey experience() {
        return experience;
    }

    /**
     * Returns skill points key.
     *
     * @return skill points key
     */
    public NamespacedKey skillPoints() {
        return skillPoints;
    }

    /**
     * Returns unlocked skills key.
     *
     * @return unlocked skills key
     */
    public NamespacedKey unlockedSkills() {
        return unlockedSkills;
    }

    /**
     * Returns rarity key.
     *
     * @return rarity key
     */
    public NamespacedKey rarity() {
        return rarity;
    }

    /**
     * Returns skin id key.
     *
     * @return skin id key
     */
    public NamespacedKey skinId() {
        return skinId;
    }

    /**
     * Returns custom name key.
     *
     * @return custom name key
     */
    public NamespacedKey customName() {
        return customName;
    }

    /**
     * Returns expiration key.
     *
     * @return expiration key
     */
    public NamespacedKey expiresAt() {
        return expiresAt;
    }

    /**
     * Returns checksum key.
     *
     * @return checksum key
     */
    public NamespacedKey checksum() {
        return checksum;
    }

    /**
     * Returns GUI menu slot key.
     *
     * @return menu slot key
     */
    public NamespacedKey menuSlot() {
        return menuSlot;
    }

    /**
     * Returns locked slot marker key.
     *
     * @return locked slot key
     */
    public NamespacedKey lockedSlot() {
        return lockedSlot;
    }

    /**
     * Returns filler marker key.
     *
     * @return filler key
     */
    public NamespacedKey filler() {
        return filler;
    }

    private NamespacedKey key(JavaPlugin plugin, String value) {
        return new NamespacedKey(plugin, value);
    }
}

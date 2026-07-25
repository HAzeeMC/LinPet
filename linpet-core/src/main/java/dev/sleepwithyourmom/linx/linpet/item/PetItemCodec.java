package dev.sleepwithyourmom.linx.linpet.item;

import dev.sleepwithyourmom.linx.linpet.api.model.PetRarity;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetInstance;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Encodes and decodes Lin'Pet item data in {@link PersistentDataContainer}.
 */
public class PetItemCodec {
    private static final int ITEM_VERSION = 1;

    private final LinPetKeys keys;
    private final ItemIntegrityService integrityService;
    private final SkinResolver skinResolver;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Creates a pet item codec.
     *
     * @param keys PDC keys
     * @param integrityService HMAC service
     * @param skinResolver skin resolver
     */
    public PetItemCodec(LinPetKeys keys, ItemIntegrityService integrityService, SkinResolver skinResolver) {
        if (keys == null || integrityService == null || skinResolver == null) {
            throw new IllegalArgumentException("keys, integrityService and skinResolver must not be null");
        }
        this.keys = keys;
        this.integrityService = integrityService;
        this.skinResolver = skinResolver;
    }

    /**
     * Creates a signed player-head item for a pet instance.
     *
     * @param pet pet instance
     * @param definition pet definition
     * @return item stack with PDC data and checksum
     */
    public ItemStack encode(PetInstance pet, PetDefinition definition) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.displayName(miniMessage.deserialize(RarityPalette.color(pet.rarity()) + pet.displayName(definition)));
        meta.lore(lore(pet, definition));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setMaxStackSize(1);
        skinResolver.apply(meta, pet.skinId());
        writePetData(meta.getPersistentDataContainer(), pet);
        item.setItemMeta(meta);
        item.setAmount(1);
        return item;
    }

    /**
     * Returns true when the item contains Lin'Pet marker data.
     *
     * @param item item to check
     * @return true for Lin'Pet pet items
     */
    public boolean isPetItem(ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(keys.marker(), PersistentDataType.INTEGER);
    }

    /**
     * Decodes and verifies a pet item.
     *
     * @param item item stack
     * @return decoded pet when the item is not a Lin'Pet item
     * @throws PetItemDecodeException when marker data is present but invalid
     */
    public Optional<PetInstance> decode(ItemStack item) {
        if (!isPetItem(item)) {
            return Optional.empty();
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        try {
            int version = required(pdc, keys.itemVersion(), PersistentDataType.INTEGER, "item_version");
            if (version != ITEM_VERSION) {
                throw new PetItemDecodeException("Unsupported pet item version " + version);
            }
            long expiresAtMillis = required(pdc, keys.expiresAt(), PersistentDataType.LONG, "expires_at");
            PetInstance pet = new PetInstance(
                UUID.fromString(required(pdc, keys.instanceId(), PersistentDataType.STRING, "pet_instance_id")),
                UUID.fromString(required(pdc, keys.ownerId(), PersistentDataType.STRING, "owner_id")),
                required(pdc, keys.templateId(), PersistentDataType.STRING, "pet_id"),
                required(pdc, keys.level(), PersistentDataType.INTEGER, "level"),
                required(pdc, keys.experience(), PersistentDataType.DOUBLE, "experience"),
                required(pdc, keys.skillPoints(), PersistentDataType.INTEGER, "skill_points"),
                decodeSkills(required(pdc, keys.unlockedSkills(), PersistentDataType.STRING, "unlocked_skills")),
                PetRarity.valueOf(required(pdc, keys.rarity(), PersistentDataType.STRING, "rarity")),
                required(pdc, keys.skinId(), PersistentDataType.STRING, "skin_id"),
                optional(pdc, keys.customName(), PersistentDataType.STRING),
                expiresAtMillis < 0L ? null : Instant.ofEpochMilli(expiresAtMillis)
            );
            String checksum = required(pdc, keys.checksum(), PersistentDataType.STRING, "checksum");
            if (!integrityService.verify(pet, checksum)) {
                throw new PetItemDecodeException("Pet item checksum does not match");
            }
            return Optional.of(pet);
        } catch (IllegalArgumentException ex) {
            throw new PetItemDecodeException("Pet item data is malformed", ex);
        }
    }

    /**
     * Marks an item as an equipment menu slot.
     *
     * @param item item to mark
     * @param slot slot number
     * @param locked whether this slot is locked
     * @return marked item
     */
    public ItemStack markMenuSlot(ItemStack item, int slot, boolean locked) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keys.menuSlot(), PersistentDataType.INTEGER, slot);
        pdc.set(keys.lockedSlot(), PersistentDataType.INTEGER, locked ? 1 : 0);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Marks an item as a neutral filler.
     *
     * @param item item to mark
     * @return marked item
     */
    public ItemStack markFiller(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(keys.filler(), PersistentDataType.INTEGER, 1);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Reads a menu slot marker from an item.
     *
     * @param item item stack
     * @return menu slot when present
     */
    public Optional<Integer> menuSlot(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        Integer slot = item.getItemMeta().getPersistentDataContainer().get(keys.menuSlot(), PersistentDataType.INTEGER);
        return Optional.ofNullable(slot);
    }

    /**
     * Returns true when an item marks a locked equipment slot.
     *
     * @param item item stack
     * @return true for locked slot marker
     */
    public boolean isLockedSlot(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        Integer value = item.getItemMeta().getPersistentDataContainer().get(keys.lockedSlot(), PersistentDataType.INTEGER);
        return value != null && value == 1;
    }

    private List<Component> lore(PetInstance pet, PetDefinition definition) {
        java.util.ArrayList<Component> lines = new java.util.ArrayList<>();
        lines.add(miniMessage.deserialize("<gray>ID: <white>" + pet.instanceId()));
        lines.add(miniMessage.deserialize("<gray>Cấp: <green>" + pet.level()));
        lines.add(miniMessage.deserialize("<gray>Độ hiếm: " + RarityPalette.color(pet.rarity()) + pet.rarity().displayName()));
        definition.attributeBuffs().stream()
            .limit(4)
            .map(buff -> "<gray>" + buff.attributeKey() + ": <green>" + trim(buff.amount()) + " " + buff.operation())
            .map(miniMessage::deserialize)
            .forEach(lines::add);
        if (definition.attributeBuffs().size() > 4) {
            lines.add(miniMessage.deserialize("<dark_gray>+" + (definition.attributeBuffs().size() - 4) + " buff khác"));
        }
        return List.copyOf(lines);
    }

    private String trim(double value) {
        if (value == Math.rint(value)) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private void writePetData(PersistentDataContainer pdc, PetInstance pet) {
        pdc.set(keys.marker(), PersistentDataType.INTEGER, 1);
        pdc.set(keys.itemVersion(), PersistentDataType.INTEGER, ITEM_VERSION);
        pdc.set(keys.instanceId(), PersistentDataType.STRING, pet.instanceId().toString());
        pdc.set(keys.ownerId(), PersistentDataType.STRING, pet.ownerId().toString());
        pdc.set(keys.templateId(), PersistentDataType.STRING, pet.templateId());
        pdc.set(keys.level(), PersistentDataType.INTEGER, pet.level());
        pdc.set(keys.experience(), PersistentDataType.DOUBLE, pet.experience());
        pdc.set(keys.skillPoints(), PersistentDataType.INTEGER, pet.skillPoints());
        pdc.set(keys.unlockedSkills(), PersistentDataType.STRING, encodeSkills(pet.unlockedSkillIds()));
        pdc.set(keys.rarity(), PersistentDataType.STRING, pet.rarity().name());
        pdc.set(keys.skinId(), PersistentDataType.STRING, pet.skinId());
        if (pet.customName() == null) {
            pdc.remove(keys.customName());
        } else {
            pdc.set(keys.customName(), PersistentDataType.STRING, pet.customName());
        }
        pdc.set(keys.expiresAt(), PersistentDataType.LONG, pet.expiresAt() == null ? -1L : pet.expiresAt().toEpochMilli());
        pdc.set(keys.checksum(), PersistentDataType.STRING, integrityService.sign(pet));
    }

    private <T, Z> Z required(PersistentDataContainer pdc, org.bukkit.NamespacedKey key, PersistentDataType<T, Z> type, String label) {
        Z value = pdc.get(key, type);
        if (value == null) {
            throw new PetItemDecodeException("Pet item missing " + label);
        }
        return value;
    }

    private <T, Z> Z optional(PersistentDataContainer pdc, org.bukkit.NamespacedKey key, PersistentDataType<T, Z> type) {
        return pdc.get(key, type);
    }

    private String encodeSkills(Set<String> skills) {
        return skills.stream()
            .sorted()
            .collect(Collectors.joining(","));
    }

    private Set<String> decodeSkills(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(encoded.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}

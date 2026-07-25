package dev.sleepwithyourmom.linx.linpet.service;

import dev.sleepwithyourmom.linx.linpet.config.LinPetAttributeKeys;
import dev.sleepwithyourmom.linx.linpet.domain.buff.AggregatedBuffs;
import dev.sleepwithyourmom.linx.linpet.domain.buff.AttributeAggregateKey;
import dev.sleepwithyourmom.linx.linpet.domain.buff.AttributeOperation;
import dev.sleepwithyourmom.linx.linpet.domain.buff.PotionBuffDefinition;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Applies aggregate buffs to players and tracks the exact Lin'Pet modifiers to remove later.
 */
public class BuffApplier {
    private final JavaPlugin plugin;
    private final Map<UUID, AggregatedBuffs> activeBuffs = new ConcurrentHashMap<>();
    private final Map<UUID, Set<PotionEffectType>> appliedPotionEffects = new ConcurrentHashMap<>();

    /**
     * Creates a buff applier.
     *
     * @param plugin owning plugin
     */
    public BuffApplier(JavaPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }
        this.plugin = plugin;
    }

    /**
     * Applies a new aggregate, removing previous Lin'Pet modifiers first.
     *
     * @param player player
     * @param aggregate aggregate buffs
     */
    public void apply(Player player, AggregatedBuffs aggregate) {
        if (player == null || aggregate == null) {
            throw new IllegalArgumentException("player and aggregate must not be null");
        }
        remove(player);
        activeBuffs.put(player.getUniqueId(), aggregate);
        applyAttributes(player, aggregate.attributes());
        applyPotions(player, aggregate.potions());
    }

    /**
     * Removes Lin'Pet effects and modifiers from a player.
     *
     * @param player player
     */
    public void remove(Player player) {
        if (player == null) {
            return;
        }
        removePotionEffects(player);
        removeAttributeModifiers(player);
        activeBuffs.remove(player.getUniqueId());
    }

    /**
     * Returns active aggregate buffs for a player.
     *
     * @param playerId player UUID
     * @return active aggregate when present
     */
    public Optional<AggregatedBuffs> activeBuffs(UUID playerId) {
        return Optional.ofNullable(activeBuffs.get(playerId));
    }

    /**
     * Returns an active custom Lin'Pet attribute value for a player.
     *
     * @param playerId player UUID
     * @param attributeKey custom attribute key
     * @return summed attribute amount
     */
    public double customAttribute(UUID playerId, String attributeKey) {
        AggregatedBuffs buffs = activeBuffs.get(playerId);
        if (buffs == null) {
            return 0.0D;
        }
        return buffs.attributes().entrySet().stream()
            .filter(entry -> attributeKey.equals(entry.getKey().attributeKey()))
            .mapToDouble(Map.Entry::getValue)
            .sum();
    }

    /**
     * Applies health regeneration from the custom aggregate attribute.
     *
     * @param player online player
     */
    public void tickCustomEffects(Player player) {
        AggregatedBuffs buffs = activeBuffs.get(player.getUniqueId());
        if (buffs == null) {
            return;
        }
        double healPerSecond = buffs.attributes().entrySet().stream()
            .filter(entry -> "linpet:health_regen".equals(entry.getKey().attributeKey()))
            .mapToDouble(Map.Entry::getValue)
            .sum();
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (healPerSecond > 0.0D && player.getHealth() > 0.0D && maxHealth != null) {
            player.setHealth(Math.min(maxHealth.getValue(), player.getHealth() + healPerSecond));
        }
    }

    private void applyAttributes(Player player, Map<AttributeAggregateKey, Double> attributes) {
        for (Map.Entry<AttributeAggregateKey, Double> entry : attributes.entrySet()) {
            AttributeAggregateKey key = entry.getKey();
            if (LinPetAttributeKeys.isCustomAttribute(key.attributeKey())) {
                continue;
            }
            NamespacedKey attributeKey = NamespacedKey.fromString(key.attributeKey(), plugin);
            Attribute attribute = attributeKey == null ? null : Registry.ATTRIBUTE.get(attributeKey);
            if (attribute == null) {
                plugin.getLogger().warning("Skipping unknown attribute during apply: " + key.attributeKey());
                continue;
            }
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                plugin.getLogger().fine("Player " + player.getName() + " has no attribute instance for " + key.attributeKey());
                continue;
            }
            instance.addModifier(new AttributeModifier(
                modifierKey(key),
                entry.getValue(),
                toBukkitOperation(key.operation())
            ));
        }
    }

    private void applyPotions(Player player, Map<String, PotionBuffDefinition> potions) {
        Set<PotionEffectType> applied = new HashSet<>();
        for (PotionBuffDefinition buff : potions.values()) {
            NamespacedKey effectKey = NamespacedKey.fromString(buff.effectKey(), plugin);
            PotionEffectType effectType = effectKey == null ? null : Registry.MOB_EFFECT.get(effectKey);
            if (effectType == null) {
                plugin.getLogger().warning("Skipping unknown potion effect during apply: " + buff.effectKey());
                continue;
            }
            int durationTicks = buff.durationSeconds() == -1 ? 20 * 60 * 2 : Math.max(1, buff.durationSeconds() * 20);
            player.addPotionEffect(new PotionEffect(effectType, durationTicks, buff.amplifier(), true, false, false));
            applied.add(effectType);
        }
        appliedPotionEffects.put(player.getUniqueId(), applied);
    }

    private void removePotionEffects(Player player) {
        Set<PotionEffectType> effects = appliedPotionEffects.remove(player.getUniqueId());
        if (effects == null) {
            return;
        }
        effects.forEach(player::removePotionEffect);
    }

    private void removeAttributeModifiers(Player player) {
        for (Attribute attribute : Registry.ATTRIBUTE) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            for (AttributeModifier modifier : Set.copyOf(instance.getModifiers())) {
                NamespacedKey key = modifier.getKey();
                if ("linpet".equals(key.getNamespace()) && key.getKey().startsWith("buff_")) {
                    instance.removeModifier(modifier);
                }
            }
        }
    }

    private NamespacedKey modifierKey(AttributeAggregateKey key) {
        String value = "buff_" + key.attributeKey()
            .replace(':', '_')
            .replace('.', '_')
            .replace('/', '_')
            .toLowerCase(Locale.ROOT)
            + "_" + key.operation().name().toLowerCase(Locale.ROOT);
        return new NamespacedKey(plugin, value);
    }

    private AttributeModifier.Operation toBukkitOperation(AttributeOperation operation) {
        return switch (operation) {
            case ADD_NUMBER -> AttributeModifier.Operation.ADD_NUMBER;
            case ADD_SCALAR -> AttributeModifier.Operation.ADD_SCALAR;
            case MULTIPLY_SCALAR_1 -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
        };
    }
}

package dev.sleepwithyourmom.linx.linpet.listener;

import dev.sleepwithyourmom.linx.linpet.service.BuffApplier;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;

/**
 * Applies event-driven Lin'Pet custom attributes that cannot be represented as vanilla attributes.
 */
public class CustomAttributeListener implements Listener {
    private static final Set<String> MAGIC_CAUSES = Set.of(
        "MAGIC",
        "DRAGON_BREATH",
        "SONIC_BOOM"
    );
    private static final Set<String> FIRE_CAUSES = Set.of(
        "FIRE",
        "FIRE_TICK",
        "LAVA",
        "HOT_FLOOR"
    );

    private final BuffApplier buffApplier;

    /**
     * Creates a custom attribute listener.
     *
     * @param buffApplier buff applier
     */
    public CustomAttributeListener(BuffApplier buffApplier) {
        if (buffApplier == null) {
            throw new IllegalArgumentException("buffApplier must not be null");
        }
        this.buffApplier = buffApplier;
    }

    /**
     * Applies outgoing critical and lifesteal attributes.
     *
     * @param event damage event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOutgoingDamage(EntityDamageByEntityEvent event) {
        Player attacker = player(event.getDamager());
        if (attacker == null) {
            return;
        }
        UUID playerId = attacker.getUniqueId();
        double criticalChance = percent(playerId, "linpet:critical_chance");
        if (roll(criticalChance)) {
            double criticalDamage = Math.max(150.0D, percent(playerId, "linpet:critical_damage"));
            event.setDamage(event.getDamage() * (criticalDamage / 100.0D));
        }
        double lifesteal = percent(playerId, "linpet:lifesteal");
        if (lifesteal > 0.0D) {
            double heal = event.getFinalDamage() * (lifesteal / 100.0D);
            AttributeInstance maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null && heal > 0.0D && attacker.getHealth() > 0.0D) {
                attacker.setHealth(Math.min(maxHealth.getValue(), attacker.getHealth() + heal));
            }
        }
    }

    /**
     * Applies incoming defensive custom attributes.
     *
     * @param event damage event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIncomingDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (roll(percent(playerId, "linpet:dodge"))) {
            event.setCancelled(true);
            return;
        }
        if (roll(percent(playerId, "linpet:block_chance"))) {
            event.setDamage(event.getDamage() * 0.5D);
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setDamage(reduced(event.getDamage(), percent(playerId, "linpet:fall_damage_reduction")));
        } else if (MAGIC_CAUSES.contains(event.getCause().name())) {
            event.setDamage(reduced(event.getDamage(), percent(playerId, "linpet:magic_resist")));
        } else if (FIRE_CAUSES.contains(event.getCause().name())) {
            event.setDamage(reduced(event.getDamage(), percent(playerId, "linpet:fire_resist")));
        } else if (event.getCause() == EntityDamageEvent.DamageCause.POISON) {
            event.setDamage(reduced(event.getDamage(), percent(playerId, "linpet:poison_resist")));
        } else if (event.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            event.setDamage(reduced(event.getDamage(), percent(playerId, "linpet:wither_resist")));
        }

        if (event instanceof EntityDamageByEntityEvent byEntityEvent) {
            Player attacker = player(byEntityEvent.getDamager());
            double thorns = percent(playerId, "linpet:thorns");
            if (attacker != null && thorns > 0.0D) {
                attacker.damage(event.getFinalDamage() * (thorns / 100.0D), player);
            }
        }
    }

    /**
     * Applies poison and wither resistance before effects are added.
     *
     * @param event potion effect event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getNewEffect() == null) {
            return;
        }
        String effectKey = event.getNewEffect().getType().getKey().getKey();
        if ("poison".equals(effectKey) && roll(percent(player.getUniqueId(), "linpet:poison_resist"))) {
            event.setCancelled(true);
        }
        if ("wither".equals(effectKey) && roll(percent(player.getUniqueId(), "linpet:wither_resist"))) {
            event.setCancelled(true);
        }
    }

    private Player player(Entity entity) {
        return entity instanceof Player player ? player : null;
    }

    private double percent(UUID playerId, String key) {
        return Math.max(0.0D, buffApplier.customAttribute(playerId, key));
    }

    private boolean roll(double percent) {
        return percent > 0.0D && ThreadLocalRandom.current().nextDouble(100.0D) < percent;
    }

    private double reduced(double damage, double percent) {
        return damage * Math.max(0.0D, 1.0D - Math.min(100.0D, percent) / 100.0D);
    }
}

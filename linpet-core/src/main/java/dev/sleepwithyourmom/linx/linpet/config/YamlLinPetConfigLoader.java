package dev.sleepwithyourmom.linx.linpet.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.sleepwithyourmom.linx.linpet.api.model.PetRarity;
import dev.sleepwithyourmom.linx.linpet.domain.auction.AuctionRules;
import dev.sleepwithyourmom.linx.linpet.domain.buff.AttributeBuffDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.buff.AttributeOperation;
import dev.sleepwithyourmom.linx.linpet.domain.buff.BuffScalingConfig;
import dev.sleepwithyourmom.linx.linpet.domain.buff.PotionBuffDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.pet.PetDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.rank.RankDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.shop.ShopEntry;
import dev.sleepwithyourmom.linx.linpet.domain.skill.SkillDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.skill.SkillTreeDefinition;
import dev.sleepwithyourmom.linx.linpet.domain.skill.SkillTreeValidator;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads, normalizes, and validates Lin'Pet YAML configuration files.
 */
public class YamlLinPetConfigLoader {
    private static final int SCHEMA_VERSION = 1;

    private final JavaPlugin plugin;
    private final SkillTreeValidator skillTreeValidator = new SkillTreeValidator();

    /**
     * Creates a config loader.
     *
     * @param plugin owning plugin
     */
    public YamlLinPetConfigLoader(JavaPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }
        this.plugin = plugin;
    }

    /**
     * Loads the complete configuration snapshot from disk.
     *
     * @return validated config snapshot
     */
    public LinPetConfig load() {
        ensureDefault("config.yml");
        ensureDefault("pets.yml");
        ensureDefault("ranks.yml");
        ensureDefault("shop.yml");
        ensureDefault("skilltree.yml");
        ensureDefault("skins.yml");
        ensureDefault("quests.yml");
        ensureDefault("lang/vi.yml");

        FileConfiguration config = loadYaml("config.yml");
        LinPetSettings settings = loadSettings(config);
        Map<String, PetDefinition> pets = loadPets(loadVersionedYaml("pets.yml"));
        Map<String, RankDefinition> ranks = loadRanks(loadVersionedYaml("ranks.yml"));
        Map<String, ShopEntry> shop = loadShop(loadVersionedYaml("shop.yml"));
        Map<String, SkillTreeDefinition> skillTrees = loadSkillTrees(loadVersionedYaml("skilltree.yml"));
        SkinRegistry skinRegistry = loadSkins(loadVersionedYaml("skins.yml"));
        skillTreeValidator.validate(skillTrees);
        return new LinPetConfig(settings, pets, ranks, shop, skillTrees, skinRegistry);
    }

    private void ensureDefault(String resourcePath) {
        File target = new File(plugin.getDataFolder(), resourcePath);
        if (!target.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private FileConfiguration loadVersionedYaml(String relativePath) {
        FileConfiguration yaml = loadYaml(relativePath);
        int version = yaml.getInt("schema-version", -1);
        if (version != SCHEMA_VERSION) {
            throw new ConfigValidationException(relativePath + ": schema-version must be " + SCHEMA_VERSION + ", found " + version);
        }
        return yaml;
    }

    private FileConfiguration loadYaml(String relativePath) {
        File file = new File(plugin.getDataFolder(), relativePath);
        return YamlConfiguration.loadConfiguration(file);
    }

    private LinPetSettings loadSettings(FileConfiguration yaml) {
        ConfigurationSection database = section(yaml, "settings.database", "config.yml");
        DatabaseSettings databaseSettings = new DatabaseSettings(
            database.getString("type", "sqlite"),
            database.getString("host", "localhost"),
            database.getInt("port", 3306),
            database.getString("name", "linpet"),
            database.getString("user", ""),
            database.getString("password", ""),
            database.getInt("pool-size", 10)
        );
        BuffScalingConfig scaling = new BuffScalingConfig(
            yaml.getBoolean("buff-scaling.enabled", true),
            yaml.getDouble("buff-scaling.scaling-per-level", 0.02D),
            yaml.getInt("buff-scaling.max-level", 100)
        );
        AuctionRules auctionRules = new AuctionRules(
            BigDecimal.valueOf(yaml.getDouble("auction.min-bid-increment", 1.0D)),
            BigDecimal.valueOf(yaml.getDouble("auction.tax-percent", 5.0D)),
            true
        );
        return new LinPetSettings(
            yaml.getString("settings.language", "auto"),
            yaml.getBoolean("settings.enable-folia", true),
            databaseSettings,
            yaml.getInt("settings.default-pet-limit", 5),
            yaml.getInt("settings.max-slots-per-player", 20),
            yaml.getInt("slots.default", 3),
            yaml.getDouble("slots.buy-price-per-slot", 5000.0D),
            Set.copyOf(yaml.getStringList("blacklist.worlds")),
            scaling,
            auctionRules,
            yaml.getStringList("auction.system-pets"),
            yaml.getInt("auction.duration-minutes", 30),
            BigDecimal.valueOf(yaml.getDouble("auction.starting-price", 1000000.0D)),
            yaml.getBoolean("auction.allow-player-auction", true),
            yaml.getInt("auction.cooldown-between-auctions", 3600)
        );
    }

    private Map<String, PetDefinition> loadPets(FileConfiguration yaml) {
        Map<String, PetDefinition> result = new LinkedHashMap<>();
        for (String petId : yaml.getKeys(false)) {
            if ("schema-version".equals(petId)) {
                continue;
            }
            ConfigurationSection section = section(yaml, petId, "pets.yml");
            String rarityText = requireString(section, "rarity", "pets.yml:" + petId);
            PetRarity rarity = PetRarity.fromConfig(rarityText)
                .orElseThrow(() -> new ConfigValidationException("pets.yml:" + petId + ".rarity is invalid: " + rarityText));
            List<PotionBuffDefinition> potionBuffs = readPotionBuffs(section, "base-buffs", "pets.yml:" + petId);
            List<AttributeBuffDefinition> attributes = readAttributeBuffs(section, "attributes", "pets.yml:" + petId);
            result.put(petId, new PetDefinition(
                petId,
                requireString(section, "display-name", "pets.yml:" + petId),
                rarity,
                requireString(section, "default-skin", "pets.yml:" + petId),
                potionBuffs,
                attributes,
                section.getStringList("special-abilities")
            ));
        }
        if (result.isEmpty()) {
            throw new ConfigValidationException("pets.yml must define at least one pet");
        }
        return result;
    }

    private List<PotionBuffDefinition> readPotionBuffs(ConfigurationSection section, String path, String context) {
        List<PotionBuffDefinition> result = new ArrayList<>();
        for (Map<?, ?> map : mapList(section, path, context)) {
            String type = stringValue(map, "type", context + "." + path);
            if (!"POTION".equalsIgnoreCase(type)) {
                continue;
            }
            String effect = normalizeEffectKey(stringValue(map, "effect", context + "." + path));
            int amplifier = intValue(map, "amplifier", context + "." + path);
            int duration = intValue(map, "duration", context + "." + path);
            validatePotion(effect, context + "." + path);
            result.add(new PotionBuffDefinition(effect, amplifier, duration));
        }
        return result;
    }

    private List<AttributeBuffDefinition> readAttributeBuffs(ConfigurationSection section, String path, String context) {
        List<AttributeBuffDefinition> result = new ArrayList<>();
        for (Map<?, ?> map : mapList(section, path, context)) {
            String attribute = normalizeAttributeKey(stringValue(map, "attribute", context + "." + path));
            double amount = doubleValue(map, "amount", context + "." + path);
            AttributeOperation operation = AttributeOperation.parse(stringValue(map, "operation", context + "." + path));
            validateAttribute(attribute, context + "." + path);
            result.add(new AttributeBuffDefinition(attribute, amount, operation));
        }
        return result;
    }

    private Map<String, RankDefinition> loadRanks(FileConfiguration yaml) {
        ConfigurationSection ranks = section(yaml, "ranks", "ranks.yml");
        Map<String, RankDefinition> result = new LinkedHashMap<>();
        for (String rankId : ranks.getKeys(false)) {
            ConfigurationSection rank = section(ranks, rankId, "ranks.yml:ranks");
            result.put(rankId, new RankDefinition(
                rankId,
                rank.getString("display-name", rankId),
                rank.getInt("slots", 0),
                rank.getDouble("auction-fee-discount-percent", 0.0D),
                rank.getBoolean("bypass-blacklist", false)
            ));
        }
        return result;
    }

    private Map<String, ShopEntry> loadShop(FileConfiguration yaml) {
        ConfigurationSection items = section(yaml, "items", "shop.yml");
        Map<String, ShopEntry> result = new LinkedHashMap<>();
        for (String petId : items.getKeys(false)) {
            ConfigurationSection item = section(items, petId, "shop.yml:items");
            result.put(petId, new ShopEntry(
                petId,
                BigDecimal.valueOf(item.getDouble("price", 0.0D)),
                item.getInt("daily-limit", 0)
            ));
        }
        return result;
    }

    private Map<String, SkillTreeDefinition> loadSkillTrees(FileConfiguration yaml) {
        ConfigurationSection trees = section(yaml, "trees", "skilltree.yml");
        Map<String, SkillTreeDefinition> result = new LinkedHashMap<>();
        for (String petId : trees.getKeys(false)) {
            ConfigurationSection petTree = section(trees, petId, "skilltree.yml:trees");
            ConfigurationSection skills = section(petTree, "skills", "skilltree.yml:trees." + petId);
            Map<String, SkillDefinition> skillMap = new LinkedHashMap<>();
            for (String skillId : skills.getKeys(false)) {
                ConfigurationSection skill = section(skills, skillId, "skilltree.yml:trees." + petId + ".skills");
                skillMap.put(skillId, new SkillDefinition(
                    skillId,
                    skill.getString("branch", "special"),
                    requireString(skill, "display-name", "skilltree.yml:" + petId + "." + skillId),
                    skill.getString("description", ""),
                    skill.getInt("cost", 1),
                    skill.getInt("required-level", 1),
                    Set.copyOf(skill.getStringList("prerequisites")),
                    readPotionBuffs(skill, "buffs", "skilltree.yml:" + petId + "." + skillId),
                    readAttributeBuffs(skill, "attributes", "skilltree.yml:" + petId + "." + skillId)
                ));
            }
            result.put(petId, new SkillTreeDefinition(petId, skillMap));
        }
        return result;
    }

    private SkinRegistry loadSkins(FileConfiguration yaml) {
        ConfigurationSection skins = section(yaml, "skins", "skins.yml");
        Map<String, String> result = new LinkedHashMap<>();
        for (String skinId : skins.getKeys(false)) {
            String value = requireString(skins, skinId, "skins.yml:skins");
            validateSkinReference(value, "skins.yml:skins." + skinId);
            result.put(skinId, value);
        }
        return new SkinRegistry(result);
    }

    private ConfigurationSection section(ConfigurationSection root, String path, String file) {
        ConfigurationSection section = root.getConfigurationSection(path);
        if (section == null) {
            throw new ConfigValidationException(file + " missing section '" + path + "'");
        }
        return section;
    }

    private String requireString(ConfigurationSection section, String path, String context) {
        String value = section.getString(path);
        if (value == null || value.isBlank()) {
            throw new ConfigValidationException(context + "." + path + " must be a non-empty string");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<?, ?>> mapList(ConfigurationSection section, String path, String context) {
        List<?> list = section.getList(path, List.of());
        List<Map<?, ?>> result = new ArrayList<>();
        for (Object value : list) {
            if (!(value instanceof Map<?, ?> map)) {
                throw new ConfigValidationException(context + "." + path + " must contain map entries");
            }
            result.add((Map<?, ?>) map);
        }
        return result;
    }

    private String stringValue(Map<?, ?> map, String key, String context) {
        Object value = map.get(key);
        if (!(value instanceof String string) || string.isBlank()) {
            throw new ConfigValidationException(context + "." + key + " must be a non-empty string");
        }
        return string;
    }

    private int intValue(Map<?, ?> map, String key, String context) {
        Object value = map.get(key);
        if (!(value instanceof Number number)) {
            throw new ConfigValidationException(context + "." + key + " must be a number");
        }
        return number.intValue();
    }

    private double doubleValue(Map<?, ?> map, String key, String context) {
        Object value = map.get(key);
        if (!(value instanceof Number number)) {
            throw new ConfigValidationException(context + "." + key + " must be a number");
        }
        double amount = number.doubleValue();
        if (!Double.isFinite(amount)) {
            throw new ConfigValidationException(context + "." + key + " must be finite");
        }
        return amount;
    }

    private String normalizeAttributeKey(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (!value.contains(":")) {
            value = "minecraft:" + value;
        }
        return value.replace("minecraft:generic.", "minecraft:");
    }

    private String normalizeEffectKey(String raw) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return value.contains(":") ? value : "minecraft:" + value;
    }

    private void validateAttribute(String key, String context) {
        if (LinPetAttributeKeys.isCustomAttribute(key)) {
            return;
        }
        NamespacedKey namespacedKey = NamespacedKey.fromString(key, plugin);
        if (namespacedKey == null || Registry.ATTRIBUTE.get(namespacedKey) == null) {
            throw new ConfigValidationException(context + " unknown attribute '" + key + "'");
        }
    }

    private void validatePotion(String key, String context) {
        NamespacedKey namespacedKey = NamespacedKey.fromString(key, plugin);
        if (namespacedKey == null || Registry.MOB_EFFECT.get(namespacedKey) == null) {
            throw new ConfigValidationException(context + " unknown potion effect '" + key + "'");
        }
    }

    private void validateSkinReference(String value, String context) {
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return;
        }
        if (value.startsWith("base64:")) {
            String encoded = value.substring("base64:".length());
            try {
                String json = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
                JsonObject object = JsonParser.parseString(json).getAsJsonObject();
                if (!object.has("textures")) {
                    throw new ConfigValidationException(context + " base64 texture missing textures object");
                }
                return;
            } catch (IllegalArgumentException | IllegalStateException ex) {
                throw new ConfigValidationException(context + " base64 texture is invalid", ex);
            }
        }
        if (value.length() > 16 && !value.matches("[A-Za-z0-9_]{1,16}")) {
            throw new ConfigValidationException(context + " must be a player name, URL, or base64: payload");
        }
    }
}

package dev.sleepwithyourmom.linx.linpet.service;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads localized messages and renders them through Adventure.
 */
public class MessageService {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final AtomicReference<FileConfiguration> messages = new AtomicReference<>();

    /**
     * Creates a message service.
     *
     * @param plugin owning plugin
     */
    public MessageService(JavaPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }
        this.plugin = plugin;
    }

    /**
     * Reloads the default Vietnamese language file.
     */
    public void reload() {
        File file = new File(plugin.getDataFolder(), "lang/vi.yml");
        messages.set(YamlConfiguration.loadConfiguration(file));
    }

    /**
     * Sends a message to a command sender.
     *
     * @param sender target sender
     * @param key message key
     */
    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    /**
     * Sends a message with placeholder values.
     *
     * @param sender target sender
     * @param key message key
     * @param placeholders placeholder map without braces
     */
    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(component(key, placeholders));
    }

    /**
     * Builds a component for a message key.
     *
     * @param key message key
     * @param placeholders placeholder map without braces
     * @return rendered component
     */
    public Component component(String key, Map<String, String> placeholders) {
        String raw = messages.get().getString(key, "<red>Missing message: " + key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", miniMessage.escapeTags(entry.getValue()));
        }
        if (raw.indexOf('&') >= 0 && raw.indexOf('<') < 0) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
        }
        return miniMessage.deserialize(raw);
    }
}

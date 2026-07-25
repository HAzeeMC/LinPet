package dev.sleepwithyourmom.linx.linpet.economy;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Resolves an available economy provider.
 */
public class EconomyResolver {
    private final JavaPlugin plugin;

    /**
     * Creates an economy resolver.
     *
     * @param plugin owning plugin
     */
    public EconomyResolver(JavaPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }
        this.plugin = plugin;
    }

    /**
     * Detects a configured economy provider.
     *
     * @return provider, or a no-op provider when none is available
     */
    public EconomyProvider resolve() {
        EconomyProvider vault = resolveVault();
        if (vault.available()) {
            plugin.getLogger().info("Using Vault economy provider.");
            return vault;
        }
        plugin.getLogger().warning("No supported economy provider found; shop and auction purchases will be blocked.");
        return new NoEconomyProvider();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private EconomyProvider resolveVault() {
        try {
            Class economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider registration = Bukkit.getServicesManager().getRegistration(economyClass);
            if (registration == null || registration.getProvider() == null) {
                return new NoEconomyProvider();
            }
            return new VaultEconomyProvider(registration.getProvider());
        } catch (ClassNotFoundException ex) {
            return new NoEconomyProvider();
        }
    }
}

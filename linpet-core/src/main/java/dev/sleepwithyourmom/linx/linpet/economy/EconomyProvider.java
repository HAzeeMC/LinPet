package dev.sleepwithyourmom.linx.linpet.economy;

import java.math.BigDecimal;
import org.bukkit.OfflinePlayer;

/**
 * Internal economy abstraction used by shop and auction services.
 */
public interface EconomyProvider {
    /**
     * Returns provider name.
     *
     * @return provider name
     */
    String name();

    /**
     * Returns whether this provider can process money operations.
     *
     * @return true when available
     */
    boolean available();

    /**
     * Checks if a player has at least an amount.
     *
     * @param player player
     * @param amount amount
     * @return true when enough balance exists
     */
    boolean has(OfflinePlayer player, BigDecimal amount);

    /**
     * Withdraws money from a player.
     *
     * @param player player
     * @param amount amount
     * @return transaction result
     */
    EconomyResult withdraw(OfflinePlayer player, BigDecimal amount);

    /**
     * Deposits money to a player.
     *
     * @param player player
     * @param amount amount
     * @return transaction result
     */
    EconomyResult deposit(OfflinePlayer player, BigDecimal amount);
}

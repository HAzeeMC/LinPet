package dev.sleepwithyourmom.linx.linpet.economy;

import java.math.BigDecimal;
import org.bukkit.OfflinePlayer;

/**
 * Economy provider used when no supported economy plugin is installed.
 */
public class NoEconomyProvider implements EconomyProvider {
    @Override
    public String name() {
        return "none";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public boolean has(OfflinePlayer player, BigDecimal amount) {
        return false;
    }

    @Override
    public EconomyResult withdraw(OfflinePlayer player, BigDecimal amount) {
        return EconomyResult.failure("Không tìm thấy provider kinh tế.");
    }

    @Override
    public EconomyResult deposit(OfflinePlayer player, BigDecimal amount) {
        return EconomyResult.failure("Không tìm thấy provider kinh tế.");
    }
}

package dev.sleepwithyourmom.linx.linpet.economy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import org.bukkit.OfflinePlayer;

/**
 * Reflection adapter for Vault economy without a compile-time Vault dependency.
 */
public class VaultEconomyProvider implements EconomyProvider {
    private final Object economy;
    private final Method hasMethod;
    private final Method withdrawMethod;
    private final Method depositMethod;
    private final Method responseSuccessMethod;
    private final Method responseErrorMessageMethod;

    /**
     * Creates a Vault economy provider.
     *
     * @param economy Vault economy service instance
     */
    public VaultEconomyProvider(Object economy) {
        if (economy == null) {
            throw new IllegalArgumentException("economy must not be null");
        }
        try {
            this.economy = economy;
            Class<?> economyClass = economy.getClass();
            this.hasMethod = economyClass.getMethod("has", OfflinePlayer.class, double.class);
            this.withdrawMethod = economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            this.depositMethod = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            Class<?> responseClass = withdrawMethod.getReturnType();
            this.responseSuccessMethod = responseClass.getMethod("transactionSuccess");
            this.responseErrorMessageMethod = responseClass.getMethod("errorMessage");
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException("Vault economy service has incompatible API", ex);
        }
    }

    @Override
    public String name() {
        return "Vault";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public boolean has(OfflinePlayer player, BigDecimal amount) {
        try {
            return (boolean) hasMethod.invoke(economy, player, amount.doubleValue());
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Vault balance check failed", ex);
        }
    }

    @Override
    public EconomyResult withdraw(OfflinePlayer player, BigDecimal amount) {
        return transaction(withdrawMethod, player, amount);
    }

    @Override
    public EconomyResult deposit(OfflinePlayer player, BigDecimal amount) {
        return transaction(depositMethod, player, amount);
    }

    private EconomyResult transaction(Method method, OfflinePlayer player, BigDecimal amount) {
        if (amount.signum() < 0) {
            return EconomyResult.failure("Số tiền không được âm.");
        }
        try {
            Object response = method.invoke(economy, player, amount.doubleValue());
            boolean success = (boolean) responseSuccessMethod.invoke(response);
            String message = String.valueOf(responseErrorMessageMethod.invoke(response));
            return success ? EconomyResult.ok() : EconomyResult.failure(message);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Vault transaction failed", ex);
        }
    }
}

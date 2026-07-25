package dev.sleepwithyourmom.linx.linpet.economy;

/**
 * Result of an economy operation.
 *
 * @param success whether the operation succeeded
 * @param message provider message or failure reason
 */
public record EconomyResult(boolean success, String message) {
    /**
     * Creates a success result.
     *
     * @return success result
     */
    public static EconomyResult ok() {
        return new EconomyResult(true, "");
    }

    /**
     * Creates a failure result.
     *
     * @param message failure reason
     * @return failure result
     */
    public static EconomyResult failure(String message) {
        return new EconomyResult(false, message);
    }
}

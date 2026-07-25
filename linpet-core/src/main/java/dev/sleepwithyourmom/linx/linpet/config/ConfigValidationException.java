package dev.sleepwithyourmom.linx.linpet.config;

/**
 * Signals that a Lin'Pet configuration file is structurally invalid.
 */
public class ConfigValidationException extends RuntimeException {
    /**
     * Creates a validation exception.
     *
     * @param message validation message with enough context for administrators
     */
    public ConfigValidationException(String message) {
        super(message);
    }

    /**
     * Creates a validation exception with a cause.
     *
     * @param message validation message with enough context for administrators
     * @param cause underlying failure
     */
    public ConfigValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

package dev.sleepwithyourmom.linx.linpet.domain.buff;

import java.util.Locale;

/**
 * Domain-level attribute operation names supported by Lin'Pet configuration.
 */
public enum AttributeOperation {
    /**
     * Adds the configured number directly.
     */
    ADD_NUMBER,

    /**
     * Adds a scalar value based on the original value.
     */
    ADD_SCALAR,

    /**
     * Multiplies the final scalar value.
     */
    MULTIPLY_SCALAR_1;

    /**
     * Parses an operation from YAML.
     *
     * @param value operation value
     * @return parsed operation
     * @throws IllegalArgumentException when the value is unsupported
     */
    public static AttributeOperation parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("attribute operation must not be blank");
        }
        return AttributeOperation.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}

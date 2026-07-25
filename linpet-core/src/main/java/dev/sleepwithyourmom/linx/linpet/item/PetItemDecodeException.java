package dev.sleepwithyourmom.linx.linpet.item;

/**
 * Signals that an item looked like a Lin'Pet pet but failed validation.
 */
public class PetItemDecodeException extends RuntimeException {
    /**
     * Creates a decode exception.
     *
     * @param message reason the item is invalid
     */
    public PetItemDecodeException(String message) {
        super(message);
    }

    /**
     * Creates a decode exception with cause.
     *
     * @param message reason the item is invalid
     * @param cause underlying parse failure
     */
    public PetItemDecodeException(String message, Throwable cause) {
        super(message, cause);
    }
}

package dev.sleepwithyourmom.linx.linpet.repository;

/**
 * Unchecked wrapper for repository failures with operation context.
 */
public class RepositoryException extends RuntimeException {
    /**
     * Creates a repository exception.
     *
     * @param message operation context
     * @param cause underlying SQL failure
     */
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}

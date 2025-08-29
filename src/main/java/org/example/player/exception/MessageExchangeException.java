package org.example.player.exception;

/**
 * Exception thrown when message exchange operations fail. This exception wraps various message exchange related
 * errors that can occur during message sending, receiving, or channel operations.
 *
 * Responsibilities:
 * - Represent message exchange failures in a consistent way
 * - Provide meaningful error messages for debugging
 * - Support exception chaining for root cause analysis -- how?
 */
public class MessageExchangeException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exchange exception with the specified message.
     *
     * @param message the detail message explaining the exchange failure
     */
    public MessageExchangeException(String message) {
        super(message);
    }

    /**
     * Creates a new exchange exception with the specified message and cause.
     *
     * @param message the detail message explaining the exchange failure
     * @param cause the underlying cause of this exception
     */
    public MessageExchangeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exchange exception wrapping another exception.
     *
     * @param cause the underlying cause of this exception
     */
    public MessageExchangeException(Throwable cause) {
        super(cause);
    }
}

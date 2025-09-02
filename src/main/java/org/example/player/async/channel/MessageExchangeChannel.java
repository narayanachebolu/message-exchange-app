package org.example.player.async.channel;

import org.example.player.message.Message;

import java.util.concurrent.CompletableFuture;

/**
 * Defines the asynchronous communication contract between players. This interface provides non-blocking message
 * operations using CompletableFuture for better scalability and responsiveness.
 *
 * Responsibilities:
 * - Abstract asynchronous communication mechanism between players
 * - Support both same-process and cross-process communication
 * - Provide non-blocking message delivery through futures
 * - Handle connection lifecycle (connect, disconnect, cleanup)
 * - Enable reactive programming patterns for message processing
 */
public interface MessageExchangeChannel extends AutoCloseable {
    /**
     * Establishes connection for message exchange asynchronously. This method should be called before sending or
     * receiving messages.
     *
     * @return CompletableFuture that completes when connection is established.
     */
    CompletableFuture<Void> connect();

    /**
     * Sends a message through this channel asynchronously.
     *
     * @param message: the message to send.
     * @return CompletableFuture that completes when message is sent.
     */
    CompletableFuture<Void> sendMessage(Message message);

    /**
     * Receive the next message from this channel asynchronously. The returned future completes when a message
     * become available.
     *
     * @return CompletableFuture containing the received message.
     */
    CompletableFuture<Void> receiveMessage();

    /**
     * Check if the channel is currently connected and operational.
     *
     * @return true if connected, false otherwise.
     */
    boolean isConnected();

    /**
     * Sets a message handler that will be called asynchronously when message arrive. This enables reactive message
     * processing without explicit polling.
     *
     * @param messageHandler: then message handler to call for incoming messages.
     */
    void setMessageHandler(MessageHandler messageHandler);

    /**
     * Gracefully closes the message exchange channel and release resources. This method is idempotent - safe to call
     * multiple times.
     */
    @Override
    void close();

    /**
     * Functional interface for handling incoming message asynchronously.
     */
    @FunctionalInterface
    interface MessageHandler {
        /**
         * Handles an incoming message asynchronously.
         *
         * @param message the received message.
         * @return CompletableFeature that completes when message handling is done.
         */
        CompletableFuture<Void> handleMessage(Message message);
    }
}

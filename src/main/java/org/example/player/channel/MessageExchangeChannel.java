package org.example.player.channel;

import org.example.player.exception.MessageExchangeException;
import org.example.player.message.Message;

/**
 * Defines the message exchange contract between players. This interface abstracts the underlying message exchange
 * mechanism, allowing for different implementations (in-process, socket-based, etc.).
 *
 * Responsibilities:
 * - Abstract message exchange mechanism between players.
 * - Support both same-process and cross-process message exchange.
 * - Provide reliable message delivery.
 * - Handle connection lifecycle (connect, disconnect, cleanup).
 */
public interface MessageExchangeChannel extends AutoCloseable {
    /**
     * Establishes connection for message exchange. This method should be called before sending or receiving messages.
     *
     * @throws MessageExchangeException if connection cannot be established
     */
    void connect() throws MessageExchangeException;

    /**
     * Sends a message through this channel.
     *
     * @param message the message to send
     * @throws MessageExchangeException if the message cannot be sent
     */
    void sendMessage(Message message) throws MessageExchangeException;

    /**
     * Receives the next message from this channel. This method blocks until a message is available.
     *
     * @return the received message
     * @throws MessageExchangeException if message cannot be received
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    Message receiveMessage() throws MessageExchangeException, InterruptedException;

    /**
     * Checks if this channel is currently connected and operational.
     *
     * @return true if connected, false otherwise
     */
    boolean isConnected();

    /**
     * Gracefully closes the message exchange channel and releases resources.
     * This method is idempotent - safe to call multiple times.
     */
    @Override
    void close();
}

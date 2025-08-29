package org.example.player.message;

import java.io.Serializable;

/**
 * This interface defines the contract for a message that can be sent between players.
 *
 * Responsibilities:
 * - Ensure messages are serializable for cross-process message exchange
 * - Provide access to message content and metadata
 */
public interface Message extends Serializable {

    /**
     * Get the content of the message.
     *
     * @return the message content as String.
     */
    String getContent();

    /**
     * Get the sender's identifier.
     *
     * @return the identifier of the player who has sent this message.
     */
    String getSenderId();

    /**
     * Get the recipient's identifier.
     *
     * @return the identifier of the recipient of this message.
     */
    String getRecipientId();

    /**
     * Gets the sequence number of this message from the sender.
     *
     * @return the sequence number of this message.
     */
    int getSequenceNumber();
}

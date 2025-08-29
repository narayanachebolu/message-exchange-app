package org.example.player.message;

import java.io.Serial;
import java.util.Objects;

/**
 * Simple implementation of the Message interface. Represents a basic message with content, sender/recipient IDs, and
 * sequence number.
 *
 * Responsibilities:
 * - Define the structure for messages exchanged between players
 * - Provide immutable message data
 * - Support serialization for cross-process message exchange
 * - Implement proper equals/hashCode for message comparison
 */
public class SimpleMessage implements Message {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String content;
    private final String senderId;
    private final String recipientId;
    private final int sequenceNumber;

    /**
     * Creates a new simple message.
     *
     * @param content the message content
     * @param senderId the ID of the sender
     * @param recipientId the ID of the recipient
     * @param sequenceNumber the sequence number of this message
     */
    public SimpleMessage(String content, String senderId, String recipientId, int sequenceNumber) {
        this.content = Objects.requireNonNull(content, "Content cannot be null");
        this.senderId = Objects.requireNonNull(senderId, "Sender ID cannot be null");
        this.recipientId = Objects.requireNonNull(recipientId, "Recipient ID cannot be null");
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public String getSenderId() {
        return senderId;
    }

    @Override
    public String getRecipientId() {
        return recipientId;
    }

    @Override
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SimpleMessage that = (SimpleMessage) obj;
        return sequenceNumber == that.sequenceNumber &&
                Objects.equals(content, that.content) &&
                Objects.equals(senderId, that.senderId) &&
                Objects.equals(recipientId, that.recipientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, senderId, recipientId, sequenceNumber);
    }

    @Override
    public String toString() {
        return String.format("SimpleMessage{content='%s', from='%s', to='%s', seq=%d}",
                content, senderId, recipientId, sequenceNumber);
    }
}

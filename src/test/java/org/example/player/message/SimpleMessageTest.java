package org.example.player.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SimpleMessage class.
 * These tests verify message creation, validation, and behavior.
 */
class SimpleMessageTest {

    @Test
    @DisplayName("Simple message should be created with all fields")
    void testSimpleMessageCreation() {
        String content = "Test message";
        String senderId = "sender123";
        String recipientId = "recipient456";
        int sequenceNumber = 42;

        SimpleMessage message = new SimpleMessage(content, senderId, recipientId, sequenceNumber);

        assertEquals(content, message.getContent());
        assertEquals(senderId, message.getSenderId());
        assertEquals(recipientId, message.getRecipientId());
        assertEquals(sequenceNumber, message.getSequenceNumber());
    }

    @Test
    @DisplayName("Simple message should reject null content")
    void testSimpleMessageWithNullContent() {
        assertThrows(NullPointerException.class, () ->
                new SimpleMessage(null, "sender", "recipient", 1));
    }

    @Test
    @DisplayName("Simple message should reject null sender ID")
    void testSimpleMessageWithNullSender() {
        assertThrows(NullPointerException.class, () ->
                new SimpleMessage("content", null, "recipient", 1));
    }

    @Test
    @DisplayName("Simple message should reject null recipient ID")
    void testSimpleMessageWithNullRecipient() {
        assertThrows(NullPointerException.class, () ->
                new SimpleMessage("content", "sender", null, 1));
    }

    @Test
    @DisplayName("Messages with same content should be equal")
    void testMessageEquality() {
        SimpleMessage message1 = new SimpleMessage("content", "sender", "recipient", 1);
        SimpleMessage message2 = new SimpleMessage("content", "sender", "recipient", 1);

        assertEquals(message1, message2);
        assertEquals(message1.hashCode(), message2.hashCode());
    }

    @Test
    @DisplayName("Messages with different content should not be equal")
    void testMessageInequality() {
        SimpleMessage message1 = new SimpleMessage("content1", "sender", "recipient", 1);
        SimpleMessage message2 = new SimpleMessage("content2", "sender", "recipient", 1);

        assertNotEquals(message1, message2);
    }

    @Test
    @DisplayName("Messages with different senders should not be equal")
    void testMessageInequalityDifferentSenders() {
        SimpleMessage message1 = new SimpleMessage("content", "sender1", "recipient", 1);
        SimpleMessage message2 = new SimpleMessage("content", "sender2", "recipient", 1);

        assertNotEquals(message1, message2);
    }

    @Test
    @DisplayName("Messages with different sequence numbers should not be equal")
    void testMessageInequalityDifferentSequence() {
        SimpleMessage message1 = new SimpleMessage("content", "sender", "recipient", 1);
        SimpleMessage message2 = new SimpleMessage("content", "sender", "recipient", 2);

        assertNotEquals(message1, message2);
    }

    @Test
    @DisplayName("Message should not equal null")
    void testMessageNotEqualNull() {
        SimpleMessage message = new SimpleMessage("content", "sender", "recipient", 1);

        assertNotEquals(message, null);
    }

    @Test
    @DisplayName("Message should not equal different class")
    void testMessageNotEqualDifferentClass() {
        SimpleMessage message = new SimpleMessage("content", "sender", "recipient", 1);
        String string = "not a message";

        assertNotEquals(message, string);
    }

    @Test
    @DisplayName("Message should equal itself")
    void testMessageEqualsSelf() {
        SimpleMessage message = new SimpleMessage("content", "sender", "recipient", 1);

        assertEquals(message, message);
    }

    @Test
    @DisplayName("ToString should contain message information")
    void testToString() {
        SimpleMessage message = new SimpleMessage("Hello World", "alice", "bob", 5);

        String result = message.toString();

        assertTrue(result.contains("Hello World"));
        assertTrue(result.contains("alice"));
        assertTrue(result.contains("bob"));
        assertTrue(result.contains("5"));
    }

    @Test
    @DisplayName("Message should handle empty content")
    void testEmptyContent() {
        SimpleMessage message = new SimpleMessage("", "sender", "recipient", 1);

        assertEquals("", message.getContent());
    }

    @Test
    @DisplayName("Message should handle zero sequence number")
    void testZeroSequenceNumber() {
        SimpleMessage message = new SimpleMessage("content", "sender", "recipient", 0);

        assertEquals(0, message.getSequenceNumber());
    }

    @Test
    @DisplayName("Message should handle negative sequence number")
    void testNegativeSequenceNumber() {
        SimpleMessage message = new SimpleMessage("content", "sender", "recipient", -1);

        assertEquals(-1, message.getSequenceNumber());
    }
}

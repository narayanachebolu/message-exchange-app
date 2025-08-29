package org.example.player.channel;

import org.example.player.exception.MessageExchangeException;
import org.example.player.message.Message;
import org.example.player.message.SimpleMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the InProcessChannel class. These tests verify the behavior of in-process message exchange channels.
 */
class InProcessChannelTest {

    private InProcessChannel channel;
    private static final String CHANNEL_ID = "test-channel";

    @BeforeEach
    void setUp() {
        channel = new InProcessChannel(CHANNEL_ID);
    }

    @Test
    @DisplayName("New channel should not be connected initially")
    void testInitialState() {
        assertFalse(channel.isConnected());
        assertEquals(0, channel.getQueueSize());
    }

    @Test
    @DisplayName("Channel should connect successfully")
    void testConnect() throws MessageExchangeException {
        channel.connect();
        assertTrue(channel.isConnected());
    }

    @Test
    @DisplayName("Double connect should throw exception")
    void testDoubleConnect() throws MessageExchangeException {
        channel.connect();

        assertThrows(MessageExchangeException.class, () -> channel.connect());
    }

    @Test
    @DisplayName("Send message should work when connected")
    void testSendMessage() throws MessageExchangeException {
        channel.connect();
        Message message = new SimpleMessage("test", "sender", "recipient", 1);

        channel.sendMessage(message);

        assertEquals(1, channel.getQueueSize());
    }

    @Test
    @DisplayName("Send message should fail when not connected")
    void testSendMessageNotConnected() {
        Message message = new SimpleMessage("test", "sender", "recipient", 1);

        assertThrows(MessageExchangeException.class, () -> channel.sendMessage(message));
    }

    @Test
    @DisplayName("Receive message should work when connected and messages available")
    void testReceiveMessage() throws MessageExchangeException, InterruptedException {
        channel.connect();
        Message sentMessage = new SimpleMessage("test", "sender", "recipient", 1);

        channel.sendMessage(sentMessage);
        Message receivedMessage = channel.receiveMessage();

        assertEquals(sentMessage, receivedMessage);
        assertEquals(0, channel.getQueueSize());
    }

    @Test
    @DisplayName("Receive message should fail when not connected")
    void testReceiveMessageNotConnected() {
        assertThrows(MessageExchangeException.class, () -> channel.receiveMessage());
    }

    @Test
    @DisplayName("Receive message should block until message is available")
    void testReceiveMessageBlocking() throws MessageExchangeException, InterruptedException, ExecutionException,
            TimeoutException {
        channel.connect();
        CompletableFuture<Message> future = new CompletableFuture<>();

        // Start receiving in another thread
        Thread receiver = new Thread(() -> {
            try {
                Message message = channel.receiveMessage();
                future.complete(message);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        receiver.start();

        // Give receiver time to start and block
        Thread.sleep(100);
        assertFalse(future.isDone());

        // Send a message
        Message sentMessage = new SimpleMessage("delayed", "sender", "recipient", 1);
        channel.sendMessage(sentMessage);

        // Receiver should now complete
        Message receivedMessage = future.get(1, TimeUnit.SECONDS);
        assertEquals(sentMessage, receivedMessage);

        receiver.join();
    }

    @Test
    @DisplayName("Multiple messages should be received in FIFO order")
    void testMessageOrder() throws MessageExchangeException, InterruptedException {
        channel.connect();

        Message message1 = new SimpleMessage("first", "sender", "recipient", 1);
        Message message2 = new SimpleMessage("second", "sender", "recipient", 2);
        Message message3 = new SimpleMessage("third", "sender", "recipient", 3);

        channel.sendMessage(message1);
        channel.sendMessage(message2);
        channel.sendMessage(message3);

        assertEquals(message1, channel.receiveMessage());
        assertEquals(message2, channel.receiveMessage());
        assertEquals(message3, channel.receiveMessage());
    }

    @Test
    @DisplayName("Close should disconnect the channel")
    void testClose() throws MessageExchangeException {
        channel.connect();
        assertTrue(channel.isConnected());

        channel.close();
        assertFalse(channel.isConnected());
    }

    @Test
    @DisplayName("Close should clear message queue")
    void testClosesClearsQueue() throws MessageExchangeException {
        channel.connect();
        Message message = new SimpleMessage("test", "sender", "recipient", 1);
        channel.sendMessage(message);

        assertEquals(1, channel.getQueueSize());

        channel.close();
        assertEquals(0, channel.getQueueSize());
    }

    @Test
    @DisplayName("Multiple close calls should be safe")
    void testMultipleClose() throws MessageExchangeException {
        channel.connect();

        channel.close();
        channel.close(); // Should not throw exception

        assertFalse(channel.isConnected());
    }

    @Test
    @DisplayName("Operations after close should fail")
    void testOperationsAfterClose() throws MessageExchangeException {
        channel.connect();
        channel.close();

        Message message = new SimpleMessage("test", "sender", "recipient", 1);

        assertThrows(MessageExchangeException.class, () -> channel.sendMessage(message));
        assertThrows(MessageExchangeException.class, () -> channel.receiveMessage());
    }
}

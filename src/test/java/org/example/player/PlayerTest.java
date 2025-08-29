package org.example.player;

import org.example.player.channel.MessageExchangeChannel;
import org.example.player.exception.MessageExchangeException;
import org.example.player.message.Message;
import org.example.player.message.SimpleMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Player class.
 *
 * These tests verify the behavior of individual Player methods using mocked dependencies.
 */
class PlayerTest {

    @Mock
    private MessageExchangeChannel mockChannel;

    private Player player;
    private static final String PLAYER_ID = "TestPlayer";
    private static final String RECIPIENT_ID = "RecipientPlayer";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        player = new Player(PLAYER_ID, mockChannel);
    }

    @Test
    @DisplayName("Player should be created with valid ID and channel")
    void testPlayerCreation() {
        assertEquals(PLAYER_ID, player.getPlayerId());
        assertEquals(0, player.getMessageCounter());
        assertFalse(player.isConnected());
    }

    @Test
    @DisplayName("Player constructor should reject null ID")
    void testPlayerCreationWithNullId() {
        assertThrows(NullPointerException.class, () ->
                new Player(null, mockChannel));
    }

    @Test
    @DisplayName("Player constructor should reject null channel")
    void testPlayerCreationWithNullChannel() {
        assertThrows(NullPointerException.class, () ->
                new Player(PLAYER_ID, null));
    }

    @Test
    @DisplayName("Connect should delegate to message exchange channel")
    void testConnect() throws MessageExchangeException {
        player.connect();
        verify(mockChannel).connect();
    }

    @Test
    @DisplayName("Connect should propagate message exchange exceptions")
    void testConnectWithException() throws MessageExchangeException {
        MessageExchangeException expectedException = new MessageExchangeException("Connection failed");
        doThrow(expectedException).when(mockChannel).connect();

        MessageExchangeException actualException = assertThrows(MessageExchangeException.class, () ->
                player.connect());

        assertEquals(expectedException, actualException);
    }

    @Test
    @DisplayName("Send message should increment counter and delegate to channel")
    void testSendMessage() throws MessageExchangeException {
        String messageContent = "Hello World";

        player.sendMessage(messageContent, RECIPIENT_ID);

        assertEquals(1, player.getMessageCounter());
        verify(mockChannel).sendMessage(any(Message.class));

        // Verify the message was created correctly
        verify(mockChannel).sendMessage(argThat(message ->
                message.getContent().equals(messageContent) &&
                        message.getSenderId().equals(PLAYER_ID) &&
                        message.getRecipientId().equals(RECIPIENT_ID) &&
                        message.getSequenceNumber() == 1
        ));
    }

    @Test
    @DisplayName("Multiple send messages should increment counter correctly")
    void testMultipleSendMessages() throws MessageExchangeException {
        player.sendMessage("Message 1", RECIPIENT_ID);
        player.sendMessage("Message 2", RECIPIENT_ID);
        player.sendMessage("Message 3", RECIPIENT_ID);

        assertEquals(3, player.getMessageCounter());
        verify(mockChannel, times(3)).sendMessage(any(Message.class));
    }

    @Test
    @DisplayName("Receive message should delegate to channel")
    void testReceiveMessage() throws MessageExchangeException, InterruptedException {
        Message expectedMessage = new SimpleMessage("Test", "Sender", PLAYER_ID, 1);
        when(mockChannel.receiveMessage()).thenReturn(expectedMessage);

        Message actualMessage = player.receiveMessage();

        assertEquals(expectedMessage, actualMessage);
        verify(mockChannel).receiveMessage();
    }

    @Test
    @DisplayName("Create response should concatenate content with counter")
    void testCreateResponse() {
        Message receivedMessage = new SimpleMessage("Original message", "Sender", PLAYER_ID, 1);

        String response = player.createResponse(receivedMessage);

        // Response should be original content + space + (current counter + 1)
        assertEquals("Original message 1", response);
    }

    @Test
    @DisplayName("Create response with existing counter should increment correctly")
    void testCreateResponseAfterSendingMessages() throws MessageExchangeException {
        // Send some messages first to increment counter
        player.sendMessage("First", RECIPIENT_ID);
        player.sendMessage("Second", RECIPIENT_ID);

        Message receivedMessage = new SimpleMessage("Received", "Sender", PLAYER_ID, 1);
        String response = player.createResponse(receivedMessage);

        // Should be "Received 3" (counter was 2, response will be 3)
        assertEquals("Received 3", response);
    }

    @Test
    @DisplayName("Send response should create and send response message")
    void testSendResponse() throws MessageExchangeException {
        Message receivedMessage = new SimpleMessage("Hello", "Sender", PLAYER_ID, 1);

        player.sendResponse(receivedMessage);

        assertEquals(1, player.getMessageCounter());
        verify(mockChannel).sendMessage(argThat(message ->
                message.getContent().equals("Hello 1") &&
                        message.getSenderId().equals(PLAYER_ID) &&
                        message.getRecipientId().equals("Sender") &&
                        message.getSequenceNumber() == 1
        ));
    }

    @Test
    @DisplayName("Is connected should delegate to channel")
    void testIsConnected() {
        when(mockChannel.isConnected()).thenReturn(true);
        assertTrue(player.isConnected());

        when(mockChannel.isConnected()).thenReturn(false);
        assertFalse(player.isConnected());

        verify(mockChannel, times(2)).isConnected();
    }

    @Test
    @DisplayName("Disconnect should close the channel")
    void testDisconnect() {
        player.disconnect();
        verify(mockChannel).close();
    }

    @Test
    @DisplayName("ToString should include player information")
    void testToString() {
        when(mockChannel.isConnected()).thenReturn(true);

        String result = player.toString();

        assertTrue(result.contains(PLAYER_ID));
        assertTrue(result.contains("messagesSent=0"));
        assertTrue(result.contains("connected=true"));
    }
}

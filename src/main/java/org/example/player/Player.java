package org.example.player;

import org.example.player.channel.MessageExchangeChannel;
import org.example.player.exception.MessageExchangeException;
import org.example.player.message.Message;
import org.example.player.message.SimpleMessage;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Represents a player that can exchange a message with other players. Each player has a unique ID and can
 * send/receive messages through a message exchange channel. Players maintain a count of messages sent and implement
 * the core message exchange logic.
 *
 * Responsibilities:
 * - Manage player identity and state (ID, message counter)
 * - Send and receive messages through message exchange channels
 * - Implement message processing logic (concatenation with counter)
 * - Handle message exchange channel lifecycle (connect, disconnect)
 * - Support both initiator and coplayer roles in message exchange.
 */
public class Player {
    private static final Logger LOG = Logger.getLogger(Player.class.getName());

    private final String playerId;
    private final MessageExchangeChannel channel;
    private final AtomicInteger messageCounter;

    /**
     * Creates a new player with the specified ID and message exchange channel.
     *
     * @param playerId unique identifier for this player
     * @param channel message exchange channel for sending/receiving messages
     */
    public Player(String playerId, MessageExchangeChannel channel) {
        this.playerId = Objects.requireNonNull(playerId, "Player ID cannot be null");
        this.channel = Objects.requireNonNull(channel, "Message Exchange Channel cannot be null");
        this.messageCounter = new AtomicInteger(0);
    }

    /**
     * Establishes connection for message exchange. Must be called before sending or receiving messages.
     *
     * @throws org.example.player.exception.MessageExchangeException if connection cannot be established
     */
    public void connect() throws MessageExchangeException {
        channel.connect();
        LOG.fine("Player " + playerId + " connected");
    }

    /**
     * Sends a message to another player. The message will include the current message counter value.
     *
     * @param content the message content to send
     * @param recipientId the ID of the recipient player
     * @throws MessageExchangeException if the message cannot be sent
     */
    public void sendMessage(String content, String recipientId) throws MessageExchangeException {
        int sequenceNumber = messageCounter.incrementAndGet();
        Message message = new SimpleMessage(content, playerId, recipientId, sequenceNumber);
        channel.sendMessage(message);

        LOG.info(String.format("Player %s sent message #%d: '%s' to %s",
                playerId, sequenceNumber, content, recipientId));
    }

    /**
     * Receives the next message from the message exchange channel. This method blocks until a message is available.
     *
     * @return the received message
     * @throws MessageExchangeException if message cannot be received
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public Message receiveMessage() throws MessageExchangeException, InterruptedException {
        Message message = channel.receiveMessage();
        LOG.info(String.format("Player %s received message from %s: '%s'",
                playerId, message.getSenderId(), message.getContent()));
        return message;
    }

    /**
     * Creates a response message by concatenating the received message content with this player's message counter.
     * This implements the required response logic.
     *
     * @param receivedMessage the message that was received
     * @return response message content
     */
    public String createResponse(Message receivedMessage) {
        int currentCounter = messageCounter.get() + 1; // Counter for the response message
        String response = receivedMessage.getContent() + " " + currentCounter;
        LOG.fine(String.format("Player %s created response: '%s'", playerId, response));
        return response;
    }

    /**
     * Sends a response message to the sender of a received message. The response contains the original message
     * content concatenated with the message counter.
     *
     * @param receivedMessage the message being responded to
     * @throws MessageExchangeException if the response cannot be sent
     */
    public void sendResponse(Message receivedMessage) throws MessageExchangeException {
        String responseContent = createResponse(receivedMessage);
        sendMessage(responseContent, receivedMessage.getSenderId());
    }

    /**
     * Gets the current message counter value. This represents the number of messages this player has sent.
     *
     * @return the current message counter
     */
    public int getMessageCounter() {
        return messageCounter.get();
    }

    /**
     * Gets this player's unique identifier.
     *
     * @return the player ID
     */
    public String getPlayerId() {
        return playerId;
    }

    /**
     * Checks if this player's message exchange channel is connected.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return channel.isConnected();
    }

    /**
     * Disconnects this player and closes the message exchange channel.
     * This method should be called when the player is no longer needed.
     */
    public void disconnect() {
        channel.close();
        LOG.fine("Player " + playerId + " disconnected");
    }

    @Override
    public String toString() {
        return String.format("Player{id='%s', messagesSent=%d, connected=%s}",
                playerId, messageCounter.get(), isConnected());
    }
}

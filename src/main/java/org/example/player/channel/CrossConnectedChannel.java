package org.example.player.channel;

import org.example.player.exception.MessageExchangeException;
import org.example.player.message.Message;

/**
 * A message exchange channel that wraps two separate channels for bidirectional message exchanges. This implementation
 * enables same-process message exchange by connecting two players with cross-connected send/receive channels.
 *
 * Responsibilities:
 * - Coordinate bidirectional message exchange between two channels.
 * - Route outgoing messages to the appropriate send channel.
 * - Route incoming messages from the appropriate receive channel.
 * - Manage connection state for both underlying channels.
 * - Provide unified interface for cross-connected message exchange.
 */
public class CrossConnectedChannel implements MessageExchangeChannel {
    private final MessageExchangeChannel sendChannel;
    private final MessageExchangeChannel receiveChannel;

    /**
     * Creates a cross-connected channel using separate send and receive channels.
     *
     * @param sendChannel channel for sending messages (connects to other player's receive)
     * @param receiveChannel channel for receiving messages (connects to other player's send)
     */
    public CrossConnectedChannel(MessageExchangeChannel sendChannel, MessageExchangeChannel receiveChannel) {
        this.sendChannel = sendChannel;
        this.receiveChannel = receiveChannel;
    }

    @Override
    public void connect() throws MessageExchangeException {
        try {
            sendChannel.connect();
            receiveChannel.connect();
        } catch (MessageExchangeException e) {
            // If one fails, try to clean up the other
            try {
                sendChannel.close();
            } catch (Exception ignored) {}
            try {
                receiveChannel.close();
            } catch (Exception ignored) {}
            throw e;
        }
    }

    @Override
    public void sendMessage(Message message) throws MessageExchangeException {
        sendChannel.sendMessage(message);
    }

    @Override
    public Message receiveMessage() throws MessageExchangeException, InterruptedException {
        return receiveChannel.receiveMessage();
    }

    @Override
    public boolean isConnected() {
        return sendChannel.isConnected() && receiveChannel.isConnected();
    }

    @Override
    public void close() {
        try {
            sendChannel.close();
        } catch (Exception e) {
            System.err.println("Error closing send channel: " + e.getMessage());
        }

        try {
            receiveChannel.close();
        } catch (Exception e) {
            System.err.println("Error closing receive channel: " + e.getMessage());
        }
    }
}

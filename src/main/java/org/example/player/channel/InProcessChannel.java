package org.example.player.channel;

import org.example.player.exception.MessageExchangeException;
import org.example.player.message.Message;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-process implementation of MessageExchangeChannel within the same-process. Uses blocking queues to enable
 * thread-safe message passing between players running in the same JVM process.
 *
 * Responsibilities:
 * - Provide thread-safe in-process message passing
 * - Handle blocking receive operations
 * - Manage connection state for same-process players
 * - Support graceful shutdown and resource cleanup
 */
public class InProcessChannel implements MessageExchangeChannel {
    private final BlockingQueue<Message> messageQueue;
    private final AtomicBoolean connected;
    private final String channelId;

    /**
     * Creates a new in-process message exchange channel.
     *
     * @param channelId identifier for this channel (used for debugging)
     */
    public InProcessChannel(String channelId) {
        this.channelId = channelId;
        this.messageQueue = new LinkedBlockingQueue<>();
        this.connected = new AtomicBoolean(false);
    }

    @Override
    public void connect() throws MessageExchangeException {
        if (!connected.compareAndSet(false, true)) {
            throw new MessageExchangeException("Channel " + channelId + " is already connected");
        }
        System.out.println("InProcessChannel " + channelId + " connected");
    }

    @Override
    public void sendMessage(Message message) throws MessageExchangeException {
        if (!isConnected()) {
            throw new MessageExchangeException("Channel " + channelId + " is not connected");
        }

        try {
            messageQueue.offer(message);
            System.out.println("Sent message via InProcessChannel " + channelId + ": " + message);
        } catch (Exception e) {
            throw new MessageExchangeException("Failed to send message via channel " + channelId, e);
        }
    }

    @Override
    public Message receiveMessage() throws MessageExchangeException, InterruptedException {
        if (!isConnected()) {
            throw new MessageExchangeException("Channel " + channelId + " is not connected");
        }

        try {
            Message message = messageQueue.take(); // Blocks until message is available
            System.out.println("Received message via InProcessChannel " + channelId + ": " + message);
            return message;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            throw e;
        } catch (Exception e) {
            throw new MessageExchangeException("Failed to receive message via channel " + channelId, e);
        }
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public void close() {
        if (connected.compareAndSet(true, false)) {
            messageQueue.clear();
            System.out.println("InProcessChannel " + channelId + " closed");
        }
    }

    /**
     * Gets the number of messages currently queued in this channel.
     * Useful for testing and debugging.
     *
     * @return the number of queued messages
     */
    public int getQueueSize() {
        return messageQueue.size();
    }
}

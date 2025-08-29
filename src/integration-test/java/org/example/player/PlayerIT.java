package org.example.player;

import org.example.player.channel.CrossConnectedChannel;
import org.example.player.channel.InProcessChannel;
import org.example.player.channel.SocketChannel;
import org.example.player.coordinator.MessageExchangeCoordinator;
import org.example.player.message.Message;
import org.example.player.message.SimpleMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Message Exchange System. These tests verify the complete message exchange flow
 * between players using real message exchange channels.
 */
class PlayerIT {

    @Test
    @DisplayName("Same process message exchange should complete successfully")
    @Timeout(10)
    void testSameProcessMessageExchange() throws Exception {
        // Create shared in-process channels
        InProcessChannel initiatorToCoplayer = new InProcessChannel("initiator->coplayer");
        InProcessChannel coplayerToInitiator = new InProcessChannel("coplayer->initiator");

        // Create players with cross-connected channels
        Player initiator = new Player("Initiator",
                new CrossConnectedChannel(initiatorToCoplayer, coplayerToInitiator));
        Player responder = new Player("Responder",
                new CrossConnectedChannel(coplayerToInitiator, initiatorToCoplayer));

        // Test the complete game
        MessageExchangeCoordinator coordinator = new MessageExchangeCoordinator(initiator, responder);
        coordinator.startMessageExchange();

        // Verify final state
        assertEquals(10, initiator.getMessageCounter());
        assertEquals(10, responder.getMessageCounter());
        assertFalse(initiator.isConnected());
        assertFalse(responder.isConnected());
    }

    @Test
    @DisplayName("Socket message exchange should work between processes")
    @Timeout(15)
    void testSocketMessageExchange() throws Exception {
        int port = 8081; // Use different port to avoid conflicts

        // Start server in background thread
        CompletableFuture<Player> serverFuture = CompletableFuture.supplyAsync(() -> {
            try {
                SocketChannel serverChannel = new SocketChannel(port);
                Player server = new Player("Server", serverChannel);
                server.connect();
                return server;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Give server time to start
        Thread.sleep(1000);

        // Create client
        SocketChannel clientChannel = new SocketChannel("localhost", port);
        Player client = new Player("Client", clientChannel);
        client.connect();

        // Get server player
        Player server = serverFuture.get(5, TimeUnit.SECONDS);

        try {
            // Test message exchange
            String testMessage = "Hello from client";

            // Client sends message
            client.sendMessage(testMessage, "Server");

            // Server receives and responds
            Message receivedMessage = server.receiveMessage();
            assertEquals(testMessage, receivedMessage.getContent());
            assertEquals("Client", receivedMessage.getSenderId());

            server.sendResponse(receivedMessage);

            // Client receives response
            Message response = client.receiveMessage();
            assertEquals(testMessage + " 1", response.getContent());
            assertEquals("Server", response.getSenderId());

            // Verify counters
            assertEquals(1, client.getMessageCounter());
            assertEquals(1, server.getMessageCounter());

        } finally {
            // Cleanup
            client.disconnect();
            server.disconnect();
        }
    }

    @Test
    @DisplayName("Multiple rounds of message exchange should work correctly")
    @Timeout(10)
    void testMultipleRoundsMessageExchange() throws Exception {
        InProcessChannel channel1 = new InProcessChannel("1->2");
        InProcessChannel channel2 = new InProcessChannel("2->1");

        Player player1 = new Player("Player1",
                new CrossConnectedChannel(channel1, channel2));
        Player player2 = new Player("Player2",
                new CrossConnectedChannel(channel2, channel1));

        player1.connect();
        //player2.connect();

        try {
            String currentMessage = "Start";

            // Run 5 rounds of message exchange
            for (int i = 1; i <= 5; i++) {
                // Player1 sends
                player1.sendMessage(currentMessage, "Player2");

                // Player2 receives and responds
                Message received = player2.receiveMessage();
                player2.sendResponse(received);

                // Player1 receives response
                Message response = player1.receiveMessage();
                currentMessage = response.getContent();

                // Verify progression
                assertTrue(currentMessage.contains(String.valueOf(i)));
                assertEquals(i, player1.getMessageCounter());
                assertEquals(i, player2.getMessageCounter());
            }

        } finally {
            player1.disconnect();
            player2.disconnect();
        }
    }

    @Test
    @DisplayName("Message exchange should handle concurrent access safely")
    @Timeout(10)
    void testConcurrentMessageExchange() throws Exception {
        InProcessChannel channel = new InProcessChannel("concurrent");
        channel.connect();

        int messageCount = 100;
        CompletableFuture<Void> sender = CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < messageCount; i++) {
                    Message message = new SimpleMessage("Message " + i, "Sender", "Receiver", i);
                    channel.sendMessage(message);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Integer> receiver = CompletableFuture.supplyAsync(() -> {
            try {
                int received = 0;
                for (int i = 0; i < messageCount; i++) {
                    Message message = channel.receiveMessage();
                    assertNotNull(message);
                    received++;
                }
                return received;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Wait for both to complete
        sender.get(5, TimeUnit.SECONDS);
        Integer receivedCount = receiver.get(5, TimeUnit.SECONDS);

        assertEquals(messageCount, receivedCount);
        assertEquals(0, channel.getQueueSize());

        channel.close();
    }

    @Test
    @DisplayName("Message Exchange Coordinator should handle edge cases gracefully")
    @Timeout(10)
    void testGameCoordinatorEdgeCases() throws Exception {
        InProcessChannel channel1 = new InProcessChannel("edge1");
        InProcessChannel channel2 = new InProcessChannel("edge2");

        Player initiator = new Player("EdgeInitiator",
                new CrossConnectedChannel(channel1, channel2));
        Player coplayer = new Player("EdgeCoplayer",
                new CrossConnectedChannel(channel2, channel1));

        MessageExchangeCoordinator coordinator = new MessageExchangeCoordinator(initiator, coplayer);

        // Should complete without errors
        coordinator.startMessageExchange();

        // Verify cleanup occurred
        assertFalse(initiator.isConnected());
        assertFalse(coplayer.isConnected());
    }

    @Test
    @DisplayName("Cross-connected channels should maintain isolation")
    @Timeout(5)
    void testCrossConnectedChannelIsolation() throws Exception {
        InProcessChannel channelA = new InProcessChannel("A");
        InProcessChannel channelB = new InProcessChannel("B");

        CrossConnectedChannel player1Channel = new CrossConnectedChannel(channelA, channelB);

        player1Channel.connect();
        //player2Channel.connect();

        try (player1Channel; CrossConnectedChannel player2Channel = new CrossConnectedChannel(channelB, channelA)) {
            Message message1 = new SimpleMessage("From Player 1", "Player1", "Player2", 1);
            Message message2 = new SimpleMessage("From Player 2", "Player2", "Player1", 1);

            // Each player sends a message
            player1Channel.sendMessage(message1);
            player2Channel.sendMessage(message2);

            // Each should receive the other's message
            Message received1 = player1Channel.receiveMessage();
            Message received2 = player2Channel.receiveMessage();

            assertEquals(message2, received1); // Player 1 receives Player 2's message
            assertEquals(message1, received2); // Player 2 receives Player 1's message

        }
    }
}

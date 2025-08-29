package org.example.player;

import org.example.player.channel.CrossConnectedChannel;
import org.example.player.channel.InProcessChannel;
import org.example.player.channel.SocketChannel;
import org.example.player.coordinator.MessageExchangeCoordinator;
import org.example.player.exception.MessageExchangeException;
import org.example.player.message.Message;

/**
 * This is the application entry point for the Message Exchange System. This class handles command-line arguments
 * and coordinates the execution of both same-process and separate-process message exchange scenarios.
 *
 * Responsibilities:
 * - Parse command-line arguments to determine execution mode.
 * - Coordinate same-process message exchange using in-process channels.
 * - Support separate-process message exchange using socket channels.
 * - Handle application lifecycle and error management.
 * - Provide user-friendly error messages and usage information.
 */
public class MessageExchangeApp {
    private static final String USAGE =
            "Usage:\n" +
                    "  Same process:     java -jar message-exchange-app.jar same-process\n" +
                    "  Separate process: java -jar message-exchange-app.jar separate-process [server|client] [port] [host]\n" +
                    "\n" +
                    "Examples:\n" +
                    "  java -jar message-exchange-app.jar same-process\n" +
                    "  java -jar message-exchange-app.jar separate-process server 8080\n" +
                    "  java -jar message-exchange-app.jar separate-process client 8080 localhost\n";

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                System.out.println(USAGE);
                System.exit(1);
            }

            String mode = args[0].toLowerCase();

            switch (mode) {
                case "same-process":
                    runSameProcessMessageExchange();
                    break;

                case "separate-process":
                    runSeparateProcessMessageExchange(args);
                    break;

                default:
                    System.err.println("Unknown mode: " + mode);
                    System.out.println(USAGE);
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Application failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Starts the message exchange task with both players in the same JVM process.
     * Uses in-process channel for message exchange between players.
     */
    private static void runSameProcessMessageExchange() throws MessageExchangeException, InterruptedException {
        System.out.println("=== Same Process Message Exchange Mode ===");

        // Create shared in-process channels
        InProcessChannel initiatorToCoplayer = new InProcessChannel("initiator->coplayer");
        InProcessChannel coplayerToInitiator = new InProcessChannel("coplayer->initiator");

        // Create players with cross-connected channels
        Player initiator = new Player("Initiator", new CrossConnectedChannel(initiatorToCoplayer, coplayerToInitiator));
        Player coplayer = new Player("Coplayer", new CrossConnectedChannel(coplayerToInitiator, initiatorToCoplayer));

        // Create and run the game
        MessageExchangeCoordinator coordinator = new MessageExchangeCoordinator(initiator, coplayer);
        coordinator.startMessageExchange();
    }

    /**
     * Runs the message exchange game with players in separate JVM processes.
     * Uses socket-based message exchange between players.
     */
    private static void runSeparateProcessMessageExchange(String[] args) throws MessageExchangeException, InterruptedException {
        if (args.length < 3) {
            System.err.println("Separate process mode requires additional arguments");
            System.out.println(USAGE);
            System.exit(1);
        }

        String role = args[1].toLowerCase();
        int port = Integer.parseInt(args[2]);

        switch (role) {
            case "server":
                runAsServer(port);
                break;

            case "client":
                String host = args.length > 3 ? args[3] : "localhost";
                runAsClient(host, port);
                break;

            default:
                System.err.println("Unknown role: " + role + ". Use 'server' or 'client'");
                System.exit(1);
        }
    }

    /**
     * Runs this process as the server (responder) player.
     */
    private static void runAsServer(int port) throws MessageExchangeException, InterruptedException {
        System.out.println("=== Separate Process Message Exchange Mode - Server (Responder) ===");
        System.out.println("Starting server on port " + port);

        SocketChannel serverChannel = new SocketChannel(port);
        Player responder = new Player("Server-Responder", serverChannel);

        responder.connect();

        try {
            // Server acts as responder - waits for messages and responds
            for (int i = 0; i < 10; i++) {
                System.out.println("--- Server Round " + (i + 1) + " ---");

                // Receive message from client
                Message receivedMessage = responder.receiveMessage();

                // Send response back
                responder.sendResponse(receivedMessage);
            }

            System.out.println("=== Server Message Exchange Completed ===");
            System.out.println("Server sent " + responder.getMessageCounter() + " response messages");

        } finally {
            responder.disconnect();
        }
    }

    /**
     * Runs this process as the client (initiator) player.
     */
    private static void runAsClient(String host, int port) throws MessageExchangeException, InterruptedException {
        System.out.println("=== Separate Process Message Exchange Mode - Client (Initiator) ===");
        System.out.println("Connecting to server at " + host + ":" + port);

        SocketChannel clientChannel = new SocketChannel(host, port);
        Player initiator = new Player("Client-Initiator", clientChannel);

        initiator.connect();

        try {
            String currentMessage = "Hello";

            // Client acts as initiator - sends messages and receives responses
            for (int i = 0; i < 10; i++) {
                System.out.println("--- Client Round " + (i + 1) + " ---");

                // Send message to server
                initiator.sendMessage(currentMessage, "Server-Responder");

                // Receive response from server
                Message response = initiator.receiveMessage();

                // Use response content for next message
                currentMessage = response.getContent();
            }

            System.out.println("=== Client Message Exchange Completed ===");
            System.out.println("Client sent " + initiator.getMessageCounter() + " initial messages");

        } finally {
            initiator.disconnect();
        }
    }
}
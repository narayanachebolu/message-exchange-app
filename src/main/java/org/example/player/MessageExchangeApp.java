package org.example.player;

import org.example.player.channel.CrossConnectedChannel;
import org.example.player.channel.InProcessChannel;
import org.example.player.channel.SocketChannel;
import org.example.player.coordinator.MessageExchangeCoordinator;
import org.example.player.exception.MessageExchangeException;
import org.example.player.message.Message;

import java.util.logging.*;

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
    // ---------- Logging setup: Simple console logger config ----------
    static {
        // get root logger
        Logger rootLogger = Logger.getLogger("");

        // remove default handlers
        for (Handler h : rootLogger.getHandlers()) {
            rootLogger.removeHandler(h);
        }

        rootLogger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO);

        // Custom formatter: only print the message
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                return record.getMessage() + System.lineSeparator();
            }
        });

        rootLogger.addHandler(handler);
        rootLogger.setLevel(Level.INFO);
    }

    private static final Logger LOG = Logger.getLogger(MessageExchangeApp.class.getName());

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
                LOG.info(USAGE);
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
                    LOG.log(Level.SEVERE, "Unknown mode: " + mode);
                    LOG.info(USAGE);
                    System.exit(1);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Application failed: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Starts the message exchange task with both players in the same JVM process.
     * Uses in-process channel for message exchange between players.
     */
    private static void runSameProcessMessageExchange() throws MessageExchangeException, InterruptedException {
        LOG.info("=== Same Process Message Exchange Mode ===");
        LOG.info("");

        // Create shared in-process channels
        InProcessChannel initiatorToCoplayer = new InProcessChannel("initiator->coplayer");
        InProcessChannel coplayerToInitiator = new InProcessChannel("coplayer->initiator");

        // Create players with cross-connected channels
        Player initiator = new Player("Initiator", new CrossConnectedChannel(initiatorToCoplayer, coplayerToInitiator));
        Player coplayer = new Player("Coplayer", new CrossConnectedChannel(coplayerToInitiator, initiatorToCoplayer));

        // Create and start the message exchange among players.
        MessageExchangeCoordinator coordinator = new MessageExchangeCoordinator(initiator, coplayer);
        coordinator.startMessageExchange();
    }

    /**
     * Start the message exchange among the players in separate JVM processes.
     * Uses socket-based message exchange between players.
     */
    private static void runSeparateProcessMessageExchange(String[] args) throws MessageExchangeException, InterruptedException {
        if (args.length < 3) {
            LOG.info("Separate process mode requires additional arguments");
            LOG.info(USAGE);
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
                LOG.log(Level.SEVERE, "Unknown role: " + role + ". Use 'server' or 'client'");
                System.exit(1);
        }
    }

    /**
     * Runs this process as the server (responder) player.
     */
    private static void runAsServer(int port) throws MessageExchangeException, InterruptedException {
        LOG.info("=== Separate Process Message Exchange Mode - Server (Responder) ===");
        LOG.info("");
        LOG.info("Starting server on port " + port);

        SocketChannel serverChannel = new SocketChannel(port);
        Player responder = new Player("Server-Responder", serverChannel);

        responder.connect();

        try {
            // Server acts as responder - waits for messages and responds
            for (int i = 0; i < 10; i++) {
                LOG.info("--- Server Round " + (i + 1) + " ---");

                // Receive message from client
                Message receivedMessage = responder.receiveMessage();

                // Send response back
                responder.sendResponse(receivedMessage);
            }

            LOG.info("=== Server Message Exchange Completed ===");
            LOG.info("Server sent " + responder.getMessageCounter() + " response messages");

        } finally {
            responder.disconnect();
        }
    }

    /**
     * Runs this process as the client (initiator) player.
     */
    private static void runAsClient(String host, int port) throws MessageExchangeException, InterruptedException {
        LOG.info("=== Separate Process Message Exchange Mode - Client (Initiator) ===");
        LOG.info("");
        LOG.info("Connecting to server at " + host + ":" + port);

        SocketChannel clientChannel = new SocketChannel(host, port);
        Player initiator = new Player("Client-Initiator", clientChannel);

        initiator.connect();

        try {
            String currentMessage = "Hello";

            // Client acts as initiator - sends messages and receives responses
            for (int i = 0; i < 10; i++) {
                LOG.info("--- Client Round " + (i + 1) + " ---");

                // Send message to server
                initiator.sendMessage(currentMessage, "Server-Responder");

                // Receive response from server
                Message response = initiator.receiveMessage();

                // Use response content for next message
                currentMessage = response.getContent();
            }

            LOG.info("=== Client Message Exchange Completed ===");
            LOG.info("Client sent " + initiator.getMessageCounter() + " initial messages");

        } finally {
            initiator.disconnect();
        }
    }
}
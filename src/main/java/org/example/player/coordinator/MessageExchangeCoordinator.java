package org.example.player.coordinator;

import org.example.player.Player;
import org.example.player.exception.MessageExchangeException;
import org.example.player.message.Message;

/**
 * Coordinates the message exchange between two players. This class implements the core logic where one
 * player (initiator) sends messages and the other player responds, continuing until the stop condition is met.
 *
 * Responsibilities:
 * - Orchestrate the message exchange between two players.
 * - Enforce game rules (10 messages limit, proper sequencing).
 * - Handle both initiator and coplayer logic.
 * - Provide clean game lifecycle management.
 * - Report game results and statistics.
 */
public class MessageExchangeCoordinator {
    private static final int MAX_MESSAGES = 10;
    private static final String INITIAL_MESSAGE = "Hello";

    private final Player initiator;
    private final Player coplayer;

    /**
     * Creates a new game coordinator with the specified players.
     *
     * @param initiator the player who will start the message exchange
     * @param coplayer the player who will respond to messages
     */
    public MessageExchangeCoordinator(Player initiator, Player coplayer) {
        this.initiator = initiator;
        this.coplayer = coplayer;
    }

    /**
     * Starts the complete message exchange cycle. The initiator sends messages and receives responses until the
     * limit is reached.
     *
     * @throws MessageExchangeException if message exchange fails during the game
     * @throws InterruptedException   if the game is interrupted
     */
    public void startMessageExchange() throws MessageExchangeException, InterruptedException {
        System.out.println("=== Starting Message Exchange ===");
        System.out.println("Initiator: " + initiator.getPlayerId());
        System.out.println("Coplayer: " + coplayer.getPlayerId());
        System.out.println("Message limit: " + MAX_MESSAGES);
        System.out.println();

        // Connect both players
        initiator.connect();
        //coplayer.connect();

        try {
            // Start the message exchange
            runMessageExchange();

            // Print final results
            printGameResults();

        } finally {
            // Ensure cleanup happens
            cleanup();
        }
    }

    /**
     * Runs the message exchange loop between initiator and coplayer.
     */
    private void runMessageExchange() throws MessageExchangeException, InterruptedException {
        String currentMessage = INITIAL_MESSAGE;

        for (int round = 1; round <= MAX_MESSAGES; round++) {
            System.out.println("--- Round " + round + " ---");

            // Initiator sends message
            initiator.sendMessage(currentMessage, coplayer.getPlayerId());

            // Coplayer receives and processes the message
            Message receivedMessage = coplayer.receiveMessage();

            // Coplayer sends response back
            coplayer.sendResponse(receivedMessage);

            // Initiator receives the response
            Message response = initiator.receiveMessage();

            // Update message for next round (use the response content)
            currentMessage = response.getContent();

            System.out.println("Round " + round + " completed");
            System.out.println();
        }
    }

    /**
     * Prints the final game results and statistics.
     */
    private void printGameResults() {
        System.out.println("=== Game Completed Successfully ===");
        System.out.println("Final Statistics:");
        System.out.println("- Initiator (" + initiator.getPlayerId() + ") sent: " + initiator.getMessageCounter() + " messages");
        System.out.println("- Coplayer (" + coplayer.getPlayerId() + ") sent: " + coplayer.getMessageCounter() + " messages");
        System.out.println("- Total rounds completed: " + MAX_MESSAGES);
    }

    /**
     * Performs cleanup by disconnecting both players.
     */
    private void cleanup() {
        try {
            initiator.disconnect();
        } catch (Exception e) {
            System.err.println("Error disconnecting initiator: " + e.getMessage());
        }

        try {
            coplayer.disconnect();
        } catch (Exception e) {
            System.err.println("Error disconnecting coplayer: " + e.getMessage());
        }
    }

    /**
     * Gets the initiator player.
     *
     * @return the initiator player
     */
    public Player getInitiator() {
        return initiator;
    }

    /**
     * Gets the coplayer player.
     *
     * @return the coplayer player
     */
    public Player getCoplayer() {
        return coplayer;
    }
}

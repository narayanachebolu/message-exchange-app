package org.example.player.coordinator;

import org.example.player.Player;
import org.example.player.exception.MessageExchangeException;
import org.example.player.message.Message;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Coordinates the message exchange between two players. This class implements the core logic where one
 * player (initiator) sends messages and the other player responds, continuing until the stop condition is met.
 *
 * Responsibilities:
 * - Orchestrate the message exchange between two players.
 * - Enforce message exchange rules (10 messages limit, proper sequencing).
 * - Handle both initiator and coplayer logic.
 * - Provide clean message exchange lifecycle management.
 * - Report: Message exchange results and statistics.
 */
public class MessageExchangeCoordinator {
    private static final Logger LOG = Logger.getLogger(MessageExchangeCoordinator.class.getName());

    private static final int MAX_MESSAGES = 10;
    private static final String INITIAL_MESSAGE = "Hello";

    private final Player initiator;
    private final Player coplayer;

    /**
     * Creates a new message exchange coordinator with the specified players.
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
     * @throws MessageExchangeException if message exchange fails
     * @throws InterruptedException   if the message exchange is interrupted
     */
    public void startMessageExchange() throws MessageExchangeException, InterruptedException {
        LOG.info("*** Starting Message Exchange ***");
        LOG.info("Player 1: " + initiator.getPlayerId());
        LOG.info("Player 2: " + coplayer.getPlayerId());
        LOG.info("Message limit: " + MAX_MESSAGES);
        LOG.info("");

        // Connect both players
        initiator.connect();
        //coplayer.connect();

        try {
            // Start the message exchange
            runMessageExchange();

            // Print final results
            printMessageExchangeResults();

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
            LOG.info("--- " + round + " ---");

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

            LOG.fine("--- Round " + round + " completed ---");
            LOG.info("");
        }
    }

    /**
     * Prints the final message exchange results and statistics.
     */
    private void printMessageExchangeResults() {
        LOG.info("*** Message Exchange Completed ***");
        LOG.info("");
        LOG.info("Metrics:");
        LOG.info("- Player 1 (" + initiator.getPlayerId() + ") sent: " + initiator.getMessageCounter() + " messages");
        LOG.info("- Player 2 (" + coplayer.getPlayerId() + ") sent: " + coplayer.getMessageCounter() + " messages");
        LOG.info("- Total rounds completed: " + MAX_MESSAGES);
    }

    /**
     * Performs cleanup by disconnecting both players.
     */
    private void cleanup() {
        try {
            initiator.disconnect();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error disconnecting initiator: " + e.getMessage());
        }

        try {
            coplayer.disconnect();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error disconnecting coplayer: " + e.getMessage());
        }
    }
}

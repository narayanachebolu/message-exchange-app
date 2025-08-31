package org.example.player.channel;

import org.example.player.exception.MessageExchangeException;
import org.example.player.message.Message;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Socket-based implementation of MessageExchangeChannel for cross-process communication.
 * Uses TCP sockets and object serialization to enable message passing between
 * players running in separate JVM processes.
 *
 * Responsibilities:
 * - Provide TCP socket-based inter-process message exchange.
 * - Handle socket connection establishment and management.
 * - Serialize/deserialize messages for network transmission.
 * - Support both server (listening) and client (connecting) modes.
 * - Manage connection lifecycle and error handling.
 */
public class SocketChannel implements MessageExchangeChannel {
    private static final Logger LOG = Logger.getLogger(SocketChannel.class.getName());

    private final String host;
    private final int port;
    private final boolean isServer;
    private final AtomicBoolean connected;

    private Socket socket;
    private ServerSocket serverSocket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    /**
     * Creates a socket channel in client mode.
     *
     * @param host the hostname or IP address to connect to
     * @param port the port number to connect to
     */
    public SocketChannel(String host, int port) {
        this.host = host;
        this.port = port;
        this.isServer = false;
        this.connected = new AtomicBoolean(false);
    }

    /**
     * Creates a socket channel in server mode.
     *
     * @param port the port number to listen on
     */
    public SocketChannel(int port) {
        this.host = null;
        this.port = port;
        this.isServer = true;
        this.connected = new AtomicBoolean(false);
    }

    @Override
    public void connect() throws MessageExchangeException {
        if (!connected.compareAndSet(false, true)) {
            throw new MessageExchangeException("Socket channel is already connected");
        }

        try {
            if (isServer) {
                connectAsServer();
            } else {
                connectAsClient();
            }

            // Create object streams for serialization
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush(); // Important: flush the header
            inputStream = new ObjectInputStream(socket.getInputStream());

            LOG.fine("SocketChannel connected - " +
                    (isServer ? "Server on port " + port : "Client to " + host + ":" + port));

        } catch (IOException e) {
            connected.set(false);
            throw new MessageExchangeException("Failed to establish socket connection", e);
        }
    }

    private void connectAsServer() throws IOException {
        serverSocket = new ServerSocket(port);
        LOG.fine("Server listening on port " + port);
        socket = serverSocket.accept();
        LOG.fine("Server accepted connection from " + socket.getRemoteSocketAddress());
    }

    private void connectAsClient() throws IOException {
        // Retry logic for client connection
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            try {
                socket = new Socket(host, port);
                LOG.fine("Client connected to " + host + ":" + port);
                return;
            } catch (ConnectException e) {
                if (i == maxRetries - 1) throw e;
                LOG.log(Level.SEVERE, "Connection attempt " + (i + 1) + " failed, retrying...");
                try {
                    Thread.sleep(1000); // Wait 1 second before retry
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting to retry connection", ie);
                }
            }
        }
    }

    @Override
    public void sendMessage(Message message) throws MessageExchangeException {
        if (!isConnected()) {
            throw new MessageExchangeException("Socket channel is not connected");
        }

        try {
            outputStream.writeObject(message);
            outputStream.flush();
            LOG.fine("Sent message via socket: " + message);
        } catch (IOException e) {
            throw new MessageExchangeException("Failed to send message via socket", e);
        }
    }

    @Override
    public Message receiveMessage() throws MessageExchangeException, InterruptedException {
        if (!isConnected()) {
            throw new MessageExchangeException("Socket channel is not connected");
        }

        try {
            Message message = (Message) inputStream.readObject();
            LOG.fine("Received message via socket: " + message);
            return message;
        } catch (IOException | ClassNotFoundException e) {
            throw new MessageExchangeException("Failed to receive message via socket", e);
        }
    }

    @Override
    public boolean isConnected() {
        return connected.get() && socket != null && !socket.isClosed();
    }

    @Override
    public void close() {
        if (connected.compareAndSet(true, false)) {
            closeQuietly(inputStream);
            closeQuietly(outputStream);
            closeQuietly(socket);
            closeQuietly(serverSocket);

            LOG.fine("SocketChannel closed - " +
                    (isServer ? "Server" : "Client"));
        }
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Error closing resource: " + e.getMessage());
            }
        }
    }
}

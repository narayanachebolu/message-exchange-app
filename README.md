# Message Exchange System
A Java-based system that enables message exchange between players in both same-process and separate-process environments.

## Overview

This system implements a clean, extensible architecture for inter-player communication with the following key features:

- **Dual Communication Modes**: Supports both same-process (in-process) and separate-process (socket-based) communication
- **Clean Design**: Uses interfaces and dependency injection for maximum flexibility
- **Thread-Safe**: Handles concurrent operations safely
- **Comprehensive Testing**: Includes unit tests and integration tests
- **Easy Deployment**: Single JAR with convenient shell script

## Usage

### Same Process Communication

Both players run in the same JVM process using in-process communication:

```bash
./run.sh same-process
```

### Separate Process Communication

Players run in different JVM processes using socket communication:

**Terminal 1 (Server/Responder):**
```bash
./run.sh server 8080
```

**Terminal 2 (Client/Initiator):**
```bash
./run.sh client 8080 localhost
```

### Building and Testing

```bash
# Build the project
./run.sh build

# Run unit and integration tests
./run.sh test

# Complete demo (build + same-process execution)
./run.sh demo
```

## Message Exchange Rules

1. **Initialization**: Two players are created - an "initiator" and a "responder"
2. **Message Exchange**:
    - Initiator sends the first message ("Hello")
    - Responder receives the message and sends back a response containing the received message concatenated with their message counter
    - This continues for 10 rounds
3. **Stop Condition**: Message exchange ends after the initiator has sent 10 messages and received 10 responses

## Architecture

### Core Components

1. **Player**: The main entity that can send and receive messages
2. **Message/SimpleMessage**: Immutable message objects with content and metadata
3. **MessageExchangeChannel**: Abstract interface for communication mechanisms
4. **InProcessChannel**: Implementation for same-process communication
5. **SocketChannel**: Implementation for cross-process communication
6. **MessageExchangeCoordinator**: Orchestrates the message exchange
7. **CrossConnectedChannel**: Enables bidirectional communication

## Technical Details

### Requirements Met

- ✅ **Same Process**: Uses `InProcessChannel` with `BlockingQueue` for thread-safe communication
- ✅ **Separate Process**: Uses `SocketChannel` with TCP sockets and object serialization
- ✅ **Code Reuse**: Same `Player` and `MessageExchangeCoordinator` classes work for both modes
- ✅ **Pure Java**: No external frameworks, only standard JDK libraries
- ✅ **Maven Project**: Complete project structure with dependencies
- ✅ **Documentation**: Comprehensive class-level responsibility documentation
- ✅ **Testing**: Unit tests and integration tests with high coverage
- ✅ **Shell Script**: Convenient startup script with multiple modes

### Design Principles

1. **Separation of Concerns**: Each class has a single, well-defined responsibility
2. **Dependency Injection**: Players accept message exchange channels as dependencies
3. **Interface Segregation**: Clear contracts through interfaces
4. **Error Handling**: Comprehensive exception handling and resource cleanup
5. **Thread Safety**: Safe concurrent operations using appropriate synchronization
6. **Testability**: Mockable dependencies and comprehensive test coverage

## Dependencies

- **Java 11+**: Required runtime
- **Maven 3.6+**: For building
- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework for unit tests

## Error Handling

The system handles various error scenarios:

- Connection failures
- Message serialization/deserialization errors
- Network interruptions
- Resource cleanup on shutdown
- Thread interruption during blocking operations

## Performance Considerations

- **Memory Efficient**: Minimal object creation, reusable components
- **Non-blocking Design**: Uses appropriate blocking/non-blocking operations
- **Resource Management**: Proper cleanup of threads, sockets, and streams
- **Scalable Architecture**: Can be extended for multiple players or different transport mechanisms

## Future Enhancements

Potential improvements that maintain the current design:

1. **Support for Multiple Types**: Add UDP, JMS, or Async communication mechanisms
2. **Player Discovery**: Automatic player registration and discovery
3. **Message Persistence**: Optional message logging or replay functionality
4. **Performance Metrics**: Communication latency and throughput monitoring
5. **Configuration**: External configuration for ports, timeouts, etc.
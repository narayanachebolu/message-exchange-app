#!/bin/bash

# Message Exchange System Startup Script
# This script provides convenient ways to run the application in different modes

set -e  # Exit on any error

# Configuration
JAR_NAME="message-exchange-app-1.0.0.jar"
JAVA_OPTS="-Xms128m -Xmx512m"
DEFAULT_PORT=8080
DEFAULT_HOST="localhost"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper functions
print_usage() {
    echo -e "${BLUE}Message Exchange System - Startup Script${NC}"
    echo ""
    echo "Usage: $0 <mode> [options]"
    echo ""
    echo "Modes:"
    echo "  same-process          Run both players in the same JVM process"
    echo "  server <port>         Run as server (responder) player"
    echo "  client <port> [host]  Run as client (initiator) player"
    echo "  build                 Build the project using Maven"
    echo "  test                  Run unit and integration tests"
    echo "  demo                  Run a complete demo (builds, then runs same-process)"
    echo ""
    echo "Examples:"
    echo "  $0 same-process"
    echo "  $0 server 8080"
    echo "  $0 client 8080 localhost"
    echo "  $0 demo"
    echo ""
}

build_project() {
    echo -e "${BLUE}Building project with Maven...${NC}"
    if command -v mvn &> /dev/null; then
        mvn clean package -DskipTests=false
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}Build completed successfully!${NC}"
            return 0
        else
            echo -e "${RED}Build failed!${NC}"
            return 1
        fi
    else
        echo -e "${RED}Maven not found! Please install Maven to build the project.${NC}"
        return 1
    fi
}

run_tests() {
    echo -e "${BLUE}Running tests with Maven...${NC}"
    if command -v mvn &> /dev/null; then
        mvn test
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}All tests passed!${NC}"
            return 0
        else
            echo -e "${RED}Some tests failed!${NC}"
            return 1
        fi
    else
        echo -e "${RED}Maven not found! Please install Maven to run tests.${NC}"
        return 1
    fi
}

run_integration_tests() {
    echo -e "${BLUE}Running integration-tests with Maven...${NC}"
    if command -v mvn &> /dev/null; then
        mvn failsafe:integration-test
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}All integration-tests passed!${NC}"
            return 0
        else
            echo -e "${RED}Some integration-tests failed!${NC}"
            return 1
        fi
    else
        echo -e "${RED}Maven not found! Please install Maven to run integration-tests.${NC}"
        return 1
    fi
}

check_jar_exists() {
    if [ ! -f "target/$JAR_NAME" ]; then
        echo -e "${YELLOW}JAR file not found. Building project...${NC}"
        build_project
        if [ $? -ne 0 ]; then
            echo -e "${RED}Failed to build project. Exiting.${NC}"
            exit 1
        fi
    fi
}

run_same_process() {
    echo -e "${GREEN}Starting same-process message exchange application...${NC}"
    java $JAVA_OPTS -jar "target/$JAR_NAME" same-process
}

run_server() {
    local port=${1:-$DEFAULT_PORT}
    echo -e "${GREEN}Starting server on port $port...${NC}"
    echo -e "${YELLOW}Press Ctrl+C to stop the server${NC}"
    java $JAVA_OPTS -jar "target/$JAR_NAME" separate-process server $port
}

run_client() {
    local port=${1:-$DEFAULT_PORT}
    local host=${2:-$DEFAULT_HOST}
    echo -e "${GREEN}Starting client connecting to $host:$port...${NC}"
    java $JAVA_OPTS -jar "target/$JAR_NAME" separate-process client $port $host
}

run_demo() {
    echo -e "${BLUE}Running complete demo...${NC}"
    build_project
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${BLUE}Demo: Running same-process message exchange application.${NC}"
        run_same_process
    fi
}

start_separate_process_demo() {
    echo -e "${BLUE}Starting separate process demo...${NC}"
    echo -e "${YELLOW}This will start server in background and then run client${NC}"

    # Start server in background
    echo -e "${GREEN}Starting server on port $DEFAULT_PORT...${NC}"
    java $JAVA_OPTS -jar "target/$JAR_NAME" separate-process server $DEFAULT_PORT &
    SERVER_PID=$!

    # Give server time to start
    echo "Waiting for server to start..."
    sleep 3

    # Start client
    echo -e "${GREEN}Starting client...${NC}"
    java $JAVA_OPTS -jar "target/$JAR_NAME" separate-process client $DEFAULT_PORT $DEFAULT_HOST

    # Clean up server
    echo -e "${YELLOW}Stopping server...${NC}"
    kill $SERVER_PID 2>/dev/null || true
    wait $SERVER_PID 2>/dev/null || true
    echo -e "${GREEN}Demo completed!${NC}"
}

# Main script logic
case "${1:-}" in
    "same-process")
        check_jar_exists
        run_same_process
        ;;

    "server")
        check_jar_exists
        run_server "$2"
        ;;

    "client")
        check_jar_exists
        run_client "$2" "$3"
        ;;

    "build")
        build_project
        ;;

    "test")
        run_tests
        ;;

    "integration-test")
        run_integration_tests
        ;;

    "demo")
        run_demo
        ;;

    "separate-demo")
        check_jar_exists
        start_separate_process_demo
        ;;

    "")
        print_usage
        ;;

    *)
        echo -e "${RED}Unknown mode: $1${NC}"
        print_usage
        exit 1
        ;;
esac
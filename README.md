[English] | [Русский](README.ru.md)
# RUDP-CLI

A command-line utility for reliable file transfer built over UDP. The project implements an application-layer protocol to add extra reliability to raw DatagramSockets.

## Capabilities
*   **Reliability:** Implements Stop-and-Wait ARQ to handle dropped packets.
*   **Integrity:** Detects corruption using CRC32 header checksums
*   **Storage:** Manages directory creation based on client requests.
*   **Dependency free:** only uses standard Java libraries.

## Usage

Java 17+ using compiled jar.

### Start Server
```bash
java -jar rudp.jar server [optional_root_path]
```

### Send File
File -> target server
```bash
java -jar rudp.jar send <source_file> <server_ip> [remote_path]
```

## Protocol Implementation
The system bypasses TCP to manage reliability in user-space:
*   **Packet Structure:** 13-byte Binary Header (Sequence ID, Type, CRC32) + 8KB Payload.
*   **Handshake:** 3-way negotiation to establish filename and destination before data transfer.
*   **Flow Control:** Timeouts and retransmissions ensure delivery even in lossy network conditions.

## Build from source

1.  **Compile (from root folder):**
    ```bash
    javac src/main/java/org/subbdds/compile/*.java
    ```
2.  **Package:**
    ```bash
    jar cfm rudp.jar manifest.txt -C bin .
    ```

package org.subbdds.client;

import org.subbdds.server.Packet;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.File;
import java.io.FileInputStream;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class UDPClient {
    private static final int SERVER_PORT = 9876;
    private static final String SERVER_ADDRESS = "localhost";
    private static final String FILE_PATH = "test_image.png"; // Put your image here

    public static void main(String[] args) throws Exception {
        System.out.println("--- Starting RUDP File Transfer Client ---");

        // 1. Setup Network
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(1000); // 1-second timeout for ACKs
        InetAddress ipAddress = InetAddress.getByName(SERVER_ADDRESS);

        // 2. Setup File I/O
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            System.err.println("❌ Error: File not found at " + file.getAbsolutePath());
            return;
        }
        FileInputStream fis = new FileInputStream(file);

        // Calculate Payload size: 1024 (Max UDP safe) - 13 (Header) = 1011 bytes max.
        // We use 1000 to be safe and have round numbers.
        byte[] fileBuffer = new byte[1000];
        int bytesRead;
        int seqNum = 0;

        long totalBytes = file.length();
        long totalSent = 0;

        System.out.println("Sending: " + file.getName() + " (" + totalBytes + " bytes)");

        // --- MAIN FILE READING LOOP ---
        while ((bytesRead = fis.read(fileBuffer)) != -1) {

            // Create a byte array of the EXACT size read
            byte[] exactData = Arrays.copyOf(fileBuffer, bytesRead);

            // 1. Create the Master (Clean) Packet for this chunk
            Packet cleanPacket = new Packet(seqNum, Packet.TYPE_DATA, exactData);
            byte[] cleanBytes = cleanPacket.toBytes();

            boolean ackReceived = false;
            int retries = 0;

            // --- RETRY LOOP (Stop-and-Wait) ---
            while (!ackReceived) {
                // 2. Clone the bytes for this specific attempt
                // If we corrupt 'attemptBytes', 'cleanBytes' remains safe for the next retry.
                byte[] attemptBytes = cleanBytes.clone();

                // 3. THE SABOTEUR (Simulate Corruption)
                // 10% chance to corrupt the packet to prove Checksums work
                if (Math.random() < 0.1) {
                    System.out.print("[Simulating Corruption] ");
                    // Flip the last byte to invalidate the Checksum
                    attemptBytes[attemptBytes.length - 1]++;
                }

                DatagramPacket sendPacket = new DatagramPacket(attemptBytes, attemptBytes.length, ipAddress, SERVER_PORT);

                try {
                    // 4. Send Packet
                    socket.send(sendPacket);

                    // 5. Wait for ACK
                    byte[] ackBuffer = new byte[1024]; // Big enough for any ACK
                    DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                    socket.receive(ackPacket); // BLOCKS here until Timeout

                    // 6. Validate ACK
                    Packet ack = Packet.fromBytes(ackPacket.getData(), ackPacket.getLength());

                    if (ack.getType() == Packet.TYPE_ACK && ack.getSequenceNumber() == seqNum) {
                        ackReceived = true;

                        // Update Progress Bar
                        totalSent += bytesRead;
                        int percent = (int) ((totalSent * 100) / totalBytes);
                        System.out.print("\rUploading: [" + "#".repeat(percent/5) + " ".repeat(20 - percent/5) + "] " + percent + "%");
                    }

                } catch (SocketTimeoutException e) {
                    // 7. Handle Timeout
                    System.out.print("X"); // Visual indicator of a retry
                    retries++;
                    if (retries > 10) {
                        System.out.println("\n❌ Critical Failure: Server stopped responding.");
                        System.exit(1);
                    }
                } catch (Exception e) {
                    // Handle corruption on ACK receipt (rare, but possible)
                    System.out.print("?");
                }
            }

            // Move to next chunk
            seqNum++;
        }

        // --- FIN LOOP ---
        // Tell Server we are done. Even the FIN packet needs retries!
        System.out.println("\nFile sent. Sending FIN...");
        Packet finPacket = new Packet(seqNum, Packet.TYPE_FIN, new byte[0]);
        byte[] finBytes = finPacket.toBytes();
        boolean finAck = false;

        // Try sending FIN up to 5 times
        for (int i = 0; i < 5 && !finAck; i++) {
            socket.send(new DatagramPacket(finBytes, finBytes.length, ipAddress, SERVER_PORT));
            try {
                socket.receive(new DatagramPacket(new byte[1024], 1024));
                finAck = true; // Any response means server got it
            } catch (SocketTimeoutException e) {
                // Retry FIN
            }
        }

        System.out.println("Transfer Complete!");
        fis.close();
        socket.close();
    }
}
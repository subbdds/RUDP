package org.subbdds.compile;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;


public class UDPClient {
    private static final int PORT = 9876;
    private static final int TIMEOUT = 1000;
    private static final int MAX_RETRIES = 5;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java UDPClient <file_path> <server_ip>");
            return;
        }

        String filePath = args[0];
        String serverIp = args[1];

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT);
            InetAddress address = InetAddress.getByName(serverIp);
            File file = new File(filePath);

            if (!file.exists()) {
                System.err.println("File not found: " + filePath);
                return;
            }

            // 1. HANDSHAKE: Send Filename
            System.out.println("Step 1: Handshaking metadata...");
            sendWithRetry(socket, new Packet(0, Packet.TYPE_METADATA, file.getName().getBytes()), address);

            // 2. DATA TRANSFER
            System.out.println("Step 2: Sending data...");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[1000];
                int bytesRead;
                int seqNum = 1; // Start data at Seq 1
                long totalSent = 0;
                long fileSize = file.length();

                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] data = Arrays.copyOf(buffer, bytesRead);
                    Packet p = new Packet(seqNum, Packet.TYPE_DATA, data);

                    sendWithRetry(socket, p, address);

                    totalSent += bytesRead;
                    printProgress(totalSent, fileSize);
                    seqNum++;
                }
            }

            // 3. TERMINATION
            System.out.println("\nStep 3: Closing connection...");
            sendWithRetry(socket, new Packet(-1, Packet.TYPE_FIN, new byte[0]), address);

            System.out.println("Transfer Successful.");

        } catch (Exception e) {
            System.err.println("\nTransfer Failed: " + e.getMessage());
        }
    }

    // Reliable send helper
    private static void sendWithRetry(DatagramSocket socket, Packet packet, InetAddress address) throws IOException {
        byte[] bytes = packet.toBytes();
        DatagramPacket udpPacket = new DatagramPacket(bytes, bytes.length, address, PORT);
        byte[] ackBuf = new byte[1024];
        int attempts = 0;

        while (attempts < MAX_RETRIES) {
            try {
                socket.send(udpPacket);
                DatagramPacket ackDatagram = new DatagramPacket(ackBuf, ackBuf.length);
                socket.receive(ackDatagram);

                Packet ack = Packet.fromBytes(ackDatagram.getData(), ackDatagram.getLength());
                if (ack.getType() == Packet.TYPE_ACK && ack.getSequenceNumber() == packet.getSequenceNumber()) {
                    return; // Success
                }
            } catch (SocketTimeoutException e) {
                // Keep looping to retry
                attempts++;
            } catch (RuntimeException e) {
                // Checksum failure on ACK - just retry
                attempts++;
            }
        }
        throw new IOException("Failed to send packet after " + MAX_RETRIES + " attempts");
    }

    private static void printProgress(long current, long total) {
        int percent = (int) ((current * 100) / total);
        System.out.print("\rProgress: [" + "#".repeat(percent/5) + " ".repeat(20 - percent/5) + "] " + percent + "%");
    }
}
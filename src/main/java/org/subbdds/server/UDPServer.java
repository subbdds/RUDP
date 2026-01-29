package org.subbdds.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.FileOutputStream;

public class UDPServer {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(9876);
        byte[] receiveData = new byte[1024];

        // Prepare Output File
        try (FileOutputStream fos = new FileOutputStream("output_image.png")) {
            int expectedSeqNum = 0;

            System.out.println("Server ready to receive file...");

            boolean transferComplete = false;
            while (!transferComplete) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                try {
                    // This line now runs the CRC32 check automatically
                    Packet p = Packet.fromBytes(receivePacket.getData(), receivePacket.getLength());

                    // --- NORMAL LOGIC ---
                    if (p.getType() == Packet.TYPE_DATA) {
                        if (p.getSequenceNumber() == expectedSeqNum) {
                            fos.write(p.getPayload());
                            expectedSeqNum++;
                        } else {
                            System.out.println("Duplicate/Out-of-order: " + p.getSequenceNumber());
                        }
                        // Send ACK
                        Packet ack = new Packet(p.getSequenceNumber(), Packet.TYPE_ACK, new byte[0]);
                        byte[] ackBytes = ack.toBytes();
                        socket.send(new DatagramPacket(ackBytes, ackBytes.length, receivePacket.getAddress(), receivePacket.getPort()));
                    }
                    else if (p.getType() == Packet.TYPE_FIN) {
                        System.out.println("\nReceived FIN. Closing file.");
                        transferComplete = true;
                    }

                } catch (RuntimeException e) {
                    // --- CORRUPTION LOGIC ---
                    if (e.getMessage().equals("CORRUPT_PACKET")) {
                        System.out.println("⚠️ Received Corrupt Packet! Dropping it (Sender will timeout/retry).");
                        // Do NOTHING. Do NOT send ACK.
                    } else {
                        e.printStackTrace();
                    }
                }
            }
        }
        // socket.close(); // Don't close socket if you want to restart server
    }
}
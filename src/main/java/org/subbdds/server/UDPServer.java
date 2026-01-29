package org.subbdds.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.FileOutputStream;

public class UDPServer {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(9876);
        byte[] receiveData = new byte[1024];

        // Prepare Output File
        try (FileOutputStream fos = new FileOutputStream("output_image.jpg")) {
            int expectedSeqNum = 0;

            System.out.println("Server ready to receive file...");

            boolean transferComplete = false;
            while (!transferComplete) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                Packet p = Packet.fromBytes(receivePacket.getData(), receivePacket.getLength());

                // --- HANDLING DATA PACKETS ---
                if (p.getType() == Packet.TYPE_DATA) {

                    // Deduplication Logic
                    if (p.getSequenceNumber() == expectedSeqNum) {
                        fos.write(p.getPayload()); // Write to disk
                        expectedSeqNum++;          // Expect the next one
                    } else {
                        // It's a duplicate or out-of-order packet
                        System.out.println("Duplicate detected: " + p.getSequenceNumber());
                    }

                    // Send ACK
                    Packet ack = new Packet(p.getSequenceNumber(), Packet.TYPE_ACK, new byte[0]);
                    byte[] ackBytes = ack.toBytes();
                    DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, receivePacket.getAddress(), receivePacket.getPort());
                    socket.send(ackPacket);
                }

                // --- HANDLING FIN PACKET ---
                else if (p.getType() == Packet.TYPE_FIN) {
                    System.out.println("\nReceived FIN. Closing file.");
                    transferComplete = true;
                }
            }
        }
        // socket.close(); // Don't close socket if you want to restart server
    }
}
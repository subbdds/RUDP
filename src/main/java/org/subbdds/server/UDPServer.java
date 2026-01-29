package org.subbdds.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPServer {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(9876);
        byte[] receiveData = new byte[1024];

        System.out.println("Server listening...");

        // Inside UDPServer.java (Main Loop)

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            Packet p = Packet.fromBytes(receivePacket.getData(), receivePacket.getLength());

            if (p.getType() == Packet.TYPE_DATA) {
                System.out.println("Received Data: Seq " + p.getSequenceNumber());

                // --- SEND ACK ---

                // 1. Create the ACK packet
                Packet ack = new Packet(p.getSequenceNumber(), Packet.TYPE_ACK, new byte[0]);
                byte[] ackBytes = ack.toBytes();

                // 2. Address it to the Sender
                DatagramPacket ackPacket = new DatagramPacket(
                        ackBytes,
                        ackBytes.length,
                        receivePacket.getAddress(),
                        receivePacket.getPort() // Client's port
                );

                if (Math.random() < 0.5) { // 50% chance to fail
                    System.out.println("SIMULATING NETWORK LOSS (Dropping ACK)"); // Simulate packet loss
                    continue;
                }

                socket.send(ackPacket);
                // 3. Send it back
                socket.send(ackPacket);
                System.out.println(" -> Sent ACK " + p.getSequenceNumber());
            }
        }
    }
}
package org.subbdds.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPServer {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(9876);
        byte[] receiveData = new byte[1024];

        System.out.println("Server listening...");

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            // DESERIALIZE: Raw Bytes -> Packet Object
            Packet p = Packet.fromBytes(receivePacket.getData(), receivePacket.getLength());

            // Now we can access the metadata elegantly
            System.out.println("Received Packet:");
            System.out.println(" - Seq Num: " + p.getSequenceNumber());
            System.out.println(" - Type:    " + p.getType());
            System.out.println(" - Payload: " + new String(p.getPayload()));
            System.out.println("--------------------------");
        }
    }
}
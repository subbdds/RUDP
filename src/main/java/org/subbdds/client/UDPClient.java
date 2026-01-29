package org.subbdds.client;

import org.subbdds.server.Packet;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPClient {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName("localhost");
        int port = 9876;

        // Simulate sending 3 chunks of data
        for (int i = 0; i < 3; i++) {
            String message = "Chunk " + i;

            // Create a structured packet: SeqNum=100+i, Type=DATA
            Packet p = new Packet(100 + i, Packet.TYPE_DATA, message.getBytes());

            // SERIALIZE (Get the raw bytes)
            byte[] rawBytes = p.toBytes();

            DatagramPacket sendPacket = new DatagramPacket(rawBytes, rawBytes.length, IPAddress, port);

            System.out.println("Sending Packet #" + p.getSequenceNumber());
            socket.send(sendPacket);

            Thread.sleep(500); // Slow down slightly so we can see it
        }

        socket.close();
    }
}
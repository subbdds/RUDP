package org.subbdds.client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPClient {
    public static void main(String[] args) throws Exception {
        // 1. Create a socket
        // Note: We don't specify a port here. The OS will assign a random open port (e.g., 54321).
        DatagramSocket socket = new DatagramSocket();

        // 2. Prepare the data
        String message = "Hello from the Client!";
        byte[] sendData = message.getBytes();

        // 3. Define the Destination
        // "localhost" means the same machine.
        // If testing on two different laptops, put the Server's real IP here (e.g., "192.168.1.5")
        InetAddress IPAddress = InetAddress.getByName("localhost");
        int port = 9876;

        // 4. Create the Packet
        // The packet contains: [ The Data ] + [ The Destination IP ] + [ The Destination Port ]
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);

        // 5. Send it
        System.out.println("Sending packet...");
        socket.send(sendPacket);

        System.out.println("Packet sent!");

        // 6. Close the resource
        socket.close();
    }
}
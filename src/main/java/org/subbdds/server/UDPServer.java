package org.subbdds.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPServer {
    public static void main(String[] args) throws Exception {
        // 1. Open a UDP Socket on Port 9876
        // This tells the OS: "Send any UDP packets arriving at Port 9876 to me."
        DatagramSocket socket = new DatagramSocket(9876);

        System.out.println("🚀 Server started on port 9876. Waiting for packets...");

        // 2. Create a buffer to hold incoming data.
        // UDP packets have a size limit (usually ~64KB).
        // We'll use 1024 bytes for now, which is plenty for text.
        byte[] receiveData = new byte[1024];

        while (true) {
            // 3. Create a DatagramPacket to hold the incoming data
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            // 4. BLOCKING CALL: The program pauses here!
            // It waits until a packet actually arrives.
            socket.receive(receivePacket);

            // 5. Extract the data
            // getData() returns the raw bytes.
            // getLength() tells us how many bytes were actually sent (e.g., "Hello" is 5 bytes).
            String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

            // 6. Print who sent it
            // We can see the IP and Port of the sender.
            System.out.println("Received from " + receivePacket.getAddress() + ": " + message);

            // Note: We are NOT sending a response back yet.
        }
    }
}
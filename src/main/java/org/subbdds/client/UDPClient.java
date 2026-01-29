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

        socket.setSoTimeout(1000);

        for (int i = 0; i < 5; i++) {
            String message = "Chunk " + i;
            int seqNum = 100 + i;

            // Create the Data Packet
            Packet dataPacket = new Packet(seqNum, Packet.TYPE_DATA, message.getBytes());
            byte[] dataBytes = dataPacket.toBytes();
            DatagramPacket sendPacket = new DatagramPacket(dataBytes, dataBytes.length, IPAddress, port);

            boolean ackReceived = false;
            int retries = 0;
            int MAX_RETRIES = 5;

            // --- THE RETRY LOOP ---
            while (!ackReceived && retries < MAX_RETRIES) {
                try {
                    // 1. Send Data
                    System.out.println("Sending Seq " + seqNum + " (Attempt " + (retries + 1) + ")");
                    socket.send(sendPacket);

                    // 2. Wait for ACK (This blocks!)
                    byte[] buffer = new byte[1024];
                    DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                    
                    socket.receive(incomingPacket); // <--- Will throw Exception if timeout

                    // 3. Deserialize and Check
                    Packet ack = Packet.fromBytes(incomingPacket.getData(), incomingPacket.getLength());
                    
                    if (ack.getType() == Packet.TYPE_ACK && ack.getSequenceNumber() == seqNum) {
                        System.out.println("Received ACK " + seqNum + ". Moving on.\n");
                        ackReceived = true;
                    }

                } catch (java.net.SocketTimeoutException e) {
                    // 4. TIMEOUT CAUGHT
                    System.out.println("Timeout waiting for ACK " + seqNum + ". Retrying...");
                    retries++;
                }
            }

            if (!ackReceived) {
                System.out.println("CRITICAL FAILURE: Gave up on Seq " + seqNum);
                break;
            }
        }

        socket.close();
    }
}
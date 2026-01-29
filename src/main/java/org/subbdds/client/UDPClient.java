package org.subbdds.client;

import org.subbdds.server.Packet;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.File;
import java.io.FileInputStream;

public class UDPClient {
    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(1000); // 1 sec timeout
        InetAddress IPAddress = InetAddress.getByName("localhost");
        int port = 9876;

        // 1. Prepare the File
        File file = new File("test_image.png");
        FileInputStream fis = new FileInputStream(file);

        // Buffer for the payload (Max 1019 bytes)
        byte[] fileBuffer = new byte[1019];
        int bytesRead;
        int seqNum = 0;

        System.out.println("Starting transfer of " + file.getName() + " (" + file.length() + " bytes)");

        // 2. Read file in chunks
        while ((bytesRead = fis.read(fileBuffer)) != -1) {

            // Create a byte array of the size read
            byte[] exactData = new byte[bytesRead];
            System.arraycopy(fileBuffer, 0, exactData, 0, bytesRead);

            // Create Packet
            Packet p = new Packet(seqNum, Packet.TYPE_DATA, exactData);
            byte[] rawBytes = p.toBytes();
            DatagramPacket sendPacket = new DatagramPacket(rawBytes, rawBytes.length, IPAddress, port);

            // 3. Stop-and-Wait Logic
            boolean ackReceived = false;
            while (!ackReceived) {
                try {
                    socket.send(sendPacket);

                    byte[] ackBuffer = new byte[1024];
                    DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                    socket.receive(ackPacket);

                    Packet ack = Packet.fromBytes(ackPacket.getData(), ackPacket.getLength());
                    if (ack.getType() == Packet.TYPE_ACK && ack.getSequenceNumber() == seqNum) {
                        ackReceived = true;
                        System.out.print("."); // Progress bar
                    }
                } catch (Exception e) {
                    System.out.print("X"); // Timeout indicator
                }
            }
            seqNum++;
        }

        // 4. Send FIN Packet
        Packet fin = new Packet(seqNum, Packet.TYPE_FIN, new byte[0]);
        byte[] finBytes = fin.toBytes();
        socket.send(new DatagramPacket(finBytes, finBytes.length, IPAddress, port));

        System.out.println("\nTransfer Complete!");
        fis.close();
        socket.close();
    }
}
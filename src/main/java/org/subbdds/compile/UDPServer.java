package org.subbdds.compile;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.FileOutputStream;
import java.io.*;
import java.net.*;

public class UDPServer {
    private static final int PORT = 9876;

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(PORT);
        byte[] buffer = new byte[2048];

        while (true) {
            System.out.println("\n📡 Server Listening on port " + PORT + "...");

            FileOutputStream fos = null;
            int expectedSeq = 0;
            boolean activeTransfer = true;

            while (activeTransfer) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                try {
                    Packet p = Packet.fromBytes(request.getData(), request.getLength());

                    // LOGIC 1: Metadata (Handshake)
                    if (p.getType() == Packet.TYPE_METADATA) {
                        if (expectedSeq == 0) {
                            String filename = new String(p.getPayload());
                            System.out.println("Incoming File: " + filename);
                            fos = new FileOutputStream("received_" + filename);
                            expectedSeq = 1;
                        }
                        sendAck(socket, p.getSequenceNumber(), request.getAddress(), request.getPort());
                    }

                    // LOGIC 2: Data
                    else if (p.getType() == Packet.TYPE_DATA) {
                        if (p.getSequenceNumber() == expectedSeq) {
                            if (fos != null) fos.write(p.getPayload());
                            expectedSeq++;
                        }
                        // Always ACK current or previous seq to keep client moving
                        sendAck(socket, p.getSequenceNumber(), request.getAddress(), request.getPort());
                    }

                    // LOGIC 3: Finish
                    else if (p.getType() == Packet.TYPE_FIN) {
                        System.out.println("Transfer Complete. Saving file.");
                        sendAck(socket, p.getSequenceNumber(), request.getAddress(), request.getPort());
                        activeTransfer = false;
                    }

                } catch (RuntimeException e) {
                    System.err.println("⚠️ Corrupt packet ignored.");
                }
            }
            if (fos != null) fos.close();
        }
    }

    private static void sendAck(DatagramSocket socket, int seq, InetAddress addr, int port) throws IOException {
        Packet ack = new Packet(seq, Packet.TYPE_ACK, new byte[0]);
        byte[] bytes = ack.toBytes();
        socket.send(new DatagramPacket(bytes, bytes.length, addr, port));
    }
}
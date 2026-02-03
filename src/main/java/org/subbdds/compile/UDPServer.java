package org.subbdds.compile;

import java.io.*;
import java.net.*;

public class UDPServer {
    private static final int PORT = 9876;

    public static void main(String[] args) throws Exception {
        DatagramSocket socket = new DatagramSocket(PORT);
        byte[] buffer = new byte[32768];

        System.out.println("\nActive on port " + PORT);

        while (true) {
            FileOutputStream fos = null;
            int expectedSeq = 0;
            boolean activeTransfer = true;

            while (activeTransfer) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                try {
                    Packet p = Packet.fromBytes(request.getData(), request.getLength());

                    if (p.getType() == Packet.TYPE_METADATA && expectedSeq == 0) {
                        String destinationPath = new String(p.getPayload());

                        // 1. Determine target file path
                        File targetFile = new File(destinationPath);

                        // 2. If relative, store in "received_files" directory
                        if (!targetFile.isAbsolute()) {
                            targetFile = new File("received_files", destinationPath);
                        }

                        try {
                            // 3. Create parent directories if needed
                            File parent = targetFile.getParentFile();
                            if (parent != null) parent.mkdirs();

                            System.out.println("Target: " + targetFile.getAbsolutePath());
                            fos = new FileOutputStream(targetFile);

                            // Success! Send ACK
                            sendAck(socket, p.getSequenceNumber(), request.getAddress(), request.getPort());
                            expectedSeq = 1;
                        } catch (FileNotFoundException e) {
                            System.err.println("Permission Denied or Invalid Path: " + targetFile.getAbsolutePath());
                            // Send NACK by not incrementing expectedSeq
                        }
                    }

                    else if (p.getType() == Packet.TYPE_DATA) {
                        if (p.getSequenceNumber() == expectedSeq) {
                            if (fos != null) fos.write(p.getPayload());
                            expectedSeq++;
                        }
                        sendAck(socket, p.getSequenceNumber(), request.getAddress(), request.getPort());
                    }

                    else if (p.getType() == Packet.TYPE_FIN) {
                        System.out.println("done.");
                        sendAck(socket, p.getSequenceNumber(), request.getAddress(), request.getPort());
                        activeTransfer = false;
                    }

                } catch (Exception e) {
                    // Corruption or parsing error
                }
            }
            if (fos != null) fos.close();
        }
    }

    private static void sendAck(
            DatagramSocket socket,
            int seq,
            InetAddress addr,
            int port)
            throws IOException {

        Packet ack = new Packet(seq, Packet.TYPE_ACK, new byte[0]);
        byte[] bytes = ack.toBytes();
        socket.send(new DatagramPacket(bytes, bytes.length, addr, port));
    }
}
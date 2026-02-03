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

        // 1. Determine Output Directory
        String outputDirName = (args.length > 0) ? args[0] : "downloads";
        File outputDir = new File(outputDirName);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        System.out.println("\nServer Listening on port " + PORT);
        System.out.println("Saving files to: " + outputDir.getAbsolutePath());

        while (true) {
            FileOutputStream fos = null;
            int expectedSeq = 0;
            boolean activeTransfer = true;

            while (activeTransfer) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                try {
                    Packet p = Packet.fromBytes(request.getData(), request.getLength());

                    // LOGIC 1: Metadata (Handshake)
                    if (p.getType() == Packet.TYPE_METADATA && expectedSeq == 0) {
                        String originalName = new String(p.getPayload());

                        // 2. Smart Renaming Logic
                        File saveFile = resolveFileName(outputDir, originalName);

                        System.out.println("⬇Incoming: " + originalName + " -> Saving as: " + saveFile.getName());
                        fos = new FileOutputStream(saveFile);

                        sendAck(socket, p.getSequenceNumber(), request.getAddress(), request.getPort());
                        expectedSeq = 1;
                    }

                    // LOGIC 2: Data
                    else if (p.getType() == Packet.TYPE_DATA) {
                        if (p.getSequenceNumber() == expectedSeq) {
                            if (fos != null) fos.write(p.getPayload());
                            expectedSeq++;
                        }
                        sendAck(socket, p.getSequenceNumber(), request.getAddress(), request.getPort());
                    }

                    // LOGIC 3: Finish
                    else if (p.getType() == Packet.TYPE_FIN) {
                        System.out.println("Transfer Complete.\nWaiting for next file...");
                        sendAck(socket, p.getSequenceNumber(), request.getAddress(), request.getPort());
                        activeTransfer = false;
                    }

                } catch (RuntimeException e) {
                    // Silent drop for corruption
                }
            }
            if (fos != null) fos.close();
        }
    }

    // Helper: Handles file (1).jpg logic
    private static File resolveFileName(File dir, String filename) {
        File file = new File(dir, filename);
        if (!file.exists()) return file; // No conflict? Return original.

        // Split name and extension
        String name = filename;
        String ext = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            name = filename.substring(0, dotIndex);
            ext = filename.substring(dotIndex);
        }

        // Loop until we find a free number: file (1), file (2)...
        int counter = 1;
        while (file.exists()) {
            file = new File(dir, name + " (" + counter + ")" + ext);
            counter++;
        }
        return file;
    }

    private static void sendAck(DatagramSocket socket, int seq, InetAddress addr, int port) throws IOException {
        Packet ack = new Packet(seq, Packet.TYPE_ACK, new byte[0]);
        byte[] bytes = ack.toBytes();
        socket.send(new DatagramPacket(bytes, bytes.length, addr, port));
    }
}
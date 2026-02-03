package org.subbdds.compile;

import java.util.Arrays;

public class RUDP {
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String mode = args[0].toLowerCase();

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        try {
            switch (mode) {
                case "server":
                case "listen":
                    UDPServer.main(subArgs);
                    break;
                case "client":
                case "send":
                    UDPClient.main(subArgs);
                    break;
                default:
                    System.err.println("Unknown mode: " + mode);
                    printUsage();
                    System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("R-UDP Tool v1.0");
        System.out.println("Usage:");
        System.out.println("  Start Server: java -jar rudp.jar server [save_path]");
        System.out.println("  Send File:    java -jar rudp.jar send <source_file> <ip> [dest_path]");
    }
}
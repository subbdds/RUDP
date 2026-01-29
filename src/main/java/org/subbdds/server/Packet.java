package org.subbdds.server;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Packet {
    public static final int HEADER_SIZE = 5;

    // Packet Types
    public static final byte TYPE_DATA = 0;
    public static final byte TYPE_ACK = 1;
    public static final byte TYPE_FIN = 2;

    private int sequenceNumber;
    private byte type;
    private byte[] payload;

    // Constructor for creating a packet object
    public Packet(int sequenceNumber, byte type, byte[] payload) {
        this.sequenceNumber = sequenceNumber;
        this.type = type;
        this.payload = payload;
    }

    // 1. SERIALIZE: Convert Object -> Byte Array (To send over network)
    public byte[] toBytes() {
        int payloadSize = (payload != null) ? payload.length : 0;

        // Allocate memory: Header + Payload
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + payloadSize);

        buffer.putInt(sequenceNumber); // Write 4 bytes
        buffer.put(type);              // Write 1 byte

        if (payload != null) {
            buffer.put(payload);       // Write remaining bytes
        }

        return buffer.array(); // Return the raw byte array
    }

    // 2. DESERIALIZE: Convert Byte Array -> Object (To read incoming data)
    public static Packet fromBytes(byte[] data, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(data, 0, length);

        int seq = buffer.getInt(); // Read first 4 bytes
        byte type = buffer.get();  // Read next 1 byte

        // Calculate payload size
        byte[] payload = new byte[length - HEADER_SIZE];
        buffer.get(payload);       // Read the rest

        return new Packet(seq, type, payload);
    }

    // Getters so we can read the data later
    public int getSequenceNumber() { return sequenceNumber; }
    public byte getType() { return type; }
    public byte[] getPayload() { return payload; }
}
package org.subbdds.compile;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class Packet {
    public static final int HEADER_SIZE = 13;

    public static final byte TYPE_DATA = 0;
    public static final byte TYPE_ACK = 1;
    public static final byte TYPE_FIN = 2;
    public static final byte TYPE_METADATA = 3;


    private int sequenceNumber;
    private byte type;
    private long checksum;
    private byte[] payload;

    // Constructor 1
    public Packet(int sequenceNumber, byte type, byte[] payload) {
        this.sequenceNumber = sequenceNumber;
        this.type = type;
        this.payload = payload;

        // Calculate the math hash of the payload
        CRC32 crc = new CRC32();
        if (payload != null) {
            crc.update(payload);
        }
        this.checksum = crc.getValue(); 
    }

    // Constructor 2
    private Packet(int sequenceNumber, byte type, long checksum, byte[] payload) {
        this.sequenceNumber = sequenceNumber;
        this.type = type;
        this.checksum = checksum;
        this.payload = payload;
    }

    // SERIALIZE: Object -> Bytes
    public byte[] toBytes() {
        int payloadSize = (payload != null) ? payload.length : 0;
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + payloadSize);

        buffer.putInt(sequenceNumber);
        buffer.put(type);
        buffer.putLong(checksum); 

        if (payload != null) {
            buffer.put(payload);
        }
        return buffer.array();
    }

    // DESERIALIZE: Bytes -> Object
    public static Packet fromBytes(byte[] data, int length) {
        if (length < HEADER_SIZE) throw new RuntimeException("Packet too short");

        ByteBuffer buffer = ByteBuffer.wrap(data, 0, length);

        int seq = buffer.getInt();
        byte type = buffer.get();
        long receivedChecksum = buffer.getLong();

        byte[] payload = new byte[length - HEADER_SIZE];
        buffer.get(payload);

        // --- INTEGRITY CHECK ---
        CRC32 calculator = new CRC32();
        calculator.update(payload);
        long calculatedChecksum = calculator.getValue(); 

        // Compare
        if (receivedChecksum != calculatedChecksum) {
            throw new RuntimeException("CORRUPT_PACKET");
        }

        return new Packet(seq, type, receivedChecksum, payload);
    }

    // Getters
    public int getSequenceNumber() { return sequenceNumber; }
    public byte getType() { return type; }
    public byte[] getPayload() { return payload; }
}

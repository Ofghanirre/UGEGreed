package fr.uge.ugegreed.packets;

import java.nio.ByteBuffer;

/**
 * Represents an UPDT packet
 * @param potential potential of the network
 */
public record UpdtPacket(int potential, int appID) implements Packet  {
    private static final byte CODE = Packet.PacketCode.UPDT.getCode();

    public UpdtPacket {
        if (potential < 0) {
            throw new IllegalArgumentException("potential must be positive");
        }
    }

    @Override
    public ByteBuffer toBuffer() {
        return ByteBuffer.allocate(Byte.BYTES + Integer.BYTES * 2).put(CODE).putInt(potential).putInt(appID).flip();
    }

    @Override
    public String toString() {
        return "UPDT packet(potential: " + potential + ", appID: " + appID + ")";
    }
}

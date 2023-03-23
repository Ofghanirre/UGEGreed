package fr.uge.ugegreed.packets;

import java.nio.ByteBuffer;

/**
 * Represents an INIT packet
 * @param potential potential of the network
 */
public record InitPacket(int potential) implements Packet {
  private static final byte CODE = Packet.PacketCode.INIT.getCode();

  public InitPacket {
    if (potential < 1) {
      throw new IllegalArgumentException("potential must be positive");
    }
  }

  @Override
  public ByteBuffer toBuffer() {
    return ByteBuffer.allocate(Byte.BYTES + Integer.BYTES).put(CODE).putInt(potential).flip();
  }

  @Override
  public String toString() {
    return "INIT packet(potential: " + potential + ")";
  }
}

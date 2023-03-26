package fr.uge.ugegreed.packets;

import java.nio.ByteBuffer;

/**
 * Represents an OK_DISC packet
 */
public record OkDiscPacket() implements Packet {
  private static final byte CODE = PacketCode.OK_DISC.getCode();

  @Override
  public ByteBuffer toBuffer() {
    return ByteBuffer.allocate(Byte.BYTES).put(CODE).flip();
  }

  @Override
  public String toString() {
    return "OK_DISC packet()";
  }
}

package fr.uge.ugegreed.packets;

import fr.uge.ugegreed.utils.TypeToByteWriter;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Represents an REDI packet
 * @param new_parent parent IP and port
 */
public record RediPacket(InetSocketAddress new_parent) implements Packet {
  private static final byte CODE = PacketCode.INIT.getCode();

  public RediPacket {
    Objects.requireNonNull(new_parent);
  }

  @Override
  public ByteBuffer toBuffer() {
    var new_parent_bb = TypeToByteWriter.getHost(new_parent);
    return ByteBuffer.allocate(Byte.BYTES + new_parent_bb.capacity()).put(CODE).put(new_parent_bb).flip();
  }

  @Override
  public String toString() {
    return "REDI packet(new_parent: " + new_parent.toString() + ")";
  }
}

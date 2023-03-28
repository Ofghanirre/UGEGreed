package fr.uge.ugegreed.tests.packets;

import fr.uge.ugegreed.packets.RediPacket;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RediPacketTest {
  @Test
  public void correctToBuffer() {
    var packet = new RediPacket(new InetSocketAddress("251.12.2.42", 65535));
    var buffer = ByteBuffer.allocate(1024)
        .put((byte) 7)
        .put(new byte[]{(byte)251, 12, 2, 42})
        .putShort((short)65535)
        .flip();
    var toBuffer = packet.toBuffer();
    assertEquals(buffer, toBuffer);
  }

  @Test
  public void checkPreconditions() {
    assertThrows(NullPointerException.class, () ->
        new RediPacket(null));
  }

  @Test
  public void correctToString() {
    var packet = new RediPacket(new InetSocketAddress("127.0.0.1", 7777));
    assertEquals("REDI packet(new_parent: /127.0.0.1:7777)", packet.toString());
  }
}

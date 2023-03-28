package fr.uge.ugegreed.tests.packets;

import fr.uge.ugegreed.packets.OkDiscPacket;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OkDiscPacketTest {
  @Test
  public void correctToBuffer() {
    var packet = new OkDiscPacket();
    var buffer = ByteBuffer.allocate(1024).put((byte) 9).flip();
    var toBuffer = packet.toBuffer();
    assertEquals(buffer, toBuffer);
  }

  @Test
  public void correctToString() {
    var packet = new OkDiscPacket();
    assertEquals("OK_DISC packet()", packet.toString());
  }
}

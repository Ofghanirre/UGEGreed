package fr.uge.ugegreed.tests.packets;

import fr.uge.ugegreed.packets.InitPacket;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InitPacketTest {
  @Test
  public void correctToBuffer() {
    var packet = new InitPacket(0, 7777);
    var buffer = ByteBuffer.allocate(1024).put((byte) 1).putInt(0).putInt(7777).flip();
    var toBuffer = packet.toBuffer();
    assertEquals(buffer, toBuffer);
  }

  @Test
  public void checkPreconditions() {
    assertThrows(IllegalArgumentException.class, () -> new InitPacket(-1, 7777));
  }

  @Test
  public void correctToString() {
    var packet = new InitPacket(42, 7777);
    assertEquals("INIT packet(potential: 42, appID: 7777)", packet.toString());
  }
}
